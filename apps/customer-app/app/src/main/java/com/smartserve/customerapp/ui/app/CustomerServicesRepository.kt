package com.smartserve.customerapp.ui.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CustomerServicesRepo"

@Singleton
class CustomerServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {

    private val services   get() = firestore.collection(AuthCollections.SERVICES)
    private val profiles   get() = firestore.collection(AuthCollections.PROVIDER_PROFILES)
    private val categories get() = firestore.collection(AuthCollections.CATEGORIES)
    private val bookings   get() = firestore.collection(AuthCollections.BOOKINGS)
    private val customerProfiles get() = firestore.collection(AuthCollections.CUSTOMER_PROFILES)

    private fun cartItemsCollection(customerUid: String) =
        customerProfiles.document(customerUid).collection(AuthCollections.CUSTOMER_CART_ITEMS)

    private suspend fun providerRatingFromProfile(providerUid: String): Pair<Double, Int> {
        return runCatching {
            val ratingsSnap = profiles.document(providerUid)
                .collection("ratings")
                .get().await()

            val ratings = ratingsSnap.documents.mapNotNull { doc ->
                doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble()
            }.filter { it in 1.0..5.0 }

            if (ratings.isNotEmpty()) {
                ratings.average() to ratings.size
            } else {
                val doc = profiles.document(providerUid).get().await()
                val avg = doc.getDouble("avgRating") ?: doc.getLong("avgRating")?.toDouble() ?: 0.0
                val total = doc.getLong("totalReviews")?.toInt() ?: 0
                avg to total
            }
        }.getOrDefault(0.0 to 0)
    }

    /**
     * Live cart for the signed-in customer. Emits empty when logged out.
     * Documents use DocumentReference fields: `service`, `provider`, optional `category`.
     */
    fun observeCartItems(): Flow<List<CartItem>> = callbackFlow {
        var reg: ListenerRegistration? = null

        fun attach(uid: String?) {
            reg?.remove()
            reg = null
            if (uid.isNullOrBlank()) {
                trySend(emptyList())
                return
            }
            val col = cartItemsCollection(uid)
            Log.d(TAG, "observeCartItems attach uid=${uid.take(6)}… path=${col.path}")
            reg = col.addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeCartItems listener error path=${col.path}", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toCartLine() }.orEmpty()
                    .sortedBy { it.addedAtMillis }
                trySend(list)
            }
        }

        attach(auth.currentUser?.uid)
        val listener = FirebaseAuth.AuthStateListener { attach(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
            reg?.remove()
        }
    }.distinctUntilChanged()

    suspend fun addCartLine(item: CartItem): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Customer not signed in")
        val col = cartItemsCollection(uid)
        val ref = col.document()
        Log.d(TAG, "addCartLine writing ${ref.path}")
        ref.set(cartLinePayload(item)).await()
        Unit
    }.onFailure { e ->
        Log.e(TAG, "addCartLine FAILED path=customer_profiles/[uid]/cart_items — check rules & console subcollection", e)
    }

    suspend fun removeCartLine(lineDocumentId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Customer not signed in")
        cartItemsCollection(uid).document(lineDocumentId).delete().await()
    }

    suspend fun clearCustomerCart(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val snap = cartItemsCollection(uid).get().await()
        if (snap.isEmpty) return@runCatching
        var batch = firestore.batch()
        var n = 0
        for (doc in snap.documents) {
            batch.delete(doc.reference)
            n++
            if (n >= 450) {
                batch.commit().await()
                batch = firestore.batch()
                n = 0
            }
        }
        if (n > 0) batch.commit().await()
    }

    /** In-memory cache so BookingViewModel reacts instantly when profile saves a new address. */
    private val _homeAddressCache = MutableStateFlow("")
    val homeAddressFlow: StateFlow<String> = _homeAddressCache

    /**
     * Returns providers who have at least one active service in [categoryId].
     * Filters isActive in-memory to avoid requiring a composite Firestore index.
     */
    suspend fun getProvidersByCategory(categoryId: String): List<CustomerProviderSummary> {
        return try {
            // Read broadly and filter in-memory so both legacy and current document shapes work.
            val snap = services.get().await()

            val categoryDocs = snap.documents.filter { doc ->
                extractCategoryId(doc) == categoryId
            }

            val activeDocs = categoryDocs.filter { it.getBoolean("isActive") != false }

            val activeProviderIds = activeDocs
                .mapNotNull { doc -> extractProviderId(doc) }
                .distinct()

            Log.d(TAG, "getProvidersByCategory($categoryId): ${categoryDocs.size} category docs, ${activeProviderIds.size} active providers")

            // Try to resolve a name for each provider.
            // 1. profile.displayName (fast path – usually populated for new accounts)
            // 2. title of the onboarding service in THIS query (doc.id == providerUid)
            // 3. Separate fetch of services/{uid} for the onboarding service when it lives
            //    in a different category (e.g. provider onboarded as "Plumbing", customer
            //    browses "Electrical").

            // Build a quick map from docs already in this query
            val inQueryOnboardingTitle: Map<String, String> = activeDocs
                .mapNotNull { doc ->
                    val provId = extractProviderId(doc) ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            // Index active service docs by provider UID for quick lookup
            val serviceDocsByProvider: Map<String, List<com.google.firebase.firestore.DocumentSnapshot>> =
                activeDocs.groupBy { doc ->
                    extractProviderId(doc) ?: ""
                }

            activeProviderIds.mapNotNull { uid ->
                val profileDoc = profiles.document(uid).get().await()
                if (!isProviderProfileActive(profileDoc)) {
                    Log.d(TAG, "Skipping inactive provider in category list: $uid")
                    return@mapNotNull null
                }
                val profileName = profileDoc.getString("displayName")?.trim()
                    ?.takeIf { it.isNotBlank() }

                val fallbackName = if (profileName.isNullOrBlank()) {
                    inQueryOnboardingTitle[uid]?.takeIf { it.isNotBlank() }
                        ?: try {
                            services.document(uid).get().await()
                                .getString("title")?.trim()
                                ?.takeIf { it.isNotBlank() && it != "Service" }
                        } catch (_: Exception) { null }
                        ?: ""
                } else ""

                // Use the first active service in this category for rate & availability
                val svcDoc = serviceDocsByProvider[uid]?.firstOrNull()
                val categoryRate = svcDoc?.getDouble("hourlyRate")
                    ?: svcDoc?.getLong("hourlyRate")?.toDouble() ?: 0.0
                val categoryDays = (svcDoc?.get("availabilityDays") as? List<*>)
                    ?.mapNotNull { it?.toString() } ?: emptyList()
                val categoryStart = svcDoc?.getString("availabilityStart") ?: ""
                val categoryEnd   = svcDoc?.getString("availabilityEnd")   ?: ""
                val (avgRating, totalReviews) = providerRatingFromProfile(uid)

                val serviceTitles = runCatching {
                    snap.documents.asSequence()
                        .filter { doc ->
                            val providerMatch = extractProviderId(doc) == uid
                            providerMatch && doc.getBoolean("isActive") != false
                        }
                        .mapNotNull { doc ->
                            doc.getString("title")?.trim()?.takeIf { title -> title.isNotBlank() }
                        }
                        .distinct()
                        .sorted()
                        .toList()
                }.getOrDefault(emptyList())

                profileDoc.toCustomerProviderSummary(fallbackName = fallbackName)?.copy(
                    avgRating = avgRating,
                    totalReviews = totalReviews,
                    serviceTitles = serviceTitles,
                    serviceDescription = "",
                    hourlyRate = categoryRate,
                    categoryServiceRate = categoryRate,
                    categoryAvailabilityDays = categoryDays,
                    categoryAvailabilityStart = categoryStart,
                    categoryAvailabilityEnd = categoryEnd,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getProvidersByCategory failed", e)
            emptyList()
        }
    }

    /**
     * Returns services for a given provider, optionally filtered to a specific category.
     * Both filters are applied in-memory to avoid composite Firestore index requirements.
     */
    suspend fun getServicesForProvider(
        providerUid: String,
        providerName: String,
        categoryId: String = "",
    ): List<CustomerServiceListing> {
        return try {
            // Read services broadly and filter in-memory so older docs with legacy provider shapes
            // still show up in the customer-facing list.
            val snap = services.get().await()
            val (providerAvgRating, providerTotalReviews) = providerRatingFromProfile(providerUid)

            Log.d(TAG, "getServicesForProvider($providerUid, cat=$categoryId): ${snap.size()} docs total")

            val providerDocs = snap.documents.filter { doc ->
                extractProviderId(doc) == providerUid && doc.getBoolean("isActive") != false
            }

            val candidateDocs = if (categoryId.isBlank()) {
                providerDocs
            } else {
                val inCategory = providerDocs.filter { doc -> extractCategoryId(doc) == categoryId }
                if (inCategory.isNotEmpty()) inCategory else providerDocs
            }

            Log.d(
                TAG,
                "getServicesForProvider($providerUid, cat=$categoryId): ${providerDocs.size} provider docs, ${candidateDocs.size} candidate docs, providerName=$providerName"
            )

            candidateDocs.forEach { doc ->
                Log.d(
                    TAG,
                    "serviceMatch provider=$providerUid docId=${doc.id} title=${doc.getString("title")} extractedProvider=${extractProviderId(doc)} extractedCategory=${extractCategoryId(doc)} active=${doc.getBoolean("isActive")}",
                )
            }

            candidateDocs
                .mapNotNull { doc ->
                    val title = doc.getString("title")?.trim().orEmpty()
                        .ifBlank { return@mapNotNull null }
                    CustomerServiceListing(
                        serviceId = doc.id,
                        title = title,
                        description = doc.getString("description").orEmpty(),
                        hourlyRate = doc.getDouble("hourlyRate")
                            ?: doc.getLong("hourlyRate")?.toDouble() ?: 0.0,
                        providerUid = providerUid,
                        providerName = providerName,
                        availabilityDays = (doc.get("availabilityDays") as? List<*>)
                            ?.mapNotNull { it?.toString() } ?: emptyList(),
                        availabilityStart = doc.getString("availabilityStart") ?: "09:00",
                        availabilityEnd = doc.getString("availabilityEnd") ?: "18:00",
                        providerAvgRating = providerAvgRating,
                        providerTotalReviews = providerTotalReviews,
                        photoUrls = doc.readPhotoUrls(),
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "getServicesForProvider failed", e)
            emptyList()
        }
    }

    /**
     * Returns all services for the search screen.
     * Fetches all services and keeps only documents with `isActive == true`.
     */
    suspend fun getAllActiveServices(): List<CustomerServiceListing> {
        return try {
            // No server-side filter → no composite index; filter `isActive` in memory.
            val snap = services.get().await()

            Log.d(TAG, "getAllActiveServices: ${snap.size()} docs")

            // Batch fetch unique provider names
            val providerIds = snap.documents
                .mapNotNull { doc -> extractProviderId(doc) }
                .distinct()

            // Build name fallback map from services already fetched (onboarding doc has id == uid)
            val inQueryOnboardingTitle: Map<String, String> = snap.documents
                .mapNotNull { doc ->
                    val provId = extractProviderId(doc) ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            val providerNames = providerIds.associate { uid ->
                val profileDoc = profiles.document(uid).get().await()
                val profileName = if (isProviderProfileActive(profileDoc)) {
                    profileDoc.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
                } else {
                    null
                }

                val name = profileName
                    ?: inQueryOnboardingTitle[uid]?.takeIf { it.isNotBlank() }
                    // getAllActiveServices fetches ALL services, so if the onboarding service
                    // exists it will be in snap.documents already — no extra fetch needed here.
                    ?: "Provider ${uid.take(6)}"

                uid to name
            }

            snap.documents
                .filter { it.getBoolean("isActive") != false }
                .mapNotNull { doc ->
                val providerUid = extractProviderId(doc)
                    ?: return@mapNotNull null
                val providerDoc = profiles.document(providerUid).get().await()
                if (!isProviderProfileActive(providerDoc)) {
                    return@mapNotNull null
                }
                val title = doc.getString("title")?.trim().orEmpty()
                    .ifBlank { return@mapNotNull null }
                val (providerAvgRating, providerTotalReviews) = providerRatingFromProfile(providerUid)
                CustomerServiceListing(
                    serviceId = doc.id,
                    title = title,
                    description = doc.getString("description").orEmpty(),
                    hourlyRate = doc.getDouble("hourlyRate")
                        ?: doc.getLong("hourlyRate")?.toDouble() ?: 0.0,
                    providerUid = providerUid,
                    providerName = providerNames[providerUid] ?: "",
                    availabilityDays = (doc.get("availabilityDays") as? List<*>)
                        ?.mapNotNull { it?.toString() } ?: emptyList(),
                    availabilityStart = doc.getString("availabilityStart") ?: "09:00",
                    availabilityEnd = doc.getString("availabilityEnd") ?: "18:00",
                    providerAvgRating = providerAvgRating,
                    providerTotalReviews = providerTotalReviews,
                    photoUrls = doc.readPhotoUrls(),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllActiveServices failed", e)
            emptyList()
        }
    }

    /**
     * Returns top providers sorted by avgRating.
     * Sorts in-memory to avoid requiring a Firestore index and to include
     * providers whose avgRating field may not yet be set.
     * For providers with a blank displayName, falls back to the title of their
     * onboarding service document (doc.id == providerUid).
     */
    suspend fun getTopProviders(limit: Int = 5): List<CustomerProviderSummary> {
        return try {
            // Fetch all provider profiles (no orderBy → no index required)
            val snap = profiles.limit(50).get().await()
            Log.d(TAG, "getTopProviders: ${snap.size()} profiles fetched")

            // For any profile with a blank displayName, look up their onboarding service
            // (document ID == provider UID) to use its title as the name.
            val blankNameUids = snap.documents
                .filter { doc -> doc.getString("displayName").isNullOrBlank() }
                .map { it.id }

            val onboardingTitleByUid: Map<String, String> = blankNameUids.associate { uid ->
                val title = try {
                    services.document(uid).get().await()
                        .getString("title")?.trim().orEmpty()
                        .let { if (it == "Service") "" else it }
                } catch (_: Exception) { "" }
                uid to title
            }

            snap.documents.mapNotNull { profileDoc ->
                if (!isProviderProfileActive(profileDoc)) {
                    return@mapNotNull null
                }
                val uid = profileDoc.id
                val svcSnap = runCatching { services.document(uid).get().await() }.getOrNull()
                val listingDesc = svcSnap?.getString("description").orEmpty()
                val listingRate = svcSnap?.getDouble("hourlyRate")
                    ?: svcSnap?.getLong("hourlyRate")?.toDouble() ?: 0.0
                profileDoc.toCustomerProviderSummary(fallbackName = onboardingTitleByUid[uid] ?: "")
                    ?.copy(serviceDescription = listingDesc, hourlyRate = listingRate)
            }
                .sortedByDescending { it.avgRating }
                .take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getTopProviders failed", e)
            emptyList()
        }
    }

    // ── Customer-side helpers ─────────────────────────────────────────────────

    /** Fetches the home address saved in the customer's profile, or empty string. Also seeds [homeAddressFlow]. */
    suspend fun getCustomerHomeAddress(): String {
        val uid = auth.currentUser?.uid ?: return ""
        return runCatching {
            val addr = customerProfiles.document(uid).get().await().getString("homeAddress").orEmpty()
            _homeAddressCache.value = addr
            addr
        }.getOrDefault("")
    }

    /** Updates the in-memory home-address cache — call after a successful profile save. */
    fun setHomeAddressCache(address: String) {
        _homeAddressCache.value = address
    }

    /**
     * Writes each [CartItem] as a pending booking: refs for customer, provider, service,
     * optional category, plus `bookingDate`, `timeSlot`, `address`, `location`, `hourlyRate`, `status`.
     */
    suspend fun confirmBookings(items: List<CartItem>): Result<Unit> = runCatching {
        val customerId = auth.currentUser?.uid
            ?: error("Customer not signed in")

        // Guard against stale UI: re-check each provider slot before writing bookings.
        items.forEach { item ->
            if (isProviderSlotBlocked(item.providerUid, item.date, item.time)) {
                error("Selected time slot is no longer available for this provider")
            }
        }

        val batch = firestore.batch()
        val customerRef = customerProfiles.document(customerId)
        items.forEach { item ->
            val docId = bookingDocumentId(customerId, item)
            val doc = bookings.document(docId)
            val payload = buildBookingPayload(customerId, customerRef, item)
            batch.set(doc, payload)
        }
        batch.commit().await()
    }

    private suspend fun isProviderSlotBlocked(
        providerUid: String,
        dateLabel: String,
        slotStart: String,
    ): Boolean {
        if (providerUid.isBlank() || dateLabel.isBlank() || slotStart.isBlank()) return false

        val allowedStatuses = setOf("pending", "active", "confirmed")
        val normalizedStart = normalizeSlotStartLabel(slotStart)

        // Read broadly to support both current and legacy booking field shapes.
        val snap = bookings.get().await()
        return snap.documents.any { doc ->
            val status = doc.getString("status")?.lowercase()?.trim().orEmpty()
            if (status !in allowedStatuses) return@any false

            val docProviderId = extractProviderId(doc)
                ?: normalizeRefId(doc.getString("provider"))
                ?: normalizeRefId(doc.getString("providerId"))
                ?: normalizeRefId(doc.getString("providerUid"))
                ?: ""
            if (docProviderId != providerUid) return@any false

            val docDate = extractBookingDateLabel(doc)
            if (docDate != dateLabel) return@any false

            val docStart = extractBookingStartLabel(doc)

            docStart == normalizedStart
        }
    }

    private fun buildBookingPayload(
        customerId: String,
        customerRef: com.google.firebase.firestore.DocumentReference,
        item: CartItem,
    ): HashMap<String, Any> {
        val bookingDate = parseCartDateToTimestamp(item.date) ?: Timestamp.now()
        val scheduledAt = computeScheduledAt(bookingDate, item.time) ?: bookingDate
        val m = hashMapOf<String, Any>(
            // References
            "customer" to customerRef,
            "provider" to profiles.document(item.providerUid),
            "service" to services.document(item.serviceId),
            // Canonical booking fields (keep simple; reference-based)
            "bookingDate" to bookingDate,
            "timeSlot" to item.time,
            "scheduledAt" to scheduledAt,
            "address" to item.address,
            "location" to GeoPoint(item.lat, item.lon),
            "hourlyRate" to item.hourlyRate,
            "specialInstructions" to item.specialInstructions,
            "status" to "pending",
            "createdAt" to Timestamp.now(),
        )
        if (item.timeRange.isNotBlank()) {
            m["timeRange"] = item.timeRange
        }
        if (item.categoryId.isNotBlank()) {
            m["category"] = categories.document(item.categoryId)
        }
        return m
    }

    private fun bookingDocumentId(customerId: String, item: CartItem): String {
        // Simple deterministic id so we don't create duplicate bookings.
        // (Firestore doc ids can't contain '/')
        val raw = "${customerId}_${item.providerUid}_${item.serviceId}_${item.date}_${item.time}"
        return raw.replace("/", "_")
    }

    private fun cartLinePayload(item: CartItem): HashMap<String, Any> {
        val m = hashMapOf<String, Any>(
            "service" to services.document(item.serviceId),
            "provider" to profiles.document(item.providerUid),
            "providerNameCache" to item.providerName,
            "serviceNameCache" to item.serviceName,
            "priceLabel" to item.price,
            "hourlyRateSnapshot" to item.hourlyRate,
            "address" to item.address,
            "location" to GeoPoint(item.lat, item.lon),
            "scheduledDate" to item.date,
            "scheduledTime" to item.time,
            "timeRange" to item.timeRange,
            "specialInstructions" to item.specialInstructions,
            "addedAt" to Timestamp.now(),
        )
        if (item.categoryId.isNotBlank()) {
            m["category"] = categories.document(item.categoryId)
        }
        return m
    }

    /** Returns all bookings for the currently signed-in customer, newest first. */
    suspend fun getMyBookings(): List<CustomerBooking> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return runCatching {
            val customerRef = customerProfiles.document(uid)
            val docs = bookings.whereEqualTo("customer", customerRef).get().await().documents
            mapBookingDocuments(docs)
                .sortedByDescending { it.createdAtMillis }
        }.getOrDefault(emptyList())
    }

    /** Real-time stream of bookings for the signed-in customer, newest first. */
    fun observeMyBookings(): Flow<List<CustomerBooking>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val customerRef = customerProfiles.document(uid)
        val sub = bookings.whereEqualTo("customer", customerRef)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    Log.e(TAG, "observeMyBookings listener error", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val mapped = mapBookingDocuments(snap.documents)
                        .sortedByDescending { it.createdAtMillis }
                    trySend(mapped)
                }
            }

        awaitClose { sub.remove() }
    }.distinctUntilChanged()

    /**
     * Real-time booked slot starts for a provider+date across all services.
     * Blocks starts that are currently pending/active/confirmed.
     */
    fun observeBookedSlotStarts(
        providerUid: String,
        dateLabel: String,
    ): Flow<Set<String>> = callbackFlow {
        if (providerUid.isBlank() || dateLabel.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val allowedStatuses = setOf("pending", "active", "confirmed")

        // Read broadly and filter in-memory to include legacy booking docs that don't store
        // provider as a DocumentReference.
        val sub = bookings
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    Log.e(TAG, "observeBookedSlotStarts listener error", err)
                    trySend(emptySet())
                    return@addSnapshotListener
                }

                val blockedStarts = snap.documents.mapNotNull { doc ->
                    val status = doc.getString("status")?.lowercase()?.trim().orEmpty()
                    if (status !in allowedStatuses) return@mapNotNull null

                    val docProviderId = extractProviderId(doc)
                        ?: normalizeRefId(doc.getString("provider"))
                        ?: normalizeRefId(doc.getString("providerId"))
                        ?: normalizeRefId(doc.getString("providerUid"))
                        ?: ""
                    if (docProviderId != providerUid) return@mapNotNull null

                    val docDate = extractBookingDateLabel(doc)
                    if (docDate != dateLabel) return@mapNotNull null

                    extractBookingStartLabel(doc).takeIf { it.isNotBlank() }
                }.toSet()

                trySend(blockedStarts)
            }

        awaitClose { sub.remove() }
    }.distinctUntilChanged()

    private suspend fun mapBookingDocuments(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
    ): List<CustomerBooking> {
            val providerNameCache = mutableMapOf<String, String>()
            val serviceNameCache = mutableMapOf<String, String>()
            val categoryLabelCache = mutableMapOf<String, String>()

            suspend fun providerNameFor(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                val ref = doc.getDocumentReference("provider") ?: return ""
                return providerNameCache.getOrPut(ref.path) {
                    runCatching { ref.get().await().getString("displayName").orEmpty() }.getOrDefault("")
                }
            }

            suspend fun serviceNameFor(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                val ref = doc.getDocumentReference("service") ?: return ""
                return serviceNameCache.getOrPut(ref.path) {
                    runCatching { ref.get().await().getString("title").orEmpty() }.getOrDefault("")
                }
            }

            suspend fun typeLabelFor(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                val categoryId = doc.getDocumentReference("category")?.id
                    ?: doc.getString("category")?.trim().orEmpty()
                if (categoryId.isBlank()) return ""
                return categoryLabelCache.getOrPut(categoryId) {
                    runCatching { categories.document(categoryId).get().await().getString("label").orEmpty() }
                        .getOrDefault("")
                        .trim()
                }
            }

        return docs.map { doc ->
                val dateStr = doc.getTimestamp("bookingDate")?.let { ts ->
                    SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
                }.orEmpty()
                val timeStr = doc.getString("timeRange")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: doc.getString("timeSlot").orEmpty()
                val scheduledAtMillis = doc.getTimestamp("scheduledAt")?.toDate()?.time
                    ?: computeScheduledAt(doc.getTimestamp("bookingDate"), doc.getString("timeSlot").orEmpty())?.toDate()?.time
                    ?: 0L
                val priceStr = doc.getDouble("hourlyRate")?.let { "$${it.toInt()}/hr" }
                    ?: doc.getLong("hourlyRate")?.toDouble()?.let { "$${it.toInt()}/hr" }
                    ?: ""
                CustomerBooking(
                    id           = doc.id,
                    providerName = providerNameFor(doc),
                    typeLabel    = typeLabelFor(doc),
                    providerRating = doc.getDouble("providerRating")?.toFloat()
                        ?: doc.getLong("providerRating")?.toFloat(),
                    customerRating = doc.getDouble("customerRating")?.toFloat()
                        ?: doc.getLong("customerRating")?.toFloat(),
                    serviceName  = serviceNameFor(doc),
                    price        = priceStr,
                    date         = dateStr,
                    time         = timeStr,
                    status       = doc.getString("status") ?: "pending",
                    address      = doc.getString("address").orEmpty(),
                    scheduledAtMillis = scheduledAtMillis,
                    createdAtMillis = doc.getTimestamp("createdAt")
                        ?.toDate()?.time ?: 0L,
                )
                }
    }

    private fun computeScheduledAt(date: Timestamp?, timeSlot: String): Timestamp? {
        if (date == null) return null
        val t = timeSlot.trim()
        if (t.isBlank()) return null
        val parsed = runCatching { SimpleDateFormat("h:mm a", Locale.getDefault()).parse(t) }.getOrNull()
            ?: return null

        val base = Calendar.getInstance().apply { time = date.toDate() }
        val tm = Calendar.getInstance().apply { time = parsed }
        base.set(Calendar.HOUR_OF_DAY, tm.get(Calendar.HOUR_OF_DAY))
        base.set(Calendar.MINUTE, tm.get(Calendar.MINUTE))
        base.set(Calendar.SECOND, 0)
        base.set(Calendar.MILLISECOND, 0)
        return Timestamp(base.time)
    }

    suspend fun deleteBooking(bookingId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Customer not signed in")
        Log.d(TAG, "deleteBooking uid=${uid.take(6)}… id=$bookingId")
        bookings.document(bookingId).delete().await()
        Unit
    }

    /** Returns all service categories, sorted by label. */
    suspend fun getCategories(): List<com.smartserve.sharedauth.ServiceCategoryOption> {
        return runCatching {
            categories.get().await().documents.mapNotNull { doc ->
                val label = doc.getString("label")?.trim()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                com.smartserve.sharedauth.ServiceCategoryOption(id = doc.id, label = label)
            }.sortedBy { it.label }
        }.getOrDefault(emptyList())
    }

    suspend fun rateProvider(bookingId: String, rating: Float): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Customer not signed in")
        Log.d(TAG, "rateProvider uid=${uid.take(6)}… id=$bookingId rating=$rating")
        val bookingRef = bookings.document(bookingId)
        val bookingSnap = bookingRef.get().await()
        val providerId = bookingSnap.getDocumentReference("provider")?.id
            ?: error("Booking missing provider reference")

        val providerRatingRef = profiles.document(providerId)
            .collection("ratings")
            .document(bookingId)

        val batch = firestore.batch()
        batch.update(bookingRef, "providerRating", rating.toDouble())
        batch.set(
            providerRatingRef,
            mapOf(
                "bookingId" to bookingId,
                "providerId" to providerId,
                "customerId" to uid,
                "rating" to rating.toDouble(),
                "updatedAt" to Timestamp.now(),
            ),
            SetOptions.merge(),
        )
        batch.commit().await()

        // Recompute avgRating + totalReviews on provider_profiles/{providerId}
        val ratingsSnap = profiles.document(providerId).collection("ratings").get().await()
        val allRatings = ratingsSnap.documents
            .mapNotNull { it.getDouble("rating") ?: it.getLong("rating")?.toDouble() }
            .filter { it in 1.0..5.0 }
        if (allRatings.isNotEmpty()) {
            profiles.document(providerId).update(
                mapOf(
                    "avgRating" to allRatings.average(),
                    "totalReviews" to allRatings.size,
                )
            ).await()
            Log.d(TAG, "rateProvider synced avgRating=${allRatings.average()} totalReviews=${allRatings.size} for provider=$providerId")
        }
        Unit
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.readPhotoUrls(): List<String> =
    (get("photoUrls") as? List<*>)
        ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
        ?: emptyList()

private fun parseCartDateToTimestamp(dateStr: String): Timestamp? {
    if (dateStr.isBlank()) return null
    return runCatching {
        val fmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        Timestamp(fmt.parse(dateStr)!!)
    }.getOrNull()
}

private fun normalizeRefId(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    return value.substringAfterLast('/').takeIf { it.isNotBlank() }
}

private fun extractProviderId(doc: com.google.firebase.firestore.DocumentSnapshot): String? {
    doc.getDocumentReference("provider")?.id?.let { if (it.isNotBlank()) return it }
    normalizeRefId(doc.getString("provider"))?.let { return it }
    normalizeRefId(doc.getString("providerId"))?.let { return it }
    normalizeRefId(doc.getString("providerUid"))?.let { return it }

    val raw = doc.get("provider")
    if (raw is Map<*, *>) {
        normalizeRefId(raw["id"]?.toString())?.let { return it }
        normalizeRefId(raw["uid"]?.toString())?.let { return it }
        normalizeRefId(raw["path"]?.toString())?.let { return it }
    }
    return null
}

private fun extractCategoryId(doc: com.google.firebase.firestore.DocumentSnapshot): String? {
    doc.getDocumentReference("category")?.id?.let { if (it.isNotBlank()) return it }
    normalizeRefId(doc.getString("category"))?.let { return it }
    normalizeRefId(doc.getString("categoryId"))?.let { return it }

    val raw = doc.get("category")
    if (raw is Map<*, *>) {
        normalizeRefId(raw["id"]?.toString())?.let { return it }
        normalizeRefId(raw["path"]?.toString())?.let { return it }
    }
    return null
}

private fun providerNameMatches(
    doc: com.google.firebase.firestore.DocumentSnapshot,
    expectedName: String,
): Boolean {
    val target = expectedName.trim().lowercase()
    if (target.isBlank()) return false

    val hints = listOf(
        doc.getString("providerName"),
        doc.getString("providerDisplayName"),
        doc.getString("providerNameCache"),
        doc.getString("displayName"),
    ).mapNotNull { it?.trim()?.takeIf { s -> s.isNotBlank() } }

    return hints.any { it.lowercase() == target }
}

private fun isProviderProfileActive(
    profileDoc: com.google.firebase.firestore.DocumentSnapshot,
): Boolean {
    // Prefer explicit booleans when present.
    profileDoc.getBoolean("isActive")?.let { return it }
    profileDoc.getBoolean("active")?.let { return it }
    profileDoc.getBoolean("isAvailable")?.let { return it }
    profileDoc.getBoolean("isOnline")?.let { return it }
    profileDoc.getBoolean("online")?.let { return it }

    // Fall back to status-style fields used in some profile schemas.
    val statusTokens = listOfNotNull(
        profileDoc.getString("status"),
        profileDoc.getString("providerStatus"),
        profileDoc.getString("accountStatus"),
        profileDoc.getString("availabilityStatus"),
    ).map { it.trim().lowercase() }

    if (statusTokens.any { it in setOf("inactive", "disabled", "suspended", "offline", "blocked") }) {
        return false
    }
    if (statusTokens.any { it in setOf("active", "available", "online", "enabled") }) {
        return true
    }

    // No explicit marker found: keep backward-compatible behavior.
    return true
}

private fun normalizeSlotStartLabel(raw: String?): String {
    val input = raw?.trim().orEmpty()
    if (input.isBlank()) return ""

    val candidates = listOf(
        "h:mm a",
        "hh:mm a",
        "h:mma",
        "hh:mma",
        "H:mm",
        "HH:mm",
    )

    candidates.forEach { pattern ->
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }.parse(input)
        }.getOrNull()
        if (parsed != null) {
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed).trim().lowercase()
        }
    }

    return input.lowercase().replace("  ", " ")
}

private fun extractBookingDateLabel(
    doc: com.google.firebase.firestore.DocumentSnapshot,
): String {
    doc.getTimestamp("bookingDate")?.let { ts ->
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
    }

    val direct = listOf(
        doc.getString("scheduledDate"),
        doc.getString("date"),
        doc.getString("bookingDate"),
    ).mapNotNull { it?.trim()?.takeIf { v -> v.isNotBlank() } }.firstOrNull()

    return direct.orEmpty()
}

private fun extractBookingStartLabel(
    doc: com.google.firebase.firestore.DocumentSnapshot,
): String {
    val rawStart = listOf(
        doc.getString("timeSlot"),
        doc.getString("scheduledTime"),
        doc.getString("time"),
        doc.getString("timeRange")?.substringBefore("-"),
    ).mapNotNull { it?.trim()?.takeIf { v -> v.isNotBlank() } }.firstOrNull()

    return normalizeSlotStartLabel(rawStart)
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCartLine(): CartItem? {
    val serviceRef = getDocumentReference("service") ?: return null
    val providerRef = getDocumentReference("provider") ?: return null
    val categoryRef = getDocumentReference("category")
    return CartItem(
        lineDocumentId = id,
        serviceId = serviceRef.id,
        providerUid = providerRef.id,
        categoryId = categoryRef?.id ?: "",
        providerName = getString("providerNameCache") ?: "",
        serviceName = getString("serviceNameCache") ?: "",
        price = getString("priceLabel") ?: "",
        hourlyRate = getDouble("hourlyRateSnapshot")
            ?: getLong("hourlyRateSnapshot")?.toDouble() ?: 0.0,
        lat = getGeoPoint("location")?.latitude ?: 0.0,
        lon = getGeoPoint("location")?.longitude ?: 0.0,
        address = getString("address") ?: "",
        date = getString("scheduledDate") ?: "",
        time = getString("scheduledTime") ?: "",
        timeRange = getString("timeRange") ?: "",
        specialInstructions = getString("specialInstructions") ?: "",
        addedAtMillis = getTimestamp("addedAt")?.toDate()?.time ?: 0L,
    )
}

/**
 * [fallbackName] is used when [displayName] field is blank — callers can pass the
 * onboarding service title (service doc where doc.id == providerUid) as the name.
 * Falls back to a short UID-based placeholder so providers always appear.
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toCustomerProviderSummary(
    fallbackName: String = "",
): CustomerProviderSummary? {
    val uid = id.ifBlank { return null }
    val displayName = getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
        ?: fallbackName.takeIf { it.isNotBlank() }
        ?: "Provider ${uid.take(6)}"
    // Listing fields (description, rate, area) live on `services/*`, not provider_profiles.
    return CustomerProviderSummary(
        uid = uid,
        displayName = displayName,
        avgRating = getDouble("avgRating") ?: getLong("avgRating")?.toDouble() ?: 5.0,
        totalReviews = getLong("totalReviews")?.toInt() ?: 0,
        serviceTitles = emptyList(),
        serviceDescription = "",
        hourlyRate = 0.0,
    )
}

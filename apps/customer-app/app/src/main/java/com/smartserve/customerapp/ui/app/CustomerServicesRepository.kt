package com.smartserve.customerapp.ui.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartserve.sharedauth.AuthCollections
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
            val categoryRef = categories.document(categoryId)
            // Single-field equality only → no composite index required
            val snap = services
                .whereEqualTo("category", categoryRef)
                .get().await()

            val activeDocs = snap.documents.filter { it.getBoolean("isActive") == true }

            val activeProviderIds = activeDocs
                .mapNotNull { it.getDocumentReference("provider")?.id }
                .distinct()

            Log.d(TAG, "getProvidersByCategory($categoryId): ${snap.size()} docs, ${activeProviderIds.size} active providers")

            // Try to resolve a name for each provider.
            // 1. profile.displayName (fast path – usually populated for new accounts)
            // 2. title of the onboarding service in THIS query (doc.id == providerUid)
            // 3. Separate fetch of services/{uid} for the onboarding service when it lives
            //    in a different category (e.g. provider onboarded as "Plumbing", customer
            //    browses "Electrical").

            // Build a quick map from docs already in this query
            val inQueryOnboardingTitle: Map<String, String> = activeDocs
                .mapNotNull { doc ->
                    val provId = doc.getDocumentReference("provider")?.id ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            // Index active service docs by provider UID for quick lookup
            val serviceDocsByProvider: Map<String, List<com.google.firebase.firestore.DocumentSnapshot>> =
                activeDocs.groupBy { it.getDocumentReference("provider")?.id ?: "" }

            activeProviderIds.mapNotNull { uid ->
                val profileDoc = profiles.document(uid).get().await()
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

                profileDoc.toCustomerProviderSummary(fallbackName = fallbackName)?.copy(
                    serviceDescription = svcDoc?.getString("description").orEmpty(),
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
            val providerRef = profiles.document(providerUid)
            // Single-field equality only → no composite index required
            val snap = services
                .whereEqualTo("provider", providerRef)
                .get().await()

            Log.d(TAG, "getServicesForProvider($providerUid, cat=$categoryId): ${snap.size()} docs total")

            snap.documents
                .filter { doc ->
                    val active = doc.getBoolean("isActive") == true
                    val matchesCat = categoryId.isEmpty() ||
                        doc.getDocumentReference("category")?.id == categoryId
                    active && matchesCat
                }
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
                .mapNotNull { it.getDocumentReference("provider")?.id }
                .distinct()

            // Build name fallback map from services already fetched (onboarding doc has id == uid)
            val inQueryOnboardingTitle: Map<String, String> = snap.documents
                .mapNotNull { doc ->
                    val provId = doc.getDocumentReference("provider")?.id ?: return@mapNotNull null
                    val title = doc.getString("title")?.trim().orEmpty()
                    if (doc.id == provId && title.isNotBlank() && title != "Service")
                        provId to title
                    else null
                }.toMap()

            val providerNames = providerIds.associate { uid ->
                val profileName = profiles.document(uid).get().await().getString("displayName")
                    ?.trim()?.takeIf { it.isNotBlank() }

                val name = profileName
                    ?: inQueryOnboardingTitle[uid]?.takeIf { it.isNotBlank() }
                    // getAllActiveServices fetches ALL services, so if the onboarding service
                    // exists it will be in snap.documents already — no extra fetch needed here.
                    ?: "Provider ${uid.take(6)}"

                uid to name
            }

            snap.documents
                .filter { it.getBoolean("isActive") == true }
                .mapNotNull { doc ->
                val providerUid = doc.getDocumentReference("provider")?.id
                    ?: return@mapNotNull null
                val title = doc.getString("title")?.trim().orEmpty()
                    .ifBlank { return@mapNotNull null }
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
            "status" to "pending",
            "createdAt" to Timestamp.now(),
        )
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

            docs.map { doc ->
                val dateStr = doc.getTimestamp("bookingDate")?.let { ts ->
                    SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
                }.orEmpty()
                val timeStr = doc.getString("timeSlot").orEmpty()
                val scheduledAtMillis = doc.getTimestamp("scheduledAt")?.toDate()?.time
                    ?: computeScheduledAt(doc.getTimestamp("bookingDate"), timeStr)?.toDate()?.time
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
                .sortedByDescending { it.createdAtMillis }
        }.getOrDefault(emptyList())
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

    suspend fun rateProvider(bookingId: String, rating: Float): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Customer not signed in")
        Log.d(TAG, "rateProvider uid=${uid.take(6)}… id=$bookingId rating=$rating")
        bookings.document(bookingId).update("providerRating", rating.toDouble()).await()
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
        avgRating = getDouble("avgRating") ?: getLong("avgRating")?.toDouble() ?: 0.0,
        totalReviews = getLong("totalReviews")?.toInt() ?: 0,
        serviceDescription = "",
        hourlyRate = 0.0,
    )
}

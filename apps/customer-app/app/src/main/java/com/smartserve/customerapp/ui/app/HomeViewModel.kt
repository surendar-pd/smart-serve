package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartserve.sharedauth.ServiceCategoryOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ── Time context ───────────────────────────────────────────────────────────────

enum class TimeContext {
    MORNING,    // 05:00 – 11:59
    AFTERNOON,  // 12:00 – 16:59
    EVENING,    // 17:00 – 21:59
    NIGHT,      // 22:00 – 04:59
    WEEKEND,    // Saturday or Sunday (any hour)
}

fun currentTimeContext(): TimeContext {
    val cal = Calendar.getInstance()
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return TimeContext.WEEKEND
    return when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> TimeContext.MORNING
        in 12..16 -> TimeContext.AFTERNOON
        in 17..21 -> TimeContext.EVENING
        else      -> TimeContext.NIGHT
    }
}

// ── Badge / highlight type ────────────────────────────────────────────────────

enum class CategoryBadge {
    TOP_PICK,        // highest overall score
    FREQUENTLY_USED, // tapped ≥ 3 times
    MORNING_PICK,
    AFTERNOON_PICK,
    EVENING_PICK,
    WEEKEND_PICK,
    POPULAR,         // has time bonus but not personally boosted
}

// ── Per-category scored model ─────────────────────────────────────────────────

data class ScoredCategory(
    val category: ServiceCategoryOption,
    val score: Int,
    val badge: CategoryBadge?,
)

// ── Smart-picks provider model ────────────────────────────────────────────────

data class SmartPickProvider(
    val uid: String,
    val name: String,
    val avgRating: Double,
    val totalReviews: Int,
    val primaryServiceTitle: String,
    val bookedBefore: Boolean,
)

// ── UI state ──────────────────────────────────────────────────────────────────

data class HomeUiState(
    val scoredCategories: List<ScoredCategory> = emptyList(),
    val smartPicks: List<SmartPickProvider> = emptyList(),
    val timeContext: TimeContext = TimeContext.MORNING,
    val isLoading: Boolean = true,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val servicesRepository: CustomerServicesRepository,
    private val personalizationRepository: PersonalizationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState(isLoading = true)
            val timeContext = currentTimeContext()

            val categories = servicesRepository.getCategories()
            val tapCounts = personalizationRepository.getCategoryTapCounts()
            val bookedProviderIds = personalizationRepository.getPreviouslyBookedProviderIds()
            val topProviders = servicesRepository.getTopProviders(limit = 20)

            // ── Score each category ───────────────────────────────────────────
            val scored = categories.map { cat ->
                val tapCount = tapCounts[cat.id] ?: 0
                val tapScore = tapCount * 10
                val timeBonus = timeBonus(cat.label, timeContext)
                val totalScore = tapScore + timeBonus

                val badge: CategoryBadge? = when {
                    tapCount >= 3              -> CategoryBadge.FREQUENTLY_USED
                    timeBonus > 0              -> timeBadge(timeContext)
                    else                       -> null
                }

                ScoredCategory(category = cat, score = totalScore, badge = badge)
            }.sortedByDescending { it.score }

            // Mark the single highest-scored entry as TOP_PICK (only if it has any signal)
            val finalScored = scored.mapIndexed { idx, sc ->
                if (idx == 0 && sc.score > 0) sc.copy(badge = CategoryBadge.TOP_PICK) else sc
            }

            // ── Build smart picks ─────────────────────────────────────────────
            // Previously-booked providers first, then by rating; max 5
            val smartPicks = topProviders
                .map { p ->
                    SmartPickProvider(
                        uid = p.uid,
                        name = p.displayName,
                        avgRating = p.avgRating,
                        totalReviews = p.totalReviews,
                        primaryServiceTitle = p.serviceTitles.firstOrNull().orEmpty(),
                        bookedBefore = p.uid in bookedProviderIds,
                    )
                }
                .sortedWith(
                    compareByDescending<SmartPickProvider> { it.bookedBefore }
                        .thenByDescending { it.avgRating }
                        .thenByDescending { it.totalReviews }
                )
                .take(5)

            _state.value = HomeUiState(
                scoredCategories = finalScored,
                smartPicks = smartPicks,
                timeContext = timeContext,
                isLoading = false,
            )
        }
    }

    fun onCategoryTapped(categoryId: String) {
        viewModelScope.launch {
            personalizationRepository.recordCategoryTap(categoryId)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a time-of-day bonus score for a category label.
     * Rules are intentionally simple and transparent.
     */
    private fun timeBonus(label: String, ctx: TimeContext): Int {
        val l = label.trim().lowercase()
        return when (ctx) {
            TimeContext.MORNING -> when {
                l.contains("tutoring") || l.contains("education") -> 30
                l.contains("cleaning") -> 20
                l.contains("cooking")  -> 15
                else -> 0
            }
            TimeContext.AFTERNOON -> when {
                l.contains("handyman") || l.contains("repair") -> 25
                l.contains("lawn") || l.contains("garden")     -> 25
                l.contains("moving")                           -> 20
                l.contains("tutoring")                         -> 15
                else -> 0
            }
            TimeContext.EVENING -> when {
                l.contains("cooking")  -> 30
                l.contains("cleaning") -> 20
                l.contains("pet")      -> 15
                else -> 0
            }
            TimeContext.NIGHT -> when {
                l.contains("cleaning") -> 15
                else -> 0
            }
            TimeContext.WEEKEND -> when {
                l.contains("cleaning") -> 30
                l.contains("lawn") || l.contains("garden") -> 28
                l.contains("moving")   -> 25
                l.contains("handyman") -> 20
                l.contains("cooking")  -> 20
                l.contains("pet")      -> 15
                else -> 0
            }
        }
    }

    private fun timeBadge(ctx: TimeContext): CategoryBadge = when (ctx) {
        TimeContext.MORNING   -> CategoryBadge.MORNING_PICK
        TimeContext.AFTERNOON -> CategoryBadge.AFTERNOON_PICK
        TimeContext.EVENING   -> CategoryBadge.EVENING_PICK
        TimeContext.NIGHT     -> CategoryBadge.POPULAR
        TimeContext.WEEKEND   -> CategoryBadge.WEEKEND_PICK
    }
}


package com.smartserve.customerapp.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartserve.sharedauth.ServiceCategoryOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
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

// ── Season context ─────────────────────────────────────────────────────────────

enum class SeasonContext {
    WINTER,  // Dec – Feb  (harsh weather → indoor/repair focus)
    SPRING,  // Mar – May  (yard revival, deep clean)
    SUMMER,  // Jun – Aug  (outdoor, lawn, BBQ cooking)
    FALL,    // Sep – Nov  (prep for winter, final lawn care)
}

fun currentSeasonContext(): SeasonContext {
    return when (Calendar.getInstance().get(Calendar.MONTH)) {
        Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> SeasonContext.WINTER
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY           -> SeasonContext.SPRING
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST          -> SeasonContext.SUMMER
        else                                                    -> SeasonContext.FALL
    }
}

// ── Badge / highlight type ────────────────────────────────────────────────────

enum class CategoryBadge {
    TOP_PICK,        // highest combined score
    FREQUENTLY_USED, // tapped ≥ 3 times (personal habit)
    MORNING_PICK,
    AFTERNOON_PICK,
    EVENING_PICK,
    WEEKEND_PICK,
    SEASONAL_PICK,   // season-boosted
    POPULAR,         // any other time bonus
}

// ── Active context signals (for the info screen) ──────────────────────────────

data class ActiveSignals(
    val timeContext: TimeContext,
    val seasonContext: SeasonContext,
    val isWeekend: Boolean,
    val hourOfDay: Int,
    val month: Int,          // 0-based Calendar.MONTH
    val personalTapCount: Int, // total taps recorded for this user
    val bookedProviderCount: Int,
)

// ── Per-category scored model ─────────────────────────────────────────────────

data class ScoredCategory(
    val category: ServiceCategoryOption,
    val score: Int,
    val tapScore: Int,
    val timeScore: Int,
    val seasonScore: Int,
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
    val favoriteServices: List<CustomerServiceListing> = emptyList(),
    val timeContext: TimeContext = TimeContext.MORNING,
    val seasonContext: SeasonContext = SeasonContext.SUMMER,
    val activeSignals: ActiveSignals? = null,
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
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            servicesRepository.observeFavoriteServiceIds().collect {
                val favorites = servicesRepository.getFavoriteServices(limit = 10)
                _state.value = _state.value.copy(favoriteServices = favorites)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState(isLoading = true)
            val cal = Calendar.getInstance()
            val timeContext   = currentTimeContext()
            val seasonContext = currentSeasonContext()

            val categories        = servicesRepository.getCategories()
            val tapCounts         = personalizationRepository.getCategoryTapCounts()
            val bookedProviderIds = personalizationRepository.getPreviouslyBookedProviderIds()
            val topProviders      = servicesRepository.getTopProviders(limit = 20)
            val favorites         = servicesRepository.getFavoriteServices(limit = 10)

            val totalPersonalTaps = tapCounts.values.sum()

            // ── Score each category ───────────────────────────────────────────
            val scored = categories.map { cat ->
                val tapCount   = tapCounts[cat.id] ?: 0
                val tapScore   = tapCount * 10
                val tBonus     = timeBonus(cat.label, timeContext)
                val sBonus     = seasonBonus(cat.label, seasonContext)
                val totalScore = tapScore + tBonus + sBonus

                val badge: CategoryBadge? = when {
                    tapCount >= 3  -> CategoryBadge.FREQUENTLY_USED
                    sBonus > tBonus && sBonus > 0 -> CategoryBadge.SEASONAL_PICK
                    tBonus > 0     -> timeBadge(timeContext)
                    else           -> null
                }

                ScoredCategory(
                    category    = cat,
                    score       = totalScore,
                    tapScore    = tapScore,
                    timeScore   = tBonus,
                    seasonScore = sBonus,
                    badge       = badge,
                )
            }.sortedByDescending { it.score }

            // Mark the single highest-scored entry as TOP_PICK (only if it has any signal)
            val finalScored = scored.mapIndexed { idx, sc ->
                if (idx == 0 && sc.score > 0) sc.copy(badge = CategoryBadge.TOP_PICK) else sc
            }

            // ── Build smart picks ─────────────────────────────────────────────
            val smartPicks = topProviders
                .map { p ->
                    SmartPickProvider(
                        uid                  = p.uid,
                        name                 = p.displayName,
                        avgRating            = p.avgRating,
                        totalReviews         = p.totalReviews,
                        primaryServiceTitle  = p.serviceTitles.firstOrNull().orEmpty(),
                        bookedBefore         = p.uid in bookedProviderIds,
                    )
                }
                .sortedWith(
                    compareByDescending<SmartPickProvider> { it.bookedBefore }
                        .thenByDescending { it.avgRating }
                        .thenByDescending { it.totalReviews }
                )
                .take(5)

            val activeSignals = ActiveSignals(
                timeContext          = timeContext,
                seasonContext        = seasonContext,
                isWeekend            = timeContext == TimeContext.WEEKEND,
                hourOfDay            = cal.get(Calendar.HOUR_OF_DAY),
                month                = cal.get(Calendar.MONTH),
                personalTapCount     = totalPersonalTaps,
                bookedProviderCount  = bookedProviderIds.size,
            )

            _state.value = HomeUiState(
                scoredCategories = finalScored,
                smartPicks       = smartPicks,
                favoriteServices = favorites,
                timeContext      = timeContext,
                seasonContext    = seasonContext,
                activeSignals    = activeSignals,
                isLoading        = false,
            )
        }
    }

    fun onCategoryTapped(categoryId: String) {
        viewModelScope.launch {
            personalizationRepository.recordCategoryTap(categoryId)
        }
    }

    // ── Scoring helpers ───────────────────────────────────────────────────────

    /**
     * Time-of-day bonus. Higher value = stronger signal at this time.
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
                l.contains("cleaning")                      -> 30
                l.contains("lawn") || l.contains("garden") -> 28
                l.contains("moving")                        -> 25
                l.contains("handyman")                      -> 20
                l.contains("cooking")                       -> 20
                l.contains("pet")                           -> 15
                else -> 0
            }
        }
    }

    /**
     * Season bonus based on the current calendar month.
     * Reflects realistic demand patterns in Ottawa's climate.
     */
    private fun seasonBonus(label: String, ctx: SeasonContext): Int {
        val l = label.trim().lowercase()
        return when (ctx) {
            SeasonContext.WINTER -> when {
                l.contains("handyman") || l.contains("repair") -> 25
                l.contains("cleaning")                         -> 20
                l.contains("tutoring") || l.contains("education") -> 15
                l.contains("cooking")                          -> 10
                else -> 0
            }
            SeasonContext.SPRING -> when {
                l.contains("cleaning")                      -> 30  // spring clean
                l.contains("lawn") || l.contains("garden") -> 28
                l.contains("handyman")                      -> 15
                l.contains("moving")                        -> 15  // moving season starts
                else -> 0
            }
            SeasonContext.SUMMER -> when {
                l.contains("lawn") || l.contains("garden") -> 30
                l.contains("moving")                        -> 25  // peak moving season
                l.contains("cooking")                       -> 15
                l.contains("pet")                           -> 15
                else -> 0
            }
            SeasonContext.FALL -> when {
                l.contains("lawn") || l.contains("garden") -> 25  // last cut / leaf cleanup
                l.contains("handyman") || l.contains("repair") -> 25 // winterise home
                l.contains("cleaning")                      -> 20
                l.contains("moving")                        -> 15
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

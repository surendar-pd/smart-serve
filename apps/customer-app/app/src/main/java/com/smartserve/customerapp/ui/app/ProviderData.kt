package com.smartserve.customerapp.ui.app

internal data class ProviderItem(
    val name: String,
    val description: String,
    val categories: List<String>,
)

internal data class Service(
    val name: String,
    val description: String,
    val price: String,
) {
    val priceValue: Int get() = Regex("\\d+").find(price)?.value?.toIntOrNull() ?: 0
}

private val servicesByCategory = mapOf(
    "Cleaning" to listOf(
        Service("House Cleaning", "Standard clean of all rooms", "$80"),
        Service("Deep Clean", "Full scrub including appliances & baseboards", "$140"),
        Service("Move-out Clean", "Spotless for landlord inspection", "$180"),
    ),
    "Tutoring" to listOf(
        Service("Math Tutoring", "Algebra, calculus, and beyond", "$45/hr"),
        Service("Science Tutoring", "Physics, chemistry, biology", "$45/hr"),
        Service("Essay Review", "Editing and feedback on written work", "$35"),
    ),
    "Moving" to listOf(
        Service("Local Move", "Full home move within Ottawa", "$200"),
        Service("Packing Help", "Professional packing of your belongings", "$90"),
        Service("Furniture Assembly", "IKEA and flat-pack assembly", "$60"),
    ),
    "Lawn Care" to listOf(
        Service("Lawn Mowing", "Cut, edge, and blow clean", "$50"),
        Service("Hedge Trimming", "Neat and shaped hedges", "$70"),
        Service("Seasonal Cleanup", "Spring or fall yard cleanup", "$100"),
    ),
    "Handyman" to listOf(
        Service("General Repairs", "Drywall, fixtures, and more", "$65/hr"),
        Service("Furniture Assembly", "Any flat-pack or kit furniture", "$55"),
        Service("Interior Painting", "Per room, materials not included", "$120"),
    ),
    "Pet Care" to listOf(
        Service("Dog Walking", "30-minute neighbourhood walk", "$25"),
        Service("Pet Sitting", "At your home, per day", "$45/day"),
        Service("Pet Grooming", "Bath, brush, and trim at home", "$60"),
    ),
    "Cooking" to listOf(
        Service("Meal Prep", "Weekly batch cooking for your family", "$75"),
        Service("Private Chef", "Multi-course dinner for guests", "$150"),
        Service("Cooking Lesson", "Learn a cuisine or technique", "$80"),
    ),
)

internal fun servicesFor(categories: List<String>): List<Service> =
    categories.flatMap { servicesByCategory[it] ?: emptyList() }.distinctBy { it.name }

internal val allProviders = listOf(
    ProviderItem("Sarah M.", "Home cleaning · 4.9 ★ · 120+ jobs", listOf("Cleaning")),
    ProviderItem("Priya N.", "Deep cleaning specialist · 4.8 ★", listOf("Cleaning")),
    ProviderItem("Aisha R.", "Weekly & bi-weekly packages · 4.7 ★", listOf("Cleaning")),
    ProviderItem("David K.", "Eco-friendly, background checked · 4.8 ★", listOf("Cleaning", "Lawn Care")),
    ProviderItem("Tom H.", "Move-out & post-reno cleaning · 4.6 ★", listOf("Cleaning", "Moving")),
    ProviderItem("James T.", "Math & science tutoring · 4.8 ★", listOf("Tutoring")),
    ProviderItem("Emma L.", "English & essay coaching · 4.9 ★", listOf("Tutoring")),
    ProviderItem("Raj P.", "Physics & calculus specialist · 4.7 ★", listOf("Tutoring")),
    ProviderItem("Sam R.", "Local moves & packing · 4.7 ★", listOf("Moving")),
    ProviderItem("Luke W.", "Furniture assembly & moving · 4.6 ★", listOf("Moving", "Handyman")),
    ProviderItem("Grace T.", "Lawn mowing & edging · 4.9 ★", listOf("Lawn Care")),
    ProviderItem("Noah B.", "Garden design & maintenance · 4.8 ★", listOf("Lawn Care")),
    ProviderItem("Mike D.", "General repairs & installations · 4.7 ★", listOf("Handyman")),
    ProviderItem("Carlos J.", "Same-day availability · 4.5 ★", listOf("Handyman", "Cleaning")),
    ProviderItem("Anna K.", "Dog walking & pet sitting · 5.0 ★", listOf("Pet Care")),
    ProviderItem("Ben S.", "Pet grooming at your home · 4.9 ★", listOf("Pet Care")),
    ProviderItem("Omar F.", "Home-cooked meal prep · 4.8 ★", listOf("Cooking")),
    ProviderItem("Lisa H.", "Weekly meal plans & batch cooking · 4.7 ★", listOf("Cooking")),
)

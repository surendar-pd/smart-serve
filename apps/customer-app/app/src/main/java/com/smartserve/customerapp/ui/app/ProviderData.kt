package com.smartserve.customerapp.ui.app

internal data class ProviderItem(
    val name: String,
    val description: String,
    val categories: List<String>,
)

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

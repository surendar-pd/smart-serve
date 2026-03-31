import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")   // ADD THIS
    id("com.google.devtools.ksp")
}

/** Merge keys from monorepo root and app module (Gradle does not auto-inject custom keys from root local.properties). */
private fun loadImageKitProps(projectDir: File): Properties {
    val merged = Properties()
    listOf(
        projectDir.resolve("../../local.properties"),
        projectDir.resolve("local.properties"),
    ).forEach { f ->
        if (f.exists()) f.inputStream().use { merged.load(it) }
    }
    return merged
}

val hasGoogleServicesConfig =
    file("google-services.json").exists() ||
        file("src/debug/google-services.json").exists() ||
        file("src/release/google-services.json").exists()

if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle("google-services.json not found for :app. Skipping com.google.gms.google-services plugin.")
}

android {
    namespace = "com.smartserve.providerapp"
    compileSdk = 36

    val localProps = loadImageKitProps(rootProject.projectDir)
    val imagekitPublicKey = localProps.getProperty("IMAGEKIT_PUBLIC_KEY")?.trim().orEmpty()
        .ifBlank { (findProperty("IMAGEKIT_PUBLIC_KEY") as String?)?.trim().orEmpty() }
        .ifBlank { System.getenv("IMAGEKIT_PUBLIC_KEY")?.trim().orEmpty() }
    val imagekitPrivateKey = localProps.getProperty("IMAGEKIT_PRIVATE_KEY")?.trim().orEmpty()
        .ifBlank { (findProperty("IMAGEKIT_PRIVATE_KEY") as String?)?.trim().orEmpty() }
        .ifBlank { System.getenv("IMAGEKIT_PRIVATE_KEY")?.trim().orEmpty() }

    defaultConfig {
        applicationId = "com.smartserve.providerapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ImageKit (DO NOT hardcode secrets; use local.properties or env vars)
        buildConfigField("String", "IMAGEKIT_PUBLIC_KEY", "\"$imagekitPublicKey\"")
        buildConfigField("String", "IMAGEKIT_PRIVATE_KEY", "\"$imagekitPrivateKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.smartserve:shared-ui:1.0")
    implementation("com.smartserve:shared-auth:1.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")      //ADD
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1") //ADD
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")  // ADD

    // Hilt
    //implementation("com.google.dagger:hilt-android:2.56.1")
    //ksp("com.google.dagger:hilt-android-compiler:2.56.1")
    implementation("com.google.dagger:hilt-android:2.55")
    ksp("com.google.dagger:hilt-android-compiler:2.55")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")        // ADD

    // Coil (AsyncImage)
    implementation("io.coil-kt:coil-compose:2.6.0")                      // ADD

    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:4.3.3")         // ADD
    implementation("com.google.android.gms:play-services-maps:19.0.0")   // ADD

    // OpenStreetMap map rendering (same as customer app)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Google Play Services — GPS location
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Retrofit — HTTP client for OSRM API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp (used for ImageKit uploads)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // (No ImageKit SDK dependency; uploads use ImageKit HTTP API)
}

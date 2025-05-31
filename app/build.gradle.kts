import java.util.Properties

val localProps = Properties().apply {
    val propFile = rootProject.file("local.properties")
    if (!propFile.exists()) throw GradleException("No se encontró local.properties: define EDAMAM_APP_ID y EDAMAM_APP_KEY ahí")
    propFile.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.example.midespensa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.midespensa"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String", "EDAMAM_APP_ID",
            "\"${localProps.getProperty("EDAMAM_APP_ID") ?: throw GradleException("EDAMAM_APP_ID no definida en local.properties")}\""
        )
        buildConfigField(
            "String", "EDAMAM_APP_KEY",
            "\"${localProps.getProperty("EDAMAM_APP_KEY") ?: throw GradleException("EDAMAM_APP_KEY no definida en local.properties")}\""
        )

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    kotlin {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.realtime)
    implementation(libs.firebase.config)

    implementation(libs.firebase.storage)

    implementation(libs.coil)

    implementation(libs.androidx.material3)
    implementation(libs.material3)
    implementation (libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.espresso.core)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.work:work-runtime-ktx:2.9.0") // o versión más reciente

    implementation("com.google.mlkit:translate:17.0.2")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
}
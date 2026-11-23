import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase (arkadaş serisi) yalnızca google-services.json eklendiğinde etkinleşir.
// Dosya yoksa uygulama yine derlenir; arkadaş sekmesi kurulum mesajı gösterir.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Yayın imzası: proje kökünde keystore.properties oluşturun (README'ye bakın).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// ======= ADMOB KİMLİKLERİ =======
// Aşağıdakiler Google'ın resmi TEST kimlikleridir. Yayından önce kendi
// AdMob kimliklerinizle değiştirin (yalnızca release bloğunu).
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/9214589741"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val testRewarded = "ca-app-pub-3940256099942544/5224354917"

val releaseAppId = testAppId           // TODO: ca-app-pub-XXXXXXXX~YYYYYYYY
val releaseBanner = testBanner         // TODO: ca-app-pub-XXXXXXXX/1111111111
val releaseInterstitial = testInterstitial // TODO
val releaseRewarded = testRewarded     // TODO

android {
    namespace = "com.sudoflame.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sudoflame.app" // İsterseniz kendi paket adınızla değiştirin
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "AD_BANNER", "\"$testBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"$testInterstitial\"")
            buildConfigField("String", "AD_REWARDED", "\"$testRewarded\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["admobAppId"] = releaseAppId
            buildConfigField("String", "AD_BANNER", "\"$releaseBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"$releaseInterstitial\"")
            buildConfigField("String", "AD_REWARDED", "\"$releaseRewarded\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.work:work-runtime-ktx:2.10.2")

    // Reklam + GDPR/KVKK onayı
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")

    // Reklamları kaldır (uygulama içi satın alma)
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Arkadaş serisi
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}

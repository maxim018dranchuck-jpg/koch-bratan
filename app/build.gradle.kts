plugins {
    id("com.android.application")
}

android {
    namespace = "com.liquidkb.keyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.liquidkb.keyboard"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

plugins {
    id("com.android.application")
}

android {
    namespace = "its.madruga.wpp"
    compileSdk = 34

    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "its.madruga.wpp"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "2.24.7.79"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }


    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }



    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.jetbrains:annotations:15.0")
    compileOnly(libs.api)
    implementation(libs.circleimageview)
    implementation(libs.appcompat)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.colorpickerview)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.dexkit)
}

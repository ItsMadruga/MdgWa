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
        versionName = "2.24.5.76"
        proguardFiles()

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
    compileOnly(libs.api)
    implementation(libs.circleimageview)
    implementation(libs.appcompat)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.colorpickerview)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.dexkit)
}

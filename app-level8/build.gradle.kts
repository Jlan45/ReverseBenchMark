plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ollvmNdkDir = rootProject.file(".ollvm/ndk")
if (!ollvmNdkDir.resolve("source.properties").isFile) {
    throw GradleException("Level 5-8 require the generated OLLVM NDK overlay. Run ./scripts/setup_ollvm.sh first.")
}

android {
    namespace = "com.benchmark.level8"
    compileSdk = 34
    ndkPath = ollvmNdkDir.absolutePath

    defaultConfig {
        applicationId = "com.benchmark.level8"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DENABLE_ANTI_DEBUG=ON",
                    "-DENABLE_VMP_ENCRYPT=ON",
                    "-DUSE_OLLVM=ON"
                )
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path = file("${rootDir}/challenge-native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":challenge-native"))
    implementation("androidx.appcompat:appcompat:1.6.1")
}

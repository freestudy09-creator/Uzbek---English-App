plugins { id("com.android.application") }

android {
    namespace = "com.uzeng.languagebridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uzeng.languagebridge"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "3.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }
}

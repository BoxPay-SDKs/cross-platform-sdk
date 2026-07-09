import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

val sdkVersion = "1.0.0-beta6"

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("native.cocoapods")
    id("maven-publish")
    kotlin("plugin.serialization") version "1.9.24"
    id("org.jetbrains.compose") version "1.8.2"             // ✅ CMP
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.codingfeline.buildkonfig") version "0.15.2"
}


kotlin {
    jvmToolchain(17)
    androidTarget {
        publishAllLibraryVariants()
    }

    val xcf = XCFramework() // ✅ Create XCFramework

    iosX64 {
        binaries.framework {
            baseName = "cross-platform-sdk"
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.boxpay.crossplatformsdk"
            )
            xcf.add(this)
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "cross-platform-sdk"
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.boxpay.crossplatformsdk"
            )
            xcf.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "cross-platform-sdk"
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.boxpay.crossplatformsdk"
            )
            xcf.add(this)
        }
    }

    cocoapods {
        version = sdkVersion
        summary = "BoxPayBridge Shared SDK"
        homepage = "https://developers.boxpay.tech/"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "cross-platform-sdk"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.auth)

                // Kotlinx
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                // Navigation
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha13")

                // ✅ Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(compose.animation)

                implementation("io.github.alexzhirkevich:compottie:2.0.0") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-slf4j")
                }
                implementation("media.kamel:kamel-image:0.9.5")
                implementation("io.github.alexzhirkevich:qrose:1.0.1") {
                    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-slf4j")
                }
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation("androidx.lifecycle:lifecycle-process:2.8.7")
                implementation("com.google.android.gms:play-services-wallet:20.0.0")
                implementation("com.google.pay.button:compose-pay-button:1.2.0")
                implementation("com.revolut.payments:revolutpay:3.2.1") {
                    exclude(group = "com.squareup.okhttp3", module = "logging-interceptor")
                }
            }
        }
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

android {
    namespace = "com.crossplatform.sdk"
    compileSdk = 34
    buildFeatures.buildConfig  = true
    defaultConfig {
        minSdk = 21
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")  // Android
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

buildkonfig {
    packageName = "com.crossplatform.sdk"

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "SDK_VERSION",
            sdkVersion
        )
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}


// ✅ Publishing block

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.BoxPay-SDKs"
                artifactId = "BoxPayBridge"
                version = sdkVersion

                val androidComponent = components.findByName("release")
                if (androidComponent != null) {
                    from(androidComponent)
                }
            }
        }
    }
}

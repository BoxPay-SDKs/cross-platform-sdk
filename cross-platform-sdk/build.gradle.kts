import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

val sdkVersion = "1.0.2-beta17"

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("native.cocoapods")
    id("maven-publish")
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose") version "1.8.2"             // ✅ CMP
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.codingfeline.buildkonfig") version "0.15.2"
}


kotlin {
    jvmToolchain(17)
    androidTarget {
        publishAllLibraryVariants()
    }

    val realDeviceXcf = XCFramework("cross-platform-sdk-real-device")
    val simulatorXcf = XCFramework("cross-platform-sdk-simulator-device")

    iosArm64 {
        binaries.framework {
            baseName = "cross-platform-sdk"
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.boxpay.crossplatformsdk",
                "-Xg0",
                "-Xadd-light-debug=disable"
            )
            linkerOpts += listOf(
                "-dead_strip",
                "-Wl,-x"
            )
            realDeviceXcf.add(this)
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "cross-platform-sdk"
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.boxpay.crossplatformsdk",
                "-Xg0",
                "-Xadd-light-debug=disable"
            )
            linkerOpts += listOf("-dead_strip", "-Wl,-x")
            simulatorXcf.add(this)
        }
    }

    cocoapods {
        version = sdkVersion
        summary = "BoxPayBridge Shared SDK"
        homepage = "https://developers.boxpay.tech/"
        ios.deploymentTarget = "15.0" // ⬅️ changed from 14.1 — docs state minimum iOS 15.0 for Revolut Pay
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

                // Kotlinx
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

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
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation("androidx.lifecycle:lifecycle-process:2.8.7")
                implementation("com.google.android.gms:play-services-wallet:20.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
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
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.koin.core)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.mockk)

                implementation(libs.robolectric)
                implementation(libs.androidx.core.testing)

                implementation(libs.androidx.ui.test.junit4)
                implementation(libs.androidx.ui.test.manifest)
            }
        }
        val iosTest by creating {
            dependsOn(commonTest)
        }
        val iosArm64Test by getting {
            dependsOn(iosTest)
        }
        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }
    }
}

android {
    namespace = "com.crossplatform.sdk"
    compileSdk = 36
    buildFeatures.buildConfig  = true
    defaultConfig {
        minSdk = 21
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")  // Android
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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

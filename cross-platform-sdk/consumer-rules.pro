# ─── SLF4J (pulled in transitively by Ktor) ──────────────────────────────────
# Fixes: ERROR: R8: Missing class org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**

# ─── Ktor ─────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ─── kotlinx.serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ─── Coroutines ───────────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ─── Compottie (Lottie KMP) ───────────────────────────────────────────────────
-keep class io.github.alexzhirkevich.** { *; }
-dontwarn io.github.alexzhirkevich.**

# ─── Kamel (async image loading) ─────────────────────────────────────────────
-keep class media.kamel.** { *; }
-dontwarn media.kamel.**

# ─── Revolut Pay ──────────────────────────────────────────────────────────────
-keep class com.revolut.** { *; }
-dontwarn com.revolut.**

# ─── Affirm Android SDK ───────────────────────────────────────────────────────
-keep class com.affirm.** { *; }
-dontwarn com.affirm.**

# ─── Google Pay / Wallet ──────────────────────────────────────────────────────
-keep class com.google.android.gms.wallet.** { *; }
-dontwarn com.google.android.gms.wallet.**

# ─── ML Kit (text recognition) ────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ─── multiplatform-settings ───────────────────────────────────────────────────
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# ─── BoxPay SDK public API surface ────────────────────────────────────────────
# Android entry-point Activity (launched by merchants via Intent)
-keep class com.crossplatform.BoxPayActivity { *; }

# Elements View (merchants call BoxPayElementsView.create() via @JvmStatic)
-keep class com.crossplatform.BoxPayElementsView { *; }

# Handler merchants instantiate and pass in
-keep class com.crossplatform.sdk.data.handler.BoxPayElementsHandler { *; }
-keep class com.crossplatform.sdk.data.handler.BoxPayElementsHandler$PayableListener { *; }

# Payment response data class (serialized + returned to merchant callback)
-keep class com.crossplatform.sdk.data.model.SDKPaymentResponse { *; }

# Public enums merchants pass as parameters
-keep enum com.crossplatform.sdk.presentation.theme.BoxPayThemeMode { *; }
-keep enum com.crossplatform.sdk.domain.model.AppLanguage { *; }
-keep enum com.crossplatform.sdk.domain.model.PaymentMethodTab { *; }

# SDKPaymentResponseHandler (object merchants observe for payment result)
-keep class com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler { *; }
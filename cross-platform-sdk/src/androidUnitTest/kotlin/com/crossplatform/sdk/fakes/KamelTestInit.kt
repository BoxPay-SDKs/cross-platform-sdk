package com.crossplatform.sdk.fakes

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer

/**
 * Kamel (media.kamel:kamel-image) captures the Application context at real app startup
 * through its own `io.kamel.core.ApplicationContextInitializer`, an `androidx.startup`
 * `Initializer` triggered automatically by a manifest-declared `ContentProvider`.
 *
 * Robolectric does not run that `ContentProvider`, so under test, Kamel's context is
 * never set. The first call into `KamelImage`/`asyncPainterResource` (e.g. any screen
 * rendering a provider/bank logo via `PaymentSelectorView`/`BankComponent`) then builds
 * Kamel's lazy default `KamelConfig`, which crashes with
 * `kotlin.UninitializedPropertyAccessException: lateinit property applicationContext has
 * not been initialized` from `io.kamel.core.ApplicationContextInitializerKt.getApplicationContext`.
 *
 * `androidx.startup.AppInitializer.initializeComponent()` is the documented, public API
 * for manually triggering a specific `Initializer` when the automatic `ContentProvider`
 * discovery doesn't run (exactly this situation). It's idempotent — a component that was
 * already initialized just returns its cached result — so calling this redundantly from
 * every test's `setContent {}` is harmless.
 *
 * `io.kamel.core.ApplicationContextInitializer` is declared `internal` in Kamel's own
 * module, so it can't be referenced here as a class literal (`::class.java`) — that's a
 * Kotlin compile-time visibility check. `internal` classes are still public at the JVM
 * bytecode level, so loading the class by its fully-qualified name via reflection isn't
 * subject to that check, and lets us trigger it exactly as androidx.startup's own
 * ContentProvider would at real app startup.
 */
@Suppress("UNCHECKED_CAST")
internal fun initializeKamelApplicationContext(context: Context) {
    val initializerClass = Class.forName("io.kamel.core.ApplicationContextInitializer")
        as Class<out Initializer<Any>>
    AppInitializer.getInstance(context).initializeComponent(initializerClass)
}

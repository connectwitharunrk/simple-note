package com.arunrk.simplenote.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts Koin with this app's modules.
 *
 * Called once per process from each platform's entry point: `MainActivity` on Android,
 * `main()` on desktop, and `MainViewController` on iOS.
 *
 * @param baseUrl overrides the platform's default backend URL.
 * @param declaration a hook for platform-specific additions — `androidContext(...)` on
 *   Android, for instance — so the platform entry points do not need their own `startKoin`.
 */
fun initKoin(
    baseUrl: String? = null,
    declaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    declaration()
    modules(appModules(baseUrl))
}

# Simple Note — Kotlin Multiplatform client

This is the client half of the project. **See the [root README](../README.md)** for setup,
architecture, the API reference and testing instructions.

## Targets

| Target | Source set | Ktor engine | Default backend URL |
|---|---|---|---|
| Android | `androidMain` | OkHttp | `http://10.0.2.2:8080` |
| iOS (arm64, simulator arm64) | `iosMain` | Darwin | `http://localhost:8080` |
| Desktop (JVM) | `jvmMain` | OkHttp | `http://localhost:8080` |

Shared code lives in [`shared/src/commonMain`](./shared/src/commonMain/kotlin) — models, use
cases, networking, MVI stores and the entire Compose UI. Platform source sets contain only
what genuinely differs: the HTTP engine, the default base URL, and each platform's entry point.

## Running

Start the backend first (`cd ../note-backend && ./gradlew bootRun`), then:

```bash
./gradlew :desktopApp:run           # Desktop
./gradlew :androidApp:installDebug  # Android
```

iOS: open [`iosApp/iosApp.xcodeproj`](./iosApp) in Xcode. Requires a Mac.

## Tests

```bash
./gradlew :shared:jvmTest    # fastest — runs the shared suite on the JVM
./gradlew :shared:allTests   # every target available on this host
```

`iosSimulatorArm64Test` is reported as disabled on non-macOS hosts; simulator tests require
macOS.

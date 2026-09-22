# Repository Guidelines

## Project Structure & Module Organization

- `game-domain/src/main/kotlin/`: Android-independent models, rules, progression, tutorials, and deterministic level generation. Zero Android dependencies; runnable on the JVM alone. Domain tests live in `game-domain/src/test/kotlin/`.
- `app/src/main/kotlin/`: Android entry point, `MainViewModel`, Compose UI in `ui/`, DataStore persistence in `data/`, and procedural audio in `audio/`. Local tests are in `app/src/test/`; instrumented UI tests are in `app/src/androidTest/`.
- `app/src/main/res/`: packaged artwork and English/Spanish resources; `app/src/main/assets/licenses/` contains asset licenses. Artwork originals and prompts live in `assets/` (including `assets/cards/card_stealth.png`); only reduced-resolution exports are packaged. Skins, the launcher icon and script cards are exported with `tools/PrepareSkinAsset.java`.
- Docs (`docs/`, `README.md`) are written in **Spanish**; code comments and KDoc are English. Keep user-facing strings in both `values/` and `values-es/`.

## Architecture Notes (not obvious from filenames)

- The app is **landscape-only** (`sensorLandscape` in `AndroidManifest.xml`); the UI is designed horizontal. Do not introduce portrait layouts.
- UI observes a single `GameUiState` flow from `MainViewModel`. All gameplay actions go through `GameEngine.reduce` in `game-domain`; Compose never mutates game state directly. Rejections use `RejectReason`, not exceptions.
- `LevelGenerator.generate(seed)` / `generateScenario(seed, scenarioId)` are deterministic and validated by the engine. Keep seeded reproducibility when changing generation.
- `MainViewModel` has two constructors: the Android `Application` one and an injectable `PlayerPreferencesRepository` one used by tests.
- Free-mode selection previews use fixed seed 42 (`generateScenario(42, ...)`). `PuzzlePreview` cache keys hash in `REVISION` (currently 2): bump it whenever generator geometry or preview rendering changes, or stale previews survive.
- No CI pipelines exist in this repo; validation is manual. `local.properties` (ignored) holds `sdk.dir`.

## Build, Test, and Development Commands

Use JDK 17 with `javac` and Android SDK 37. Gradle needs a full JDK; if the default `java` resolves to the Minecraft runtime (or any JRE without `javac`), export `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (this machine's JDK). Run commands from the repository root:

- `./gradlew test`: run all local unit tests (domain + app).
- `./gradlew :game-domain:test`: run only domain tests (JVM only, fast).
- `./gradlew :app:testDebugUnitTest`: run only app local unit tests.
- `./gradlew :app:assembleDebug`: build `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew :app:installDebug`: install on a connected emulator or device, then launch the app manually.
- `./gradlew :app:lintDebug`: run Android lint before submitting changes.
- `./gradlew :app:connectedDebugAndroidTest`: run UI tests on an authorized device or emulator.

## Coding Style & Naming Conventions

Match existing Kotlin style: four-space indentation, PascalCase types and composables, camelCase functions/properties, and lowercase_underscore resource names. Keep packages under `com.aela.mainboardoverride`. Document central public APIs with KDoc. No dedicated formatter or Kotlin style linter is configured.

Keep rules in `game-domain`; route gameplay actions through `GameEngine.reduce` rather than mutating the board in Compose. Preserve seeded reproducibility. Update both `values/` and `values-es/` for user-facing text.

## Testing Guidelines

Domain tests use `kotlin.test` on JUnit Platform; app tests use JUnit 4 and Compose testing. Name classes `*Test`; use descriptive backtick names for domain cases and camelCase UI test methods. Cover rule changes in engine tests and update the GDD. Isolate test DataStore files from real profiles (`ProgressionUiTest` uses a cache-only store). No numeric coverage threshold is configured.

The domain suite is green: 51 tests, 0 failures (verified 2026-09-21), and the roadmap items A01–A10 in `docs/ROADMAP.md` section 4 are closed with recorded root causes. If a domain test fails, read that section and the engine contract first; the project expects a root-cause fix, not updated expectations. Reports live in `game-domain/build/reports/tests/test/index.html`. The instrumented suites are green (28 tests across 8 classes); run connected tests over USB, not WiFi adb (WiFi runs take ~11 min with random `No compose hierarchies` communication flakes vs ~1 min on USB).

## Commit & Pull Request Guidelines

Follow recent history with concise imperative subjects prefixed `feat:` or `fix:`. Keep commits focused. PRs should explain the behavior change, link relevant issues, report validation results, and include screenshots for UI changes. Update architecture documentation when contracts change; preserve artwork provenance and licenses.
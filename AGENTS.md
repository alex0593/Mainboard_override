# Repository Guidelines

## Project Structure & Module Organization

- `game-domain/src/main/kotlin/`: Android-independent models, rules, progression, tutorials, and deterministic level generation. Domain tests live in `game-domain/src/test/kotlin/`.
- `app/src/main/kotlin/`: Android entry point, `MainViewModel`, Compose UI in `ui/`, and DataStore persistence in `data/`. Local tests are in `app/src/test/`; instrumented UI tests are in `app/src/androidTest/`.
- `app/src/main/res/`: packaged artwork and English/Spanish resources; `app/src/main/assets/licenses/` contains asset licenses. Artwork originals and prompts live in `assets/`.
- `docs/GDD.md` describes game design; `docs/ARCHITECTURE.md` explains state flow and maintenance contracts.

## Build, Test, and Development Commands

Use JDK 17 with `javac` and Android SDK 37. Configure your SDK path in the ignored `local.properties`. Run commands from the repository root:

- `./gradlew test`: run local unit tests.
- `./gradlew :game-domain:test`: run only domain tests.
- `./gradlew :app:assembleDebug`: build `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew :app:installDebug`: install on a connected emulator or device, then launch the app manually.
- `./gradlew :app:lintDebug`: run Android lint before submitting changes.
- `./gradlew :app:connectedDebugAndroidTest`: run UI tests on an authorized device or emulator.

## Coding Style & Naming Conventions

Match existing Kotlin style: four-space indentation, PascalCase types and composables, camelCase functions/properties, and lowercase_underscore resource names. Keep packages under `com.aela.mainboardoverride`. Document central public APIs with KDoc. No dedicated formatter or Kotlin style linter is configured.

Keep rules in `game-domain`; route gameplay actions through `GameEngine.reduce` rather than mutating the board in Compose. Preserve seeded reproducibility. Update both `values/` and `values-es/` for user-facing text.

## Testing Guidelines

Domain tests use `kotlin.test` on JUnit Platform; app tests use JUnit 4 and Compose testing. Name classes `*Test`; use descriptive backtick names for domain cases and camelCase UI test methods. Cover rule changes in engine tests and update the GDD. Isolate test DataStore files from real profiles. No numeric coverage threshold is configured.

## Commit & Pull Request Guidelines

Follow recent history with concise imperative subjects prefixed `feat:` or `fix:`. Keep commits focused. PRs should explain the behavior change, link relevant issues, report validation results, and include screenshots for UI changes. Update architecture documentation when contracts change; preserve artwork provenance and licenses.

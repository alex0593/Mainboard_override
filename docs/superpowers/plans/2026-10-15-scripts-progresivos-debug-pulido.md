# Plan de implementación — Scripts progresivos + debug unlock-all + pulido UI

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Desbloquear los scripts por bandas de campaña (1–30 / 31–60 / 61–100), añadir el flag debug `unlockAll` (buildConfig), rediseñar la ventana SPOOF con chevrons, agrandar el PCB y dejar el hook de scripts desbloqueados para el futuro hub del tutorial.

**Architecture:** Todo el criterio de desbloqueo vive en `game-domain` como función pura `unlockedScripts(challengeLevel: Int?)`; `LevelGenerator` la aplica **después** del shuffle para no consumir RNG (los tableros por semilla quedan idénticos a hoy). El flag `unlockAll` se inyecta desde la app como `Boolean` plano (constructor del `MainViewModel`, `BuildConfig.DEBUG` solo en el constructor Android); release queda intacta y R8 elimina la rama. La UI de pulido (SPOOF/PCB) es local a `ScriptDialogs.kt` y `GameScreen.kt`.

**Tech Stack:** Kotlin, Jetpack Compose (landscape-only), módulo puro JVM `game-domain`, `kotlin.test`/JUnit4, tests Compose instrumentados + validación en el Samsung SM-A042M bajo `adb-lock.sh`.

**Spec:** `.superpowers/recomendaciones-coordinador-2026-10-10-mejoras.md` (encargo aprobado 2026-10-10; Anexo A de `Coordinacion/sesiones/mainboard-override.md`).

**Secuencia acordada (Anexo A):** 1) CLOAK+STUN (ya cerrado) → 2) bandas → 3) debug + pulido UI → 4) tutorial dinámico (hook). Este plan cubre 2–4.

## Decisiones ya registradas (no reabrir)

- **Bandas** = número de nivel de campaña (el desbloqueo secuencial de `challengeUnlocked` garantiza coherencia): 1–30 `PING`+`BRIDGE` · 31–60 +`SPOOF`+`CLOAK` (1.er script nuevo) · 61–100 +`KILL_PROCESS`+`STEALTH`+`STUN` (2.º script nuevo). Libres, escenarios y tutorial: los 7.
- **`bannedScripts` de `ChallengeRules` aplican encima** del desbloqueo, también con `unlockAll` (son reglas de nivel, no de progresión).
- **`BalanceTest` no se toca**: las referencias replayerean sin scripts, así que el filtro de bandas no las afecta (riesgo corregido en la ficha). En 1–30 el mazo bajará a 6 cartas.
- **Fuera de alcance:** escenarios/`startScenario` NO se desbloquean con `unlockAll` (la spec solo cita «campaña/desafíos»); el briefing de desafío no muestra scripts bloqueados; hub/selector de lecciones nuevo; DUPORT/alterar puentes; tutorial nuevo. Si el usuario quiere escenarios en debug, es un follow-up.
- `PuzzlePreview.REVISION` solo si cambia el render de previsualización (no cambia).

## Global Constraints

- **RNG intacto:** el filtro de bandas NO puede consumir aleatoriedad; los tableros por semilla deben ser byte-idénticos a los de hoy (shuffle previo idéntico, filtrado post-shuffle). Ningún `Random`/`nextInt` nuevo por el desbloqueo.
- **`unlockAll`**: `buildFeatures.buildConfig = true` en `app/build.gradle.kts`; flag con **default `false`** en el constructor primario del `MainViewModel`; solo el constructor secundario `(application)` pasa `BuildConfig.DEBUG`. Release intacta.
- **SPOOF:** sin título (`spoof_value` eliminado de `values/` y `values-es/`); panel `RoundedCornerShape(20.dp)`, `Panel.copy(alpha = .96f)`, borde `BorderStroke(1.dp, Cyan.copy(alpha = .55f))`, textura `menu_card_v1` al estilo `ConfirmGameDialog`; «antes/después» a **10sp**; textos de botón ≤12sp; **hit-targets ≥48dp** (C02) en mitades, valores, cancelar y aplicar; chevrons `▴`/`▾` en bordes superior/inferior de la columna `spoof-values` con fade ~200ms, estáticos con `LocalReducedMotion`, que desaparecen en los extremos y **no consumen taps**; se conservan `tutorial-half-*`, `tutorial-value-*`, `spoof-values`, `spoof-cancel`, `spoof-apply` (+ tags nuevos `spoof-chevron-up`/`spoof-chevron-down`).
- **PCB:** `controlsWidth = (maxWidth * .25f).coerceIn(170.dp, 230.dp)` (hoy `.29f`/190–260dp).
- **Textos EN/ES en paridad**; GDD actualizado en el mismo incremento; `docs/ROADMAP.md` y `docs/ESTADO.md` al día al cierre.
- **Dominio verde completo + `:app:lintDebug` + instrumentados verdes.** Si un test de dominio existente falla: causa raíz, nunca actualizar expectativas (sección ROADMAP 4 / contrato del motor).
- **Commits directos a `main`**, prefijo `feat:`/`fix:`/`docs:`. **Nunca** commitear `AGENTS.md` ni `web/index.html` (sucios, ajenos). Keystore/secretos fuera de git.
- **Dispositivo:** todo comando va con `$L run phone <min> '<mensaje>' -- ...` (`L=/home/aela/Coordinacion/herramientas/adb-lock.sh`); dentro del candado: `KEYCODE_WAKEUP` si `dumpsys power` no está `Awake`, `am force-stop com.aela.mainboardoverride` antes, `:app:installDebug` después. `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
- **Push solo con confirmación explícita del usuario** al cerrar.

## Review Focus

1. **Deriva de RNG por posición del filtro** (el fallo más caro: cambiaría el tablero de todos los seeds vivos) → pin golden en T1 (`challenge boards stay byte-identical to the pre-band baseline`, levels 1 y 31) + `the band filter consumes no randomness` (levels 1 y 61).
2. **Fuga del flag a tests/release** (default `true` rompería la semántica de los 33 instrumentados existentes y la store) → T2 `the lock still holds without the flag` (ctor de 2 argumentos sigue bloqueando) — todos los tests existentes usan ese ctor.
3. **Invariantes de tienda bajo `unlockAllSkins`** (que no mueva créditos ni rompa la compra clásica) → T2 `debug unlock-all grants every skin without touching credits` (créditos antes==después, idempotente) + la clase `ProgressionUiTest` completa debe seguir verde.
4. **Mazo de 6 cartas agotado a mitad de partida** (banda 1–30: 6 cartas, hueco de robo en ~turno 11) → T1 `a banded challenge plays its whole reference with the six-card pool`.
5. **Ruptura del driver de tutorial en SPOOF** (el diálogo lo mueve `PuzzleTutorialUiTest` con tags y posición inicial de scroll) → T3: replay completo de `PuzzleTutorialUiTest` + `spoof chevrons signal the scroll edges` (incluye clic en `tutorial-value-3` para probar que los chevrons no tapan taps).
6. **Panel de controles a 230dp** (botones ≥48dp, nada recortado en 1600×720) → T4: suite dirigida + volcado `uiautomator` en el Samsung.

---

### Task 1: Bandas de campaña en `game-domain`

**Files:**
- Modify: `game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/Progression.kt` (añadir `unlockedScripts`)
- Modify: `game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/LevelGenerator.kt` (firma `unlockAll` + filtro post-shuffle en `buildLevel`, líneas ~162–172)
- Test: `game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/ScriptUnlockTest.kt` (nuevo)
- Docs: `docs/GDD.md` (tabla de bandas, al final del task)

**Interfaces:**
- Consumes: `ScriptType` (`Models.kt:64`, 7 entradas), `ChallengeCatalog.level(n).rules.bannedScripts`, `LevelGenerator.generateVerified(seed, challengeLevel, scenarioId)`.
- Produces: `fun unlockedScripts(challengeLevel: Int?): Set<ScriptType>` (paquete `domain`); `fun generateChallenge(level: Int, seed: Long = ..., unlockAll: Boolean = false)`; `internal fun generateVerified(seed: Long, challengeLevel: Int? = null, scenarioId: String = "classic", unlockAll: Boolean = false)`; `buildLevel(..., unlockAll: Boolean = false)`.

- [ ] **Step 1: Pin golden ANTES de tocar producción (test rojo → pin → verde en BASE)**

Crear `ScriptUnlockTest.kt` solo con el golden (usa únicamente API existente, compila en BASE):

```kotlin
/** Pin: the band filter must not move a single board of the live seed set. */
class ScriptUnlockTest {
    private fun fingerprint(state: GameState): String = listOf(
        state.board.start, state.board.extraction, state.board.width, state.board.daemon,
        state.board.firewalls, state.board.honeypots, state.board.locks, state.board.waypoints,
        state.dominoHand, state.dominoBag,
    ).joinToString("|")

    @Test fun `challenge boards stay byte-identical to the pre-band baseline`() {
        for (level in listOf(1, 31)) {
            val state = LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level).state
            assertEquals(LEVEL_BASELINE[level], fingerprint(state), "challenge $level board drifted")
        }
    }

    private companion object {
        val LEVEL_BASELINE = mapOf(1 to "PENDING-1", 31 to "PENDING-31")
    }
}
```

- [ ] **Step 2: Ejecutar para capturar el baseline de BASE**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :game-domain:test --tests '*ScriptUnlockTest*'`
Expected: FALLA con «challenge 1 board drifted» imprimiendo el fingerprint real. Sustituir `PENDING-1`/`PENDING-31` por esos valores. Re-ejecutar → PASS (**este es el estado pre-cambio ya fijado**).

- [ ] **Step 3: Escribir el resto de tests de bandas (rojo)**

Añadir a `ScriptUnlockTest.kt` (compila contra la función aún inexistente → fallo por compilación; eso es el rojo):

```kotlin
@Test fun `the approved band table gates each campaign tier`() {
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), unlockedScripts(1))
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), unlockedScripts(30))
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK), unlockedScripts(31))
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK), unlockedScripts(60))
    assertEquals(ScriptType.entries.toSet(), unlockedScripts(61))
    assertEquals(ScriptType.entries.toSet(), unlockedScripts(100))
    assertEquals(ScriptType.entries.toSet(), unlockedScripts(null))
}

@Test fun `a challenge only deals scripts from its band`() {
    fun dealt(level: Int, unlockAll: Boolean = false) =
        LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level, unlockAll = unlockAll)
            .let { it.state.scriptHand + it.state.scriptDeck }.map { it.type }.toSet()
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), dealt(1))
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK), dealt(31))
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.CLOAK), dealt(37)) // banned SPOOF aplica encima
    assertEquals(ScriptType.entries.toSet(), dealt(61))
    assertEquals(ScriptType.entries.toSet(), dealt(5, unlockAll = true)) // flag ignora bandas…
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK) - ScriptType.SPOOF + ScriptType.SPOOF, dealt(7, unlockAll = true).let { it }) // …pero nunca banned
    assertEquals(dealt(7, unlockAll = true) - ScriptType.SPOOF, dealt(7, unlockAll = true) - ScriptType.SPOOF)
    assertFalse(ScriptType.SPOOF in dealt(7, unlockAll = true))
}

@Test fun `free play and scenarios keep every script`() {
    val free = LevelGenerator.generate(42L)
    assertEquals(ScriptType.entries.toSet(), (free.scriptHand + free.scriptDeck).map { it.type }.toSet())
    val scenario = LevelGenerator.generateScenario(42L, "ghost")
    assertEquals(ScriptType.entries.toSet(), (scenario.scriptHand + scenario.scriptDeck).map { it.type }.toSet())
}

@Test fun `the band filter consumes no randomness`() {
    for (level in listOf(1, 61)) {
        val off = LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level)
        val on = LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level, unlockAll = true)
        assertEquals(off.state.board, on.state.board, "challenge $level board depends on the flag")
        assertEquals(off.state.dominoHand to off.state.dominoBag, on.state.dominoHand to on.state.dominoBag)
        assertNotEquals(off.state.scriptDeck.map { it.type }.toSet(), on.state.scriptDeck.map { it.type }.toSet())
    }
}

@Test fun `a banded challenge plays its whole reference with the six-card pool`() {
    val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(1)!!.seed, 1)
    val pool = generated.state.scriptHand + generated.state.scriptDeck
    assertEquals(6, pool.size)
    assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), pool.map { it.type }.toSet())
    var state = generated.state
    for (action in generated.solution) {
        val transition = GameEngine.reduce(state, action)
        assertTrue(transition.events.none { it is GameEvent.Rejected }, "rejection at $action")
        state = GameEngine.reduce(transition.state, GameAction.EndTurn).state
        assertEquals(6, (state.scriptHand + state.scriptDeck).size, "cards created or destroyed")
    }
    assertEquals(GameResult.VICTORY, state.result)
}
```

Nota: en `a challenge only deals scripts from its band` deja SOLO las líneas con sentido (la 4ª/5ª del bloque `dealt(7…)` son redundantes; la aserción que vale es `assertFalse(ScriptType.SPOOF in dealt(7, unlockAll = true))`).

Run: `./gradlew :game-domain:test --tests '*ScriptUnlockTest*'`
Expected: FALLA de compilación (`unlockedScripts` no existe / parámetro `unlockAll` desconocido).

- [ ] **Step 4: Implementar `unlockedScripts` + filtro post-shuffle + `unlockAll`**

En `Progression.kt`:

```kotlin
/**
 * Scripts a campaign tier may play (approved band table 1-30 / 31-60 / 61-100).
 * Free play, scenarios and the fixed tutorial deck keep every script.
 */
fun unlockedScripts(challengeLevel: Int?): Set<ScriptType> = when {
    challengeLevel == null -> ScriptType.entries.toSet()
    challengeLevel <= 30 -> setOf(ScriptType.PING, ScriptType.BRIDGE)
    challengeLevel <= 60 -> setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK)
    else -> setOf(ScriptType.PING, ScriptType.SPOOF, ScriptType.KILL_PROCESS,
        ScriptType.BRIDGE, ScriptType.STEALTH, ScriptType.CLOAK, ScriptType.STUN)
}
```

En `LevelGenerator.kt`:
1. `generateChallenge(level: Int, seed: Long = ..., unlockAll: Boolean = false)` → pasar `unlockAll` a los `generateVerified` (los dos sitios del retry).
2. `generateVerified(..., unlockAll: Boolean = false)` → último parámetro con default; pasarlo a `buildLevel` en las dos ramas (escenario y reto — en escenario el default da igual porque `unlockedScripts(null)` son los 7).
3. `buildLevel(..., unlockAll: Boolean = false)`. En el cuerpo (~línea 162), **el shuffle se queda exactamente igual** y el filtro va después:

```kotlin
val banned = challengeLevel?.let { ChallengeCatalog.level(it)?.rules?.bannedScripts } ?: emptySet()
// Band filter AFTER the shuffle: filtering must consume no randomness, so every
// seed keeps the exact board it has today (banned rules stay pre-shuffle as before).
val scripts = (0..2).flatMap { cycle ->
    ScriptType.entries.filter { it !in banned }.map { ScriptCard("$cycle-${it.name}", it) }
}.shuffled(random).filter {
    it.type in (if (unlockAll) ScriptType.entries.toSet() else unlockedScripts(challengeLevel))
}
val state = GameState(…, scriptHand = scripts.take(STARTING_SCRIPT_HAND), scriptDeck = scripts.drop(STARTING_SCRIPT_HAND), …)
```

- [ ] **Step 5: Ejecutar el task completo de dominio**

Run: `./gradlew :game-domain:test`
Expected: BUILD SUCCESSFUL, 0 fallos (ScriptUnlockTest nuevo en verde + los 66 existentes, incluidos `BalanceTest` y `ChallengeRestrictionTest` — analizados, deben pasar sin tocar). Si un test preexistente falla → causa raíz, no actualizar expectativas.

- [ ] **Step 6: GDD**

Añadir la tabla de bandas a la sección de scripts de `docs/GDD.md` (verificar ruta exacta con `ls docs/`), incluyendo «banned aplican encima» y «libres/escenarios/tutorial: los 7».

- [ ] **Step 7: Commit**

```bash
git add game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/Progression.kt \
        game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/LevelGenerator.kt \
        game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/ScriptUnlockTest.kt
git commit -m "feat: gate campaign scripts by progress band"
```

---

### Task 2: Flag debug `unlockAll` en la app

**Files:**
- Modify: `app/build.gradle.kts:21` (`buildFeatures { compose = true; buildConfig = true }`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/MainViewModel.kt` (ctor primario +3.º param, ctor secundario, `init`, `combine`, guarda de `startChallenge`, paso a `generateChallenge`, campo en `GameUiState`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/data/PlayerPreferencesRepository.kt` (interface + `DataStorePlayerPreferencesRepository`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/PuzzleSelection.kt` (líneas ~232 y ~244)
- Test: `app/src/androidTest/kotlin/com/aela/mainboardoverride/ui/ProgressionUiTest.kt` (3 tests nuevos)

**Interfaces:**
- Consumes: `LevelGenerator.generateChallenge(level, seed, unlockAll)` (Task 1); patrón de test ya existente en `ProgressionUiTest` (`MainViewModel(app, repository)`, `compose.setContent { … collectAsState() }`, `compose.waitUntil(5000) { … }`).
- Produces: `class MainViewModel(application: Application, repository: PlayerPreferencesRepository, unlockAll: Boolean = false)` (el secundario `(application)` pasa `BuildConfig.DEBUG`); `GameUiState.unlockAll: Boolean = false`; `PlayerPreferencesRepository.unlockAllSkins()` (default vacío en la interface).

- [ ] **Step 1: Escribir los 3 tests (rojo por compilación)**

En `ProgressionUiTest.kt` (reutilizar su setup de DataStore aislado):

```kotlin
@Test fun debugUnlockAllStartsChallengesBeyondTheProgressLock() {
    val vm = MainViewModel(app, repository, unlockAll = true)
    compose.setContent {
        val state by vm.uiState.collectAsState()
        MainboardTheme { GameScreen(state, vm) {} }
    }
    compose.runOnIdle { vm.startChallenge(50) }
    compose.waitUntil(5000) { vm.uiState.value.game != null }
    assertEquals(50, vm.uiState.value.challengeLevel)
    // El flag llega al dominio: la banda 1-30 se ignora y se reparten los 7 scripts.
    val game = vm.uiState.value.game!!
    assertEquals(ScriptType.entries.toSet(), (game.scriptHand + game.scriptDeck).map { it.type }.toSet())
}

@Test fun theLockStillHoldsWithoutTheFlag() {
    val vm = MainViewModel(app, repository) // default unlockAll = false
    compose.setContent {
        val state by vm.uiState.collectAsState()
        MainboardTheme { GameScreen(state, vm) {} }
    }
    compose.runOnIdle { vm.startChallenge(50) }
    compose.waitForIdle()
    assertNull(vm.uiState.value.game)
}

@Test fun debugUnlockAllGrantsEverySkinWithoutTouchingCredits() = runBlocking {
    val repositoryRef = repository
    val creditsBefore = repositoryRef.preferences.first().credits
    val vm = MainViewModel(app, repositoryRef, unlockAll = true)
    compose.waitUntil(5000) {
        val owned = runBlocking { repositoryRef.preferences.first().ownedSkins }
        Rewards.boardSkinIds.all { Rewards.ownedKey(true, it) in owned } &&
            Rewards.dominoSkinIds.all { Rewards.ownedKey(false, it) in owned }
    }
    assertEquals(creditsBefore, repositoryRef.preferences.first().credits)
    val snapshot = repositoryRef.preferences.first().ownedSkins
    repositoryRef.unlockAllSkins() // idempotente
    assertEquals(snapshot, repositoryRef.preferences.first().ownedSkins)
}
```

(Ajustar imports: `ScriptType`, `assertNull`; `vm` no necesita `compose.setContent` en el test de grants — basta construirlo y esperar el init, pero el `waitUntil` con `runBlocking` anidado ya es el patrón del fichero.)

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :app:assembleDebugAndroidTest`
Expected: FALLA de compilación (constructor 3 args y `unlockAllSkins` no existen).

- [ ] **Step 2: Implementar**

1. `app/build.gradle.kts:21`: `buildFeatures { compose = true; buildConfig = true }`.
2. `MainViewModel.kt`:
   - `data class GameUiState(…, val unlockAll: Boolean = false, )` (al final, default `false` — los fixtures de tests no cambian).
   - `class MainViewModel(application: Application, private val repository: PlayerPreferencesRepository, private val unlockAll: Boolean = false)`.
   - Secundario: `constructor(application: Application) : this(application, DataStorePlayerPreferencesRepository(application), BuildConfig.DEBUG)` (mismo paquete → sin import).
   - `init { … ensureEquippedOwned() }` → añadir `if (unlockAll) viewModelScope.launch { repository.unlockAllSkins() }`.
   - `combine(session, repository.preferences) { game, preferences -> game.copy(preferences = preferences, unlockAll = unlockAll) }`.
   - Guard de `startChallenge`: `if (level !in 1..ChallengeCatalog.COUNT || (level > uiState.value.preferences.challengeUnlocked && !unlockAll)) return`.
   - `startChallenge(level, seed)`: `LevelGenerator.generateChallenge(level, seed, unlockAll)`.
3. `PlayerPreferencesRepository.kt`: en la interface `suspend fun unlockAllSkins() {}` (default vacío, como `buySkin`); en `DataStorePlayerPreferencesRepository`:

```kotlin
override suspend fun unlockAllSkins() {
    store.edit { values ->
        val owned = normalizeOwnedSkins(values[OWNED_SKINS] ?: emptySet())
        values[OWNED_SKINS] = owned +
            Rewards.boardSkinIds.map { Rewards.ownedKey(true, it) } +
            Rewards.dominoSkinIds.map { Rewards.ownedKey(false, it) }
    }
}
```

4. `PuzzleSelection.kt`: `unlocked = challenge.number <= state.preferences.challengeUnlocked || state.unlockAll` (línea ~232) y `if (level <= state.preferences.challengeUnlocked || state.unlockAll) GameDialog(` (línea ~244). **Escenarios intactos.**

- [ ] **Step 3: Ejecutar tests instrumentados dirigidos**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
L=/home/aela/Coordinacion/herramientas/adb-lock.sh
$L run phone 45 'MO: unlockAll ProgressionUiTest' -- sh -c 'adb shell dumpsys power | grep -q "mWakefulness = Awake" || adb shell input keyevent KEYCODE_WAKEUP; adb shell am force-stop com.aela.mainboardoverride; ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aela.mainboardoverride.ui.ProgressionUiTest && ./gradlew :app:installDebug'
```

Expected: BUILD SUCCESSFUL; XML `ProgressionUiTest` con los tests existentes + 3 nuevos, `failures="0"`. Si el candado devuelve exit 3 → pendiente, continuar con otras tareas y reintentar.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/aela/mainboardoverride/MainViewModel.kt \
        app/src/main/kotlin/com/aela/mainboardoverride/data/PlayerPreferencesRepository.kt \
        app/src/main/kotlin/com/aela/mainboardoverride/ui/PuzzleSelection.kt \
        app/src/androidTest/kotlin/com/aela/mainboardoverride/ui/ProgressionUiTest.kt
git commit -m "feat: add debug unlock-all flag for challenges and the store"
```

---

### Task 3: Ventana SPOOF al estilo ConfirmGameDialog + chevrons

**Files:**
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/ScriptDialogs.kt` (`SpoofDialog`, líneas ~58–133)
- Modify: `app/src/main/res/values/strings.xml` y `values-es/strings.xml` (borrar `spoof_value` y `spoof_scroll_hint`, línea ~68 ambas)
- Test: `app/src/androidTest/kotlin/com/aela/mainboardoverride/ui/PuzzleTutorialUiTest.kt` (1 test nuevo)
- Read: `app/src/main/kotlin/com/aela/mainboardoverride/ui/TutorialScreen.kt` (condición de apertura del diálogo, ~línea 227) y `ui/GameDialog.kt` (referencia de estilo)

**Interfaces:**
- Consumes: `LocalReducedMotion` (paquete `ui`), tags existentes, inputs `TutorialInput.Half/Value/ApplySpoof/Cancel`.
- Produces: tags nuevos `spoof-chevron-up` / `spoof-chevron-down`. **Firma de `SpoofDialog` sin cambios** (la llaman `GameScreen.kt:238` y `TutorialScreen.kt:227`).

- [ ] **Step 1: Test de chevrons (rojo)**

En `PuzzleTutorialUiTest.kt`, método nuevo que arranque la lección 6 (SPOOF) con el patrón del fichero (`vm.tutorial.start(6)` y `dispatch` de los inputs), despachando `SelectScript("SPOOF")` y `SelectTile("a")` hasta que `spoof-values` esté visible:

```kotlin
@Test fun spoofChevronsSignalTheScrollEdges() {
    // …arrancar lección 6 y despachar hasta que el diálogo SPOOF se muestre…
    compose.onNodeWithTag("spoof-values").assertExists()
    compose.waitUntil(5000) { compose.onNodeWithTag("spoof-chevron-down").assertExists() == androidx.compose.ui.test.SemanticsNodeInteraction... }
    // A value 0 (inicio): hay chevron inferior y NO superior.
    compose.onNodeWithTag("spoof-chevron-down").assertExists()
    compose.onNodeWithTag("spoof-chevron-up").assertDoesNotExist()
    // Al final de la lista: el inferior desaparece y aparece el superior (deja hasta 1s al fade).
    compose.onNodeWithTag("spoof-values").performScrollToIndex(6)
    compose.waitUntil(1000) { !compose.onNodeWithTag("spoof-chevron-down").assertDoesNotExist()... }
    // Los chevrons no tapan los taps: la fila de valores sigue siendo clicable.
    compose.onNodeWithTag("tutorial-value-3").performClick()
}
```

(Escribir las aserciones con las API reales del fichero: `waitUntil(1000) { … }` sobre presencia/ausencia — p. ej. capturar `var up = false` vía `onNodeWithTag(...).fetchSemanticsNode(...) == null` o usar `assertDoesNotExist` tras `waitForIdle()`; la única obligación: verde al final del task y probar que el clic en `tutorial-value-3` sigue funcionando.)

Run: `./gradlew :app:assembleDebugAndroidTest`
Expected: FALLA (tags `spoof-chevron-*` no existen).

- [ ] **Step 2: Restyle + chevrons en `SpoofDialog`**

1. Borrar el título: `Text(stringResource(R.string.spoof_value), …)` (línea 71).
2. `Surface`: `shape = RoundedCornerShape(20.dp)`, `color = Panel.copy(alpha = .96f)`, `border = BorderStroke(1.dp, Cyan.copy(alpha = .55f))`.
3. Contenido del `Surface`: envolver la `Column` en `Box { Image(painterResource(R.drawable.menu_card_v1), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds); Column(…) }` (mismo patrón que `GameDialog`).
4. «antes/después»: `fontSize = 12.sp` → `10.sp` (líneas 75 y 80).
5. Botones: mitades `Text(…, fontSize = 11.sp)`; cancelar/aplicar `Text(…, fontSize = 12.sp)`. **No tocar** los `heightIn(min = 48.dp)`/`height(48.dp)` ni los `testTag`.
6. Borrar la línea 98 (`spoof_scroll_hint`).
7. La `Column` que envuelve la lista queda `Box(Modifier.fillMaxWidth().height(72.dp))` con la `LazyColumn(Modifier.matchParentSize().testTag("spoof-values").border(1.dp, Cyan.copy(alpha = .35f)))` dentro (el tag sigue en la `LazyColumn`), y encima dos chevrons:

```kotlin
SpoofChevron("▴", valueList.canScrollBackward, Modifier.align(Alignment.TopCenter))
SpoofChevron("▾", valueList.canScrollForward, Modifier.align(Alignment.BottomCenter))
```

Composable privado en el mismo fichero: `Text(glyph, color = Cyan, fontSize = 12.sp, modifier = Modifier.background(Panel.copy(alpha = .92f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp).clearAndSetSemantics {})`. Si `LocalReducedMotion.current` → render condicional sin animación; si no → dentro de `AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200)))`. **Sin `clickable`/`pointerInput`** (los taps deben llegar a la fila de debajo). Imports nuevos: `Box`, `background`, `AnimatedVisibility`, `tween`, `fadeIn/fadeOut`, `clearAndSetSemantics`, `painterResource`, `ContentScale`.
8. Borrar `spoof_value` de `values/strings.xml` y `values-es/strings.xml`, y `spoof_scroll_hint` de ambas (párrafo ~línea 68).

- [ ] **Step 3: Ejecutar tests dirigidos**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
L=/home/aela/Coordinacion/herramientas/adb-lock.sh
$L run phone 45 'MO: SPOOF PuzzleTutorialUiTest' -- sh -c 'adb shell dumpsys power | grep -q "mWakefulness = Awake" || adb shell input keyevent KEYCODE_WAKEUP; adb shell am force-stop com.aela.mainboardoverride; ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aela.mainboardoverride.ui.PuzzleTutorialUiTest && ./gradlew :app:installDebug'
```

Expected: toda la clase verde — el replay completo de las 10 lecciones prueba que tags, scroll inicial y foco de tutorial siguen intactos.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/aela/mainboardoverride/ui/ScriptDialogs.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml \
        app/src/androidTest/kotlin/com/aela/mainboardoverride/ui/PuzzleTutorialUiTest.kt
git commit -m "feat: restyle the SPOOF dialog with edge chevrons"
```

---

### Task 4: PCB más grande (`controlsWidth`)

**Files:**
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/GameScreen.kt:164`

**Interfaces:**
- Consumes: `controlsWidth` (se propaga solo a `ReviewPanel` y al offset de `RotationPreview`, líneas 200 y 228).
- Produces: sin cambios de firma.

- [ ] **Step 1: Cambiar la constante**

```kotlin
val controlsWidth = (maxWidth * .25f).coerceIn(170.dp, 230.dp)
```

- [ ] **Step 2: Suite dirigida (render de pantalla de juego)**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
L=/home/aela/Coordinacion/herramientas/adb-lock.sh
$L run phone 45 'MO: PCB AccessibilityUiTest+KillBurstUiTest' -- sh -c 'adb shell dumpsys power | grep -q "mWakefulness = Awake" || adb shell input keyevent KEYCODE_WAKEUP; adb shell am force-stop com.aela.mainboardoverride; ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aela.mainboardoverride.ui.AccessibilityUiTest,com.aela.mainboardoverride.ui.KillBurstUiTest && ./gradlew :app:installDebug'
```

Expected: ambas clases verdes (hit-targets ≥48dp siguen cumpliendo C02).

- [ ] **Step 3: Volcado en el Samsung (bajo candado)**

Manteniendo el candado (o en una segunda ventana `run phone`): instalar, lanzar, entrar en una partida libre (menú → NUEVA RED) y volcar:

```bash
adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml > /tmp/opencode/mo-pcb-controls.xml
```

Verificar en el volcado (densidad 1.875 px/dp): el nodo `end-turn` tiene **alto ≥90px (48dp)** y borde derecho ≤1600; ningún nodo se sale de la pantalla. La ventana de 170dp solo aplica a anchos <680dp (en este dispositivo manda el tope 230dp ≈431px): **registrar como residual** que el suelo de 170dp no es verificable sin un dispositivo estrecho (no hay AVD).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/aela/mainboardoverride/ui/GameScreen.kt
git commit -m "feat: widen the board by shrinking the controls panel"
```

---

### Task 5: Hook de scripts desbloqueados + docs + validación final

**Files:**
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/MainViewModel.kt` (`GameUiState.unlockedScripts` + `combine`)
- Modify: `docs/ROADMAP.md`, `docs/ESTADO.md`, `README.md` (+ `docs/FUTURO.md`/`docs/PLAY.md` solo si recogen recuentos)
- Test: `app/src/androidTest/kotlin/com/aela/mainboardoverride/ui/ProgressionUiTest.kt` (1 test nuevo)

**Interfaces:**
- Consumes: `unlockedScripts(challengeLevel)` (Task 1), `unlockAll` del ctor (Task 2).
- Produces: `GameUiState.unlockedScripts: Set<ScriptType>` — **el hook que consumirá el futuro hub de tutoriales** (estado persistente de desbloqueo, sin eventos).

- [ ] **Step 1: Test del hook (rojo)**

```kotlin
@Test fun uiStateExposesTheUnlockedScriptSet() {
    val locked = MainViewModel(app, repository) // challengeUnlocked = 1
    val unlocked = MainViewModel(app, repository, unlockAll = true)
    compose.setContent {
        locked.uiState.collectAsState(); unlocked.uiState.collectAsState()
    }
    compose.waitUntil(5000) {
        locked.uiState.value.unlockedScripts == setOf(ScriptType.PING, ScriptType.BRIDGE) &&
            unlocked.uiState.value.unlockedScripts == ScriptType.entries.toSet()
    }
}
```

Run: `./gradlew :app:assembleDebugAndroidTest` → FALLA de compilación (campo inexistente).

- [ ] **Step 2: Implementar**

- `GameUiState`: `val unlockedScripts: Set<ScriptType> = ScriptType.entries.toSet()` (default = todo, fixtures intactos).
- En el `combine` de `MainViewModel`: añadir `unlockedScripts = if (unlockAll) ScriptType.entries.toSet() else unlockedScripts(preferences.challengeUnlocked)`.

- [ ] **Step 3: Docs (un solo commit al final)**

- `docs/ROADMAP.md`: marcar como hecho el punto correspondiente a desbloqueo progresivo/tutorial dinámico/PCB/SPOOF con fecha 2026-10-15 (solo con la evidencia de este incremento).
- `docs/ESTADO.md`: recuentos y estado verificado de hoy.
- `README.md`: actualizar «Estado verificado» (fecha **2026-10-15** — cierra el residual del incremento anterior) y los recuentos reales leídos de los informes.
- `docs/GDD.md`: ya tocado en Task 1; revisar coherencia final (mazo 1–30 = 6 cartas, 7 tipos = 21 cartas en libre).

- [ ] **Step 4: Validación completa**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test :app:lintDebug :app:assembleDebug
```
Expected: BUILD SUCCESSFUL, 0 fallos, lint sin errores nuevos.

```bash
L=/home/aela/Coordinacion/herramientas/adb-lock.sh
$L run phone 60 'MO: suite instrumentada completa (incremento scripts progresivos)' -- sh -c 'adb shell dumpsys power | grep -q "mWakefulness = Awake" || adb shell input keyevent KEYCODE_WAKEUP; adb shell am force-stop com.aela.mainboardoverride; ./gradlew :app:connectedDebugAndroidTest && ./gradlew :app:installDebug'
```
Expected: XML completo con `failures="0" errors="0"` (revisar el fichero, no solo el exit); el teléfono queda instalado y usable.

- [ ] **Step 5: (Opcional, no bloqueante)** Si existe `keystore.properties`: `./gradlew :app:assembleRelease` para probar que la rama `unlockAll=false` compila en release. Si no existe, anotarlo como deuda de smoke del usuario.

- [ ] **Step 6: Recuento + commits**

Leer los recuentos reales (`game-domain/build/reports/tests/…`, `app/build/outputs/androidTest-results/connected/debug/*.xml`) y reflejarlos en los docs. Commits:

```bash
git add app/src/main/kotlin/com/aela/mainboardoverride/MainViewModel.kt \
        app/src/androidTest/kotlin/com/aela/mainboardoverride/ui/ProgressionUiTest.kt
git commit -m "feat: expose the unlocked script set to the UI"
git add docs/ROADMAP.md docs/ESTADO.md README.md docs/GDD.md
git commit -m "docs: sync roadmap, status and test counts for the progressive-scripts increment"
```

**Cierre:** actualizar el ledger SDD en `.superpowers/sdd/2026-10-15-*/progress.md` (lo lleva la skill de ejecución), revisión final de rama completa, y **push solo con confirmación del usuario**.

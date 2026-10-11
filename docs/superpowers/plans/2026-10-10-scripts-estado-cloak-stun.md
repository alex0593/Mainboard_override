# Scripts de estado (CLOAK + STUN) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir dos scripts de duración —CLOAK (congela la base de rastreo 2 turnos) y STUN (pausa el avance del daemon 1 turno)— al motor `game-domain` y a la app, sin RNG nuevo.

**Architecture:** Dos scripts nuevos son entradas de `ScriptType` con estado dedicado en `GameState` (`cloakTurns`, `stunTurns`) que tickea en `GameEngine.endTurn`. La ejecución pasa por el `playCard` genérico (coste RAM, ruido, consumo de carta, rechazo sin gasto); el efecto de duración se resuelve en la fase de sistema. La app cablea el despacho inmediato (sin objetivo), sonido, háptica, ayuda y un indicador en el HUD.

**Tech Stack:** Kotlin, `game-domain` (JVM puro), Jetpack Compose, JUnit/kotlin.test. JDK 17, Android SDK 37. Sin CI (validación manual).

**Spec:** `.superpowers/spec-scripts-estado-2026-10-10.md` (decisión de producto; copia canónica en `/home/aela/Coordinacion/recomendaciones/2026-10-10-mainboard-override-scripts-spec.md`).

## Global Constraints

- **Determinismo por seed intacto**: no RNG nuevo en generación ni en `endTurn`.
- **Landscape-only** (`sensorLandscape`); no introducir layouts verticales.
- CLOAK = `ramCost 3`, `traceNoise 0`, **2 turnos**. STUN = `ramCost 3`, `traceNoise 0`, **1 turno**.
- CLOAK congela **solo** la base de 8/turno; el `pendingNoise` se sigue sumando.
- STUN: el daemon **no avanza** (no se reubica); sin daemon en el tablero → rechazo.
- Rechazos **nunca** gastan carta ni RAM (precedente `NO_PENDING_NOISE`).
- Mazo: **3 ciclos × `ScriptType.entries`** → 7 tipos = 21 cartas; mano inicial 1 → **20 en el mazo**.
- Textos EN/ES **en paridad** en `values/` y `values-es/`; docs en **español**, comentarios/KDoc en **inglés**.
- Commits `feat:` / `docs:` directos a `main`. **No** tocar `web/index.html` en commits de feature.
- `PuzzlePreview.REVISION` **no** cambia (el incremento no toca el render de previsualización).
- Preservar el estilo: 4 espacios, PascalCase en tipos/composables, `lowercase_underscore` en recursos.
- JDK 17 obligatorio: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
- **Candado de dispositivo (adb-lock):** el móvil físico es un **Samsung SM-A042M** (serial `R8YW80V050T`, `portrait 720 × 1600`, densidad ≈ 1.875), **compartido con Serpy**. Todo comando que lo toque va envuelto en `/home/aela/Coordinacion/herramientas/adb-lock.sh run phone <min> '<motivo>' -- <cmd>`; si el lock está ocupado (exit 3) se sigue con trabajo que no toque el móvil y se reintenta al final. Detalle: `.superpowers/recomendaciones-coordinador-2026-10-10-adb-lock.md`.
- La app es `sensorLandscape`: en el SM-A042M se renderiza en horizontal (1600 × 720). Re-verificar cualquier suposición de layout que dependiera del aspect ratio de la tablet anterior (CLK-LX3, ya retirada).
- Fuera de alcance: arte de carta (iconos `script_card_cloak/stun`), tutorial nuevo, mejoras de script.

## Review Focus

Fallos que la spec implica pero ningún test de tarea pisa por defecto; ordenados por probabilidad:

1. **CLOAK + STEALTH el mismo turno:** con CLOAK activo, `STEALTH` debe seguir borrando `pendingNoise` (CLOAK no debe anularla). Test en Task 1.
2. **Frontera de duración de CLOAK:** el efecto debe cubrir **exactamente** las 2 fases de sistema siguientes a la ejecución (la del propio turno incluida), no 3 ni 1. Test en Task 1.
3. **STUN sin daemon:** en un tablero sin daemon (p. ej. la base del tutorial) debe rechazar `INVALID_TARGET` sin gastar. Test en Task 2.
4. **Rechazo con efecto activo:** reintentar CLOAK/STUN con el efecto vigente debe devolver `EFFECT_ALREADY_ACTIVE` y dejar el estado intacto (RAM y mano). Tests en Task 1 y Task 2.
5. **STUN no se salta en el camino con resultado:** el `movedBoard` pausado debe aplicarse tanto si el turno continúa como si termina el match. Test en Task 2.

---

### Task 1: CLOAK — congela la base de rastreo (vertical: dominio + app)

**Files:**
- Modify: `game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/Models.kt`
- Modify: `game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/GameEngine.kt`
- Modify: `game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/ScriptRulesTest.kt`
- Modify: `game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/GameEngineTest.kt:194`
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/MainViewModel.kt` (`selectScript`, `emitDispatchCues`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/audio/SoundEffects.kt` (`SoundCue`, `renderWave`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/audio/Haptics.kt` (`hapticPattern`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/MainboardApp.kt` (`rejectionText`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/ContextHelp.kt` (`HelpTopic`, `helpTopic()`, `GeneralGameHelp`)
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/GameHeader.kt` (indicador CLOAK)
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml`

**Interfaces:**
- Produces: `ScriptType.CLOAK(ramCost = 3, traceNoise = 0)`; `GameState.cloakTurns: Int`; `RejectReason.EFFECT_ALREADY_ACTIVE`; `GameAction.PlayCloak(cardId: String)`; `SoundCue.Cloak`; `HelpTopic.CLOAK`.

- [ ] **Step 1: Escribe los tests que fallan (dominio)**

Añade a `ScriptRulesTest.kt`, reutilizando su helper `state(board)`:

> Nota: el tablero lleva una ficha colocada y una ficha jugable en la mano para que
> `resolveForcedResult` no dispare `KERNEL_PANIC` (si el turno terminara, `endTurn`
> no tickearía los contadores, igual que hoy).

```kotlin
@Test fun `cloak freezes the base trace for two turns and keeps pending noise`() {
    fun cloaked(turns: Int) = GameState(
        seed = 1,
        board = BoardState(width = 6, height = 1, start = Position(0, 0), extraction = Position(6, 0),
            placed = listOf(PlacedDomino(Domino("a", 0, 1), Position(1, 0), Orientation.HORIZONTAL))),
        dominoHand = listOf(Domino("x", 1, 2)), dominoBag = emptyList(),
        scriptHand = emptyList(), scriptDeck = emptyList(),
        tilePlacedThisTurn = true, pendingNoise = 5, cloakTurns = turns,
    )
    assertEquals(5, GameEngine.reduce(cloaked(2), GameAction.EndTurn).state.trace)
    assertEquals(1, GameEngine.reduce(cloaked(2), GameAction.EndTurn).state.cloakTurns)
    assertEquals(5, GameEngine.reduce(cloaked(1), GameAction.EndTurn).state.trace)
    assertEquals(0, GameEngine.reduce(cloaked(1), GameAction.EndTurn).state.cloakTurns)
    assertEquals(13, GameEngine.reduce(cloaked(0), GameAction.EndTurn).state.trace)
}

@Test fun `cloak spends three ram, adds no noise and is rejected while active`() {
    val initial = state(BoardState()).copy(
        scriptHand = listOf(ScriptCard("cloak", ScriptType.CLOAK)),
    )
    val cloaked = GameEngine.reduce(initial, GameAction.PlayCloak("cloak"))
    assertEquals(listOf(GameEvent.ScriptExecuted(ScriptType.CLOAK)), cloaked.events)
    assertEquals(2, cloaked.state.cloakTurns)
    assertEquals(0, cloaked.state.ram)
    assertEquals(0, cloaked.state.pendingNoise)
    assertTrue(cloaked.state.scriptHand.isEmpty())

    val active = initial.copy(cloakTurns = 2)
    val rejected = GameEngine.reduce(active, GameAction.PlayCloak("cloak"))
    assertEquals(active, rejected.state)
    assertEquals(listOf(GameEvent.Rejected(RejectReason.EFFECT_ALREADY_ACTIVE)), rejected.events)
}

@Test fun `stealth still erases pending noise while cloak is active`() {
    val base = GameState(
        seed = 1,
        board = BoardState(width = 6, height = 1, start = Position(0, 0), extraction = Position(6, 0),
            placed = listOf(PlacedDomino(Domino("a", 0, 1), Position(1, 0), Orientation.HORIZONTAL))),
        dominoHand = listOf(Domino("x", 1, 2)), dominoBag = emptyList(),
        scriptHand = listOf(ScriptCard("stealth", ScriptType.STEALTH)), scriptDeck = emptyList(),
        tilePlacedThisTurn = false, pendingNoise = 20, cloakTurns = 2,
    )
    val masked = GameEngine.reduce(base, GameAction.PlayStealth("stealth")).state
    assertEquals(0, masked.pendingNoise)
    val next = GameEngine.reduce(masked.copy(tilePlacedThisTurn = true), GameAction.EndTurn).state
    assertEquals(0, next.trace)   // frozen base + no pending noise
}
```

Actualiza `GameEngineTest.kt:194`: `assertEquals(14, generated.scriptDeck.size)` → `assertEquals(17, generated.scriptDeck.size)` (6 tipos × 3 ciclos = 18; mano 1).

- [ ] **Step 2: Ejecuta y confirma el fallo**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :game-domain:test`
Expected: FAIL de compilación — `ScriptType.CLOAK`, `cloakTurns`, `PlayCloak` y `EFFECT_ALREADY_ACTIVE` no existen.

- [ ] **Step 3: Implementa el dominio**

En `Models.kt`:
- Añade a `ScriptType`, tras `STEALTH`: `/** Freezes the base 8 of the trace clock for two system phases; pending noise still counts. */ CLOAK(3, 0),`
- En `GameState` añade `val cloakTurns: Int = 0,` (junto a `pendingNoise`).
- En `RejectReason` añade `EFFECT_ALREADY_ACTIVE,` al final.
- En `GameAction` añade `data class PlayCloak(val cardId: String) : GameAction`.

En `GameEngine.kt`:
- En `reduce`, añade la rama `is GameAction.PlayCloak -> playCard(state, action.cardId, ScriptType.CLOAK) { current -> current.copy(cloakTurns = 2) }`.
- En `playCard`, junto a los guardas de `PING`/`STEALTH`, añade:
  `if (expectedType == ScriptType.CLOAK && state.cloakTurns > 0) return rejected(state, RejectReason.EFFECT_ALREADY_ACTIVE)`.
- En `endTurn`, sustituye el cálculo de `traced` por el que congela la base y tickea el contador (**efecto primero, decremento después**):
  `val base = if (state.cloakTurns > 0) 0 else 8` y `val traced = (state.trace + base + state.pendingNoise).coerceAtMost(MAX_TRACE)`.
  Añade `cloakTurns = (state.cloakTurns - 1).coerceAtLeast(0),` al `next = state.copy(...)` (el bloque que ya reinicia `pendingNoise`, `turn`, etc.). La rama `finished` (result != null) no tickea, igual que hoy.

- [ ] **Step 4: Ejecuta el dominio y confirma el verde**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :game-domain:test`
Expected: PASS (todos los tests del dominio, incluidos los 3 nuevos).

- [ ] **Step 5: Cablea la app para CLOAK (deja el build verde)**

- `MainViewModel.selectScript`: tras la rama de `STEALTH`, añade `else if (card.type == ScriptType.CLOAK) dispatch(GameAction.PlayCloak(id))` (los no-objetivo se ejecutan al seleccionar, como PING/STEALTH).
- `MainViewModel.emitDispatchCues`, en el `when (event.type)`: añade `ScriptType.CLOAK -> SoundCue.Cloak`.
- `SoundEffects.kt`: añade `Cloak(520),` a `SoundCue` y su rama en `renderWave` (un barrido descendente filtrado, sordo/filtrado, sin transitorio agudo).
- `Haptics.kt`: añade `SoundCue.Cloak -> HapticPattern(longArrayOf(0, 40), intArrayOf(0, 90))` a `hapticPattern`.
- `MainboardApp.rejectionText`: añade `RejectReason.EFFECT_ALREADY_ACTIVE -> R.string.error_effect_already_active`.
- `ContextHelp.kt`: añade `CLOAK(R.string.help_cloak_title, R.string.help_cloak_body, ScriptType.CLOAK)` a `HelpTopic`; añade `ScriptType.CLOAK -> HelpTopic.CLOAK` a `helpTopic()`; añade `HelpTopic.CLOAK` a la lista de scripts de `GeneralGameHelp`.
- `GameHeader.kt`: dentro de la `Column` de contadores, tras `NodesIndicator(game)`, añade un indicador que solo aparece con efecto activo, con `testTag("cloak-counter")`:
  `if (game.cloakTurns > 0) Text(stringResource(R.string.cloak_active, game.cloakTurns), color = Terminal, fontSize = 11.sp, lineHeight = 13.sp, modifier = Modifier.testTag("cloak-counter"))`.
- `values/strings.xml` y `values-es/strings.xml` (mismo bloque en ambos):
  - `error_effect_already_active` → EN: `That script is already active.` / ES: `Ese script ya está activo.`
  - `cloak_active` → EN/ES: `CLOAK %1$d`
  - `help_cloak_title` → `CLOAK`
  - `help_cloak_body` → EN: `Select the card to freeze the base trace for two turns: executing the turn adds only pending noise, not the base 8. Pending noise still applies, so STEALTH remains useful. It costs 3 RAM and adds no noise. It cannot run again while active.` / ES: `Selecciona la carta para congelar el rastreo base durante dos turnos: al ejecutar el turno solo se suma el ruido pendiente, no la base de 8. El ruido pendiente sigue contando, así que STEALTH sigue siendo útil. Cuesta 3 RAM y no añade ruido. No puede ejecutarse de nuevo mientras esté activo.`

- [ ] **Step 6: Compila y pasa lint**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :app:compileDebugKotlin :app:lintDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/Models.kt \
        game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/GameEngine.kt \
        game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/ScriptRulesTest.kt \
        game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/GameEngineTest.kt \
        app/src/main/kotlin/com/aela/mainboardoverride \
        app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat: add CLOAK script freezing the base trace"
```

---

### Task 2: STUN — pausa el daemon (vertical: dominio + app)

**Files:**
- Modify: `game-domain/src/main/kotlin/com/aela/mainboardoverride/domain/Models.kt`, `GameEngine.kt`
- Modify: `game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/ScriptRulesTest.kt`
- Modify: `game-domain/src/test/kotlin/com/aela/mainboardoverride/domain/GameEngineTest.kt:194`
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/MainViewModel.kt`, `audio/SoundEffects.kt`, `audio/Haptics.kt`
- Modify: `app/src/main/kotlin/com/aela/mainboardoverride/ui/ContextHelp.kt`, `ui/GameHeader.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml`

**Interfaces:**
- Consumes: `ScriptType.CLOAK` y el patrón de Task 1.
- Produces: `ScriptType.STUN(ramCost = 3, traceNoise = 0)`; `GameState.stunTurns: Int`; `GameAction.PlayStun(cardId: String)`; `SoundCue.Stun`; `HelpTopic.STUN`.

- [ ] **Step 1: Escribe los tests que fallan**

Añade a `ScriptRulesTest.kt`:

```kotlin
@Test fun `stun pauses the daemon for one system phase then releases it`() {
    val board = BoardState(width = 6, height = 1, start = Position(0, 0), extraction = Position(6, 0),
        placed = listOf(
            PlacedDomino(Domino("a", 0, 1), Position(1, 0), Orientation.HORIZONTAL),
            PlacedDomino(Domino("b", 1, 2), Position(3, 0), Orientation.HORIZONTAL),
        ), daemon = Daemon(Position(4, 0)))
    val initial = state(board).copy(
        dominoHand = listOf(Domino("next", 2, 6)),
        scriptHand = listOf(ScriptCard("stun", ScriptType.STUN)),
    )
    // Without stun the daemon walks one connected cell toward start.
    val plain = GameEngine.reduce(initial.copy(scriptHand = emptyList(), tilePlacedThisTurn = true), GameAction.EndTurn).state
    assertEquals(Position(3, 0), plain.board.daemon!!.position)
    // With stun the system phase holds this turn, then advances next turn.
    val stunned = GameEngine.reduce(initial, GameAction.PlayStun("stun")).state
    assertEquals(1, stunned.stunTurns)
    val held = GameEngine.reduce(stunned.copy(tilePlacedThisTurn = true), GameAction.EndTurn).state
    assertEquals(Position(4, 0), held.board.daemon!!.position)
    assertEquals(0, held.stunTurns)
    val released = GameEngine.reduce(held.copy(tilePlacedThisTurn = true), GameAction.EndTurn).state
    assertEquals(Position(3, 0), released.board.daemon!!.position)
}

@Test fun `stun without a daemon is rejected untouched`() {
    val noDaemon = state(BoardState(width = 6, height = 1, start = Position(0, 0), extraction = Position(6, 0)))
        .copy(scriptHand = listOf(ScriptCard("stun", ScriptType.STUN)))
    assertEquals(noDaemon, GameEngine.reduce(noDaemon, GameAction.PlayStun("stun")).state)
    assertEquals(listOf(GameEvent.Rejected(RejectReason.INVALID_TARGET)), GameEngine.reduce(noDaemon, GameAction.PlayStun("stun")).events)
}

@Test fun `stun is rejected while active without spending`() {
    val active = state(BoardState(width = 6, height = 1, start = Position(0, 0), extraction = Position(6, 0),
        daemon = Daemon(Position(4, 0)))).copy(
        scriptHand = listOf(ScriptCard("stun", ScriptType.STUN)), stunTurns = 1,
    )
    assertEquals(active, GameEngine.reduce(active, GameAction.PlayStun("stun")).state)
    assertEquals(listOf(GameEvent.Rejected(RejectReason.EFFECT_ALREADY_ACTIVE)), GameEngine.reduce(active, GameAction.PlayStun("stun")).events)
}
```

Actualiza `GameEngineTest.kt:194` a `assertEquals(20, generated.scriptDeck.size)` (7 tipos × 3 = 21; mano 1).

- [ ] **Step 2: Ejecuta y confirma el fallo**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :game-domain:test`
Expected: FAIL de compilación — faltan `ScriptType.STUN`, `stunTurns` y `PlayStun`.

- [ ] **Step 3: Implementa el dominio**

En `Models.kt`:
- Tras `CLOAK`, añade `/** Stops the daemon from advancing for one system phase. */ STUN(3, 0),`
- En `GameState`: `val stunTurns: Int = 0,`
- En `GameAction`: `data class PlayStun(val cardId: String) : GameAction`

En `GameEngine.kt`:
- En `reduce`: `is GameAction.PlayStun -> playCard(state, action.cardId, ScriptType.STUN) { current -> current.copy(stunTurns = 1) }`.
- En `playCard`, con los demás guardas:
  `if (expectedType == ScriptType.STUN && state.stunTurns > 0) return rejected(state, RejectReason.EFFECT_ALREADY_ACTIVE)`
  `if (expectedType == ScriptType.STUN && state.board.daemon == null) return rejected(state, RejectReason.INVALID_TARGET)`
- En `endTurn`, sustituye `val movedBoard = moveDaemon(state.board)` por
  `val movedBoard = if (state.stunTurns > 0) state.board else moveDaemon(state.board)`.
  Añade `stunTurns = (state.stunTurns - 1).coerceAtLeast(0),` al `next = state.copy(...)`. La rama `finished` ya propaga `movedBoard` (pausado o no).

- [ ] **Step 4: Ejecuta el dominio y confirma el verde**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :game-domain:test`
Expected: PASS.

- [ ] **Step 5: Cablea la app para STUN**

- `MainViewModel.selectScript`: añade `else if (card.type == ScriptType.STUN) dispatch(GameAction.PlayStun(id))`.
- `MainViewModel.emitDispatchCues`: `ScriptType.STUN -> SoundCue.Stun`.
- `SoundEffects.kt`: `Stun(320)` en `SoundCue` y su rama en `renderWave` (zap corto, barrido descendente y caída rápida).
- `Haptics.kt`: `SoundCue.Stun -> HapticPattern(longArrayOf(0, 40), intArrayOf(0, 200))`.
- `ContextHelp.kt`: `HelpTopic.STUN`, `ScriptType.STUN -> HelpTopic.STUN`, y añade `HelpTopic.STUN` al grupo `SCRIPTS` de `GeneralGameHelp` (junto a CLOAK).
- `GameHeader.kt`: tras el indicador CLOAK, `if (game.stunTurns > 0) Text(stringResource(R.string.stun_active), color = Terminal, fontSize = 11.sp, lineHeight = 13.sp, modifier = Modifier.testTag("stun-counter"))`.
- `values/strings.xml` y `values-es/strings.xml`:
  - `stun_active` → `STUN`
  - `help_stun_title` → `STUN`
  - `help_stun_body` → EN: `Select the card to stop the daemon from advancing one step for the next system phase. It needs a daemon on the board; otherwise it is rejected. It costs 3 RAM and adds no noise. It cannot run again while active.` / ES: `Selecciona la carta para detener el avance del daemon durante la siguiente fase de sistema. Requiere un daemon en el tablero; sin él se rechaza. Cuesta 3 RAM y no añade ruido. No puede ejecutarse de nuevo mientras esté activo.`

- [ ] **Step 6: Compila y pasa lint**

Run: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :app:compileDebugKotlin :app:lintDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add game-domain app/src/main
git commit -m "feat: add STUN script pausing the daemon"
```

---

### Task 3: Sincronizar documentación y validar

**Files:**
- Modify: `docs/GDD.md` (tabla «Scripts incluidos», narrativa, recuento)
- Modify: `docs/ROADMAP.md` (§3.2)
- Modify: `docs/ESTADO.md`, `docs/FUTURO.md`

**Interfaces:**
- Consumes: el comportamiento ya implementado en Tasks 1–2.

- [ ] **Step 1: GDD**

En `docs/GDD.md`, tabla «Scripts incluidos» (líneas ~30-38): añade dos filas:
`| `CLOAK` | 3 | 0 | Congela el rastreo base de 8 durante dos turnos; el ruido pendiente sigue sumando. Rechaza si ya está activo. |`
`| `STUN` | 3 | 0 | El daemon no avanza en la siguiente fase de sistema. Sin daemon en el tablero se rechaza. Rechaza si ya está activo. |`
Actualiza el texto que dice «5 scripts» donde corresponda y añade una frase narrativa sobre los dos ejes nuevos (CLOAK → reloj de rastreo; STUN → reloj del daemon).

- [ ] **Step 2: ROADMAP y ESTADO/FUTURO**

- `docs/ROADMAP.md` línea ~47 (§3.2): marca CLOAK (ocultar congelar rastreo) y STUN (dañar/detener daemon) como hechos, dejando pendiente lo demás (p. ej. DUPORT / alterar puentes).
- `docs/ESTADO.md`: actualiza el recuento de tests de dominio y la lista/recuento de scripts.
- `docs/FUTURO.md`: retira o reescribe la línea de CLOAK/STUN si figura como futuro.

- [ ] **Step 3: Validación completa**

Run:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test
./gradlew :app:lintDebug :app:assembleDebug
```
Expected: dominio y app en verde; `BUILD SUCCESSFUL`; APK de debug generado.

Run (USB; móvil compartido **Samsung SM-A042M**, serial `R8YW80V050T`. **Todo comando que toca el dispositivo va envuelto en el candado** `adb-lock.sh`; ver `.superpowers/recomendaciones-coordinador-2026-10-10-adb-lock.md`). Encadena `force-stop` + instrumentada + `installDebug` en **un solo `run`** (los `run` de un mismo proyecto compiten entre sí):
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
L=/home/aela/Coordinacion/herramientas/adb-lock.sh
$L run phone 45 'MO: CLOAK/STUN suite instrumentada' -- bash -c '
  adb shell am force-stop com.aela.mainboardoverride &&
  ./gradlew :app:connectedDebugAndroidTest &&
  ./gradlew :app:installDebug'
```
Expected: suite instrumentada en verde; la app queda instalada. Si el candado está ocupado (exit 3), **no** toques el móvil: sigue con trabajo que no lo toque y reintenta al final del incremento.

Luego, con la app instalada, verifica a mano en el SM-A042M (`hold`/`release` del candado si la revisión es larga): el indicador CLOAK/STUN aparece en el HUD y el layout horizontal no se rompe. El SM-A042M es `portrait 720 × 1600` pero la app es `sensorLandscape` (1600 × 720); **re-verifica cualquier suposición de layout que dependiera del aspect ratio de la tablet anterior** (CLK-LX3, 2400 × 1080, retirada).

- [ ] **Step 4: Commit**

```bash
git add docs
git commit -m "docs: document CLOAK and STUN scripts"
```

---

## Notas de revisión (fuera de alcance)

- **Arte de carta:** `app/src/main/kotlin/.../ui/ScriptCards.kt` ya tolera tipos sin dibujo (`scriptArtwork` → `else -> null`), así que CLOAK/STUN se muestran con nombre y coste pero sin icono. Crear `script_card_cloak/stun` (origen en `assets/cards/`, export con `tools/PrepareSkinAsset.java`) queda como incremento de arte aparte.
- **Tutorial:** `TutorialCatalog.base()` reparte `ScriptType.entries`, así que CLOAK/STUN aparecen en la mano del tutorial sin cambios. Verificar que `TutorialTest` y `PuzzleTutorialUiTest` siguen verdes (esperado: sí, las cartas se añaden al final del enum y no alteran los pasos).

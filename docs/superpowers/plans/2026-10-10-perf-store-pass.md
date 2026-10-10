# Plan: Pasada de rendimiento + ajuste de tienda (2026-10-10)

## Contexto

El usuario reporta problemas de rendimiento ("el motor") en toda la app: en partida al
seleccionar/mover fichas, tirones generales, al terminar turno/mover el daemon, y en la
tienda. El `GameEngine` puro del dominio es barato (solo corre por acción de usuario); el
coste real está en cómo la UI lo conecta. Además pide que el sprite de las fichas de la
tienda sea más pequeño para que se vean los márgenes superior/inferior de la card.

## Constraints globales

- **No tocar el contrato del engine** (`game-domain`): la memoización solo cambia la
  frecuencia de llamada a `legalPlacements`/`scriptTargets`, nunca sus resultados.
  Determinismo por seed intacto.
- Landscape-only; no introducir layouts verticales.
- Estilo Kotlin del repo: 4 espacios, composables PascalCase, camelCase funciones.
- No se añaden ni cambian strings de usuario (ningún texto nuevo).
- Validación manual sin CI: `./gradlew test`, `:app:lintDebug`.
  Suite instrumentada por USB con `adb shell am force-stop` previo y `:app:installDebug`
  posterior (gotchas en `AGENTS.md`); requiere dispositivo conectado.
- No tocar `web/index.html` ni `mcp_audit.db` (ajenos/sin commitear).

## Task 1 — Tienda: sprite más pequeño y animaciones solo equipada + compradas

Archivo: `app/src/main/kotlin/com/aela/mainboardoverride/ui/ProgressionScreens.kt` (SkinGallery)

- Ficha de preview (`DominoImage`, domino "preview" 6-6): `Modifier.width(50.dp).height(100.dp)`
  → `Modifier.width(40.dp).height(80.dp)` (proporción 1:2 intacta). El overlay
  `AnimatedSkinFxWebp` del branch domino pasa a `Modifier.width(40.dp).height(80.dp)`.
- El recuadro de preview (Box de `height(100.dp)`) y la anatomía de la card (label, estado,
  reserva, botón) **no cambian** → margen visible ~15 dp arriba/abajo.
- Animación de card restringida a `owned || equipped`:
  - `val animate = owned || equipped` (calcular junto a `owned`/`equipped` existentes).
  - `.premiumCardBorder(skin.id)` → `.premiumCardBorder(if (animate) skin.id else null)`.
  - Branch pcb: `AnimatedSkinFxWebp(skin.id, Modifier.matchParentSize())` → gated por `animate`.
  - Branch domino: `AnimatedSkinFxWebp(skin.id, ...)` → gated por `animate`, size 40×80.
- `AnimatedSkinFxWebp` y `premiumCardBorder` ya son no-op con id `null` (verificado), así
  que pasar `null` basta para que la card quede estática sin cambios de layout.

## Task 2 — Partida: memoizar consultas del motor + glow compartido + una fase para la mano

Archivos: `app/src/main/kotlin/com/aela/mainboardoverride/ui/GameScreen.kt`,
`app/src/main/kotlin/com/aela/mainboardoverride/ui/Board.kt`,
`app/src/main/kotlin/com/aela/mainboardoverride/ui/ScriptCards.kt`,
`app/src/main/kotlin/com/aela/mainboardoverride/ui/HardwareHand.kt` (si aplica para P3).

### P1 — Memoizar consultas del motor (GameScreen)

Hoy `legalOrigins` se calcula inline en composición (sin `remember`) dentro del `Board`:
`GameEngine.scriptTargets(game, it, state.bridgeHorizontal)` o
`GameEngine.legalPlacements(game).filter {...}.map { it.origin }.toSet()` — se re-ejecuta en
cada recomposición (hilo principal, cientos de BFS `canPlace`), y las recomposiciones son
constantes por los glows por celda (P2).

- Envolver el cálculo actual (la lambda tal cual, sin cambiar lógica) en
  `remember(game, state.selectedScriptId, state.bridgeHorizontal, state.selectedDominoId,
  state.rotationSteps) { ... }` justo antes del `BoxWithConstraints`.
  Claves idénticas a los inputs de la lambda.

### P2 — Un solo glow compartido para el tablero (Board)

Hoy cada celda legal/target/waypoint-sin-cubrir crea su propio
`rememberInfiniteTransition(label = "cell-glow")` (700 ms), leído en **composición**
(el `.border(...)` con `glowAlpha`) → recomposición del tablero entero ~1,4×/seg.

- Hoistear un único `rememberInfiniteTransition(label = "board-glow")` en `Board` con
  `animateFloat(.55f, 1f, infiniteRepeatable(tween(700, FastOutSlowInEasing), RepeatMode.Reverse))`.
- Con `LocalReducedMotion.current` activo: sin transición, alpha estática 1f (como hoy).
- Pasar el `State<Float>` de la fase (o `null` con reduced motion) a `BoardCell`.
- En `BoardCell`, mover el glow al **draw phase**: dibujar el borde condicional
  (target/legal/waypoint) con `drawBehind` leyendo `glowPhase.value` dentro de la lambda
  (solo invalida draw, no recomposición). Mantener el resto de la cadena de modifiers
  (offset, size, testTag, padding, background, clickable, semantics) igual.
- Visual idéntico: mismo grosor/color de borde por caso (target 3dp Warning, legal 1dp
  Terminal, waypoint 1dp Warning), misma alpha animada.

### P3 — Una sola fase de animación para la mano (ScriptCards/HardwareHand)

Hoy cada ficha de la mano crea su propio `rememberInfiniteTransition(label = "tile-idle")`
(2200 ms) — 7 transiciones simultáneas; las lecturas ya son draw-phase (`drawBehind` +
`graphicsLayer`), así que el ahorro es consolidar frame clocks.

- Hoistear una única fase en el contenedor de la mano (`HardwareHand`) y pasarla a
  `DominoView` (nuevo parámetro, p. ej. `phase: State<Float>?` siendo null el fallback a
  static para otros usos de `DominoView`). Mismo aspecto: cada ficha usa
  `sin(phase.value + index * 1.3f)` con su `index` actual.
- Si `DominoView` se usa fuera de la mano sin fase disponible: mantener comportamiento
  actual para esos usos (transición propia) o fase estática de forma que no cambie lo visual.

## Task 3 — Validación

- `./gradlew test` (dominio 56 + app) y `:app:lintDebug` en verde.
- Comprobar que ningún test referencia el tamaño 50×100 ni tags de card de la tienda.
- Con dispositivo USB: baseline `dumpsys gfxinfo` (partida con ficha seleccionada) antes y
  después de los cambios; suite instrumentada con `force-stop` previo y `installDebug`
  posterior; verificación visual en CLK-LX3 (`uiautomator dump`).
- Sin dispositivo: reportar que la suite instrumentada y la medición quedan pendientes;
  no inventar resultados.

## Salida

Commits a `main` (norma del repo) con mensajes `feat:`/`fix:`/`chore:` concisos;
cerrar el bucle con el usuario reportando qué cambió, qué se midió y qué queda pendiente.
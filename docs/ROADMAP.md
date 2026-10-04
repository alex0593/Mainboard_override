# Roadmap de desarrollo — Mainboard Override

## 1. Propósito

Este documento recoge, estructura y prioriza las ideas de mejora para el juego, a partir del banco de ideas en `docs/IMPROVEMENTS.md`. No es un plan definitivo, sino una propuesta de ruta para decidir qué hacer primero y qué dejar para después.

**Revisión:** 2026-10-02. Único plan operativo del proyecto: orden,
dependencias, tareas y criterios de cierre. El estado comprobable y las
decisiones viven en [ESTADO.md](ESTADO.md). El 2026-09-20 se cerraron
A01–A09 con causas raíz registradas y B06 con reapertura limpia verificada.
El 2026-09-21 se cerró A10 con la suite instrumental completa en verde,
también cerraron C01–C06, D02–D04 y E01–E06, y se abrió el bloque F con F01
(skins Titanio/Jade/Rubí) y F02 (script STEALTH); la suite instrumental quedó
en 28/28. El 2026-09-22 se cerró F03 (skins Zafiro/Ámbar/Amatista). Hoy
(2026-10-02) se abren los bloques G (salida de alfa, versión 0.1.0) y H
(servidor propio y descarga directa en DuckDNS).
Los hilos temáticos son propuestas; no representan funciones aprobadas
ni tareas terminadas. `[ ]` significa pendiente; marcar `[x]` solo con evidencia
de cierre. Las decisiones de producto se registran antes de implementar sus ramas.

## 2. Estado actual como punto de partida

- Vertical slice con motor puro, generación determinista y validación de ruta.
- Scripts actuales: PING, SPOOF, KILL_PROCESS, BRIDGE.
- Progresión de desafíos desbloqueables y escenarios de modo libre.
- Persistencia con DataStore; compras y recompensas implementadas en el repositorio concreto.
- Última ejecución de `:game-domain:test`: 52 pruebas, 0 fallos (2026-09-22).
- Tutorial con 10 lecciones deterministas, textos EN/ES en paridad (47 instrucciones), progreso visible y avance explícito; `TutorialTest` 5/5 y `PuzzleTutorialUiTest` 6/6 en CLK-LX3 por USB.
- Audio procedural completo: soundtrack synthwave adaptativo, 16 cues de SFX, volúmenes de música/EFX en ajustes, silencio, vibración cableada (`HapticsHost`) y movimiento reducido (E04).
- Localización por-app con AppCompat (ES/EN aplican en todos los API), icono adaptativo con foreground exportado del logo (`logo_foreground.png`), capa monocroma y splash propio.
- UI distribuida en archivos por pantalla y componente (`GameScreen`, `Board`,
  menús, diálogos de scripts); `MainboardApp.kt` conserva solo el NavHost.
- Textos en español e inglés.

## 3. Hilos temáticos de trabajo

### 3.1 Jugabilidad y reglas
- Añadir modo de dificultad escalable para escenarios libres.
- Añadir modo puntuación con objetivos extra: rastreo mínimo, turnos mínimos, o ambos.
- Diversificar comportamiento del daemon: avance condicional, detección temprana, huida en casos específicos.
- Introducir nuevas amenazas u objetivos: locks, routers, células inestables, zonas de rastreo acelerado.
- Ampliar buffs: efectos de un solo uso más variados con límites claros.
- Permitir partidas con restricciones de reglas combinadas para crear tensión distinta.

### 3.2 Scripts y descarte
- Añadir scripts complementarios: ocultar rastreo temporalmente, dañar daemon, duplicar puerto, alterar puentes, etc. (Hecho parcial: STEALTH descarta el ruido pendiente del turno; el resto queda pendiente.)
- Permitir descarte o reorganización de mazo en algunas partidas.
- Ofrecer mejoras de script desbloqueables: menor coste, menor ruido, carta extra, efecto secundario controlado.
- Mejorar feedback de KILL para dejar claro qué celda o ficha se va a eliminar. (Hecho: objetivos resaltados vía `scriptTargets` + burst de destrucción con prueba.)
- Mejorar PING con más contexto útil y no solo previsualización de ficha. (Hecho: vista de próxima ficha en header + honeypot revelado resaltado.)

### 3.3 Generación y desafíos
- Mejorar variedad y equilibrio de semillas.
- Añadir validación de equilibrio para detectar niveles demasiado fáciles o muy dependientes de un solo script.
- Crear catálogo de retos con restricciones de mano.
- Ofrecer semillas amigables o didácticas.
- Añadir geometrías más variadas: huecos, columnas asimétricas, juntas irregulares.

### 3.4 UI/UX
- Extraer piezas reutilizables: header, panel de controles, indicadores de recursos.
- Mejorar accesibilidad: TalkBack, fuentes ampliadas, pantallas pequeñas, descripciones de celdas más claras.
- Añadir arrastre como interacción principal además de selección/tap.
- Mejorar feedback visual de colocación, ruido, rastreo, daemon y scripts.
- Añadir vista de cambios pendientes antes de ejecutar turno. (Hecho: banda de ruido pendiente + pista de fin de turno.)
- Actualizar previsualización de puzzles cuando cambian las skins equipadas.
- Unificar coherencia visual entre menú y partida.

### 3.5 Progresión y economía
- Validar la economía implementada: compras, saldo, recompensas e idempotencia. (Hecho y cubierto por `ProgressionUiTest`: atomicidad, migración sin retroactivos, compra/equipo, idempotencia de recompensa.)
- Añadir logros/insignias/recompensas por estilo de juego.
- Ampliar campaña con fases temáticas más claras.
- Añadir sistema de objetos/light upgrades para jugar a largo plazo.
- Permitir metas diarias/semanales en modo libre.

### 3.6 Tutorial y onboarding
- Aumentar cobertura del tutorial: scripts avanzados, errores comunes, lectura de tablero. (Hecho: lecciones de KILL y BRIDGE, ruta de entrada incorrecta, 47 instrucciones EN/ES en paridad.)
- Añadir lecciones extra o puntos de referencia desbloqueables.
- Permitir práctica sandbox dentro del tutorial. (Alcance acordado en D04: práctica guiada rejugable; sin sandbox libre.)
- Mejorar flujo entre lecciones y saltar al tema elegido. (Flujo explícito con continuar/reiniciar/siguiente; sin selector directo de lección.)

### 3.7 Sonido, VFX y atmósfera
- Conectar audio, vibración y VFX con ajustes ya persistidos. (Hecho en E04: audio, vibración cableada, partículas de recogida y flash al 80 %; glitch sostenido y estática quedan en FUTURO.)
- Añadir indicios sutiles de glitch, estática, synthwave o teclado mecánico. (Hecho synthwave, teclado mecánico y riser de urgencia; pendiente glitch visual al 80 % y estática.)
- Feedback sonoro por evento: colocación, script, trampa, daemon, victoria/derrota. (Hecho: 16 cues procedurales; daemon sin cue propia.)
- Partículas leves para rastreo alto o recogida de buffs. (Hecho en E04: destello cian al recoger y flash rojo único al cruzar 80, ambos finitos y con movimiento reducido.)

### 3.8 Calidad técnica y tests
- Aumentar tests de dominio antes de añadir reglas. (Hecho: 51 pruebas con barridos de 1000 semillas + 7×100 por escenario.)
- Añadir pruebas instrumentadas más amplias. (Hecho: 8 clases y 28 pruebas instrumentadas; A10 cerrado sin fallos pendientes.)
- Añadir validaciones masivas de semillas. (Hecho en dominio; pendiente en dispositivos reales.)
- Mejorar nombres de tests y claridad.
- Centralizar lógica de validación de UI.
- Prever invalidación de caché de previsualizaciones al cambiar geometría o renderizado. (Hecho: clave con `REVISION`.)

### 3.9 Contenido y skins
- Añadir skins temáticas coherentes con la estética. (Hecho: Cobre, Aurora, grafito, señal, obsidiana, cerámica, Titanio, Jade y Rubí + icono adaptativo y splash propio.)
- Separar mejor skins de fichas y de placa. (Hecho: galería separa fichas y PCB.)
- Ampliar textos de ayuda, ejemplos y notas de diseño en GDD. (Hecho ayuda contextual y tutorial reescrito; GDD sincronizado el 2026-09-21 con audio, vibración, VFX, puntuación, logros y meta diaria.)
- Añadir glosario de sistema para novatos.

## 4. Plan operativo y tareas

| Orden | Bloque | Prioridad | Dependencia | Resultado esperado |
| --- | --- | --- | --- | --- |
| 1 | A — Reglas y validación | P0 | Ninguna | Base comprobada y pruebas verdes |
| 2 | B — Progreso y persistencia | P0/P1 | A | Estados claros y decisiones documentadas |
| 3 | C — UI, arte y accesibilidad | P1 | A; B para progreso | Interacción consistente y verificable |
| 4 | D — Generación y tutorial | P2 | A–C | Base para ampliar contenido |
| 5 | E — Nuevas funciones | P2 | D y selección de alcance | Incrementos de contenido validados |

### A — Corregir y validar la base (P0)

Ubicación: `game-domain/src/main/kotlin/`, `game-domain/src/test/kotlin/`
y `app/src/androidTest/`. Para cada fallo, contrastar GDD, estado de prueba y
motor; registrar la causa antes de corregir código o expectativas. Documentar
un fallo por sí solo no cierra la tarea.

- [x] **A01 — Contactos:** resuelto `external mismatches reject placement but domino halves remain internally connected`. Causa: fixture desconectado — la ficha existente en (1, 3) no era vecina del inicio (-1, 3), así que el motor la rechazaba correctamente según GDD. Se fijó la existente en (0, 3) y las candidatas en (2, 3); el motor ya ignoraba el contacto interno (2 contra 5).
- [x] **A02 — Generación:** resuelto `challenge catalog contains thirty constrained solvable levels`. Causa: el nivel 2 (inicio (-1, 4), extracción (10, 6), ancho 10) necesita ≥12 celdas pero el conteo fijo era 5 dominós, y el fallback de 10 celdas tampoco era vecino del inicio. Se calcula un conteo mínimo por geometría (`(ancho + |Δy| + 1) / 2`) y el fallback se construye desde los extremos reales (espina en L + desvíos pareados). El nivel 2 queda en 6 dominós con traza 48 = límite exacto; los otros 29 niveles conservan semilla y solución.
- [x] **A03 — Derrota:** resuelto `loss takes priority over extraction when trace reaches maximum`. Causa: mismo off-by-one que A01 — las colocaciones dejaban un hueco en x=0 y `EndTurn` rechazaba con `MUST_PLACE_DOMINO`. Fijadas en (0, 3), (2, 3), (4, 3) y (6, 2)V, que encadenan valores 0-1-2-6.
- [x] **A04 — Turnos:** resuelto `challenge turn limit fails only when extraction is not reached`. Causa: colocación en (1, 3) desconectada; fijada en (0, 3). Semántica del motor intacta (el límite de turnos se comprueba tras la victoria, así que ganar en el último turno cuenta).
- [x] **A05 — Rastreo:** resuelto `challenge rules allow the exact limit and fail when trace is exceeded`. Causa: colocación en (1, 3) desconectada; fijada en (0, 3). Semántica intacta (`traced > max` permite el límite exacto 48).
- [x] **A06 — PING:** resuelto `three pings preview three tiles then reset at end turn`. Causa: colocación en (1, 2) con inicio en (-1, 2); fijada en (0, 2). El cuarto PING ya se rechazaba por RAM agotada.
- [x] **A07 — Scripts y RAM:** resuelto `ping then ram pickup allows kill in the same turn`. Causa: colocación en (1, 2) desconectada, así que el buff de RAM nunca se recogía; fijada en (0, 2). La comprobación intermedia pasó de igualdad exacta a `trap in targets` porque la ficha recién colocada también es objetivo válido de KILL según GDD (cubierto por `kill removes a complete placed domino`).
- [x] **A08 — Tutorial de dominio:** resuelto `scriptsHaveRealEffects`. Causa: el fixture de la lección 9 (tablero de 8 con ruta hasta x=5) nunca alcanzaba la extracción; se fijó el tablero a ancho 6 con extracción en (6, 2) y se añadieron dos pasos de cierre (derrotas y recompensas).
- [x] **A09 — Tutorial instrumental:** cerrado. Causa doble: (1) el panel de instrucción se autocerraba y el botón de repetir no existía a mitad de lección — repetir persistente en el header y avance explícito entre lecciones; (2) race en `AmbientSoundtrack` (write sobre un track liberado por `stop()`) que mataba el proceso de tests — `try/catch` alrededor del write. Clase `PuzzleTutorialUiTest` 6/6 en CLK-LX3 por USB.
- [x] **A10 — Cierre de validación:** cerrado 2026-09-21 — dominio 28/28 verde, `:app:testDebugUnitTest` 3/3, `:app:lintDebug` verde, suite instrumental completa 26/26 en CLK-LX3 por USB, `:app:installDebug` verificado en dispositivo. En el camino se detectó y corrigió un `fillMaxHeight` en la banda del header que colapsaba el panel (revertido; solo quedaron `lineHeight` explícitos) y se ajustó `GameHeaderUiTest` a medir la banda contra la fila del header, ya que la columna salir/reintentar la estira por encima del texto.

**Cierre:** A01–A08 pasan y la suite completa de dominio queda verde; A09 pasa
aislada y en suite; A10 no presenta fallos sin resolver. Si falta dispositivo,
registrar el bloqueo de instrumentación y mantener esa validación pendiente.

### B — Progreso, economía y persistencia (P0/P1)

Ubicación: `ProgressionScreens.kt`, `MainViewModel.kt`,
`PlayerPreferencesRepository.kt` y tests de app. Depende de A.

- [x] **B01 — Estados de escenario (P0):** se decidió persistir la victoria por escenario en `completedScenarios`; el desbloqueo sigue dependiendo de desafíos ganados y no implica haber jugado el escenario.
- [x] **B02 — Aplicar los estados (P0):** `ScenarioScreen` separa el conteo de desafíos para desbloqueo de `completedScenarios` para la marca de completado; perfiles existentes reciben el conjunto vacío sin migración retroactiva.
- [x] **B03 — Flujos de partida (P0):** verificado con `restartingChallengeKeepsTheExactSamePuzzle` (mismo puzzle al reintentar desafío) y `restartingFreeNetworkKeepsModeAndGeneratesNewSeed` (conserva modo y escenario, semilla nueva); inicio con detalle previo cubierto por `challengeOpensDetailsWithoutStartingUntilPlay` y `freePlayRequiresDetailsAndLabelsRandomExample`.
- [x] **B04 — Economía (P1):** las compras siguen dependiendo del resultado persistido de DataStore; se eliminó el estado local optimista de la galería y la prueba de progresión cubre la victoria libre idempotente junto con la persistencia de escenario.
- [x] **B05 — Reanudación (P1, decisión):** decisión registrada en `docs/GDD.md`: no se reanuda una partida tras cerrar el proceso; al reabrir se parte del menú.
- [x] **B06 — Aplicar B05 (P1):** verificado con `reopeningStartsFromCleanMenuWithoutDuplicateRewards` en `ProgressionUiTest`: tras cobrar una victoria y abandonar una partida a medias, un `MainViewModel` nuevo sobre el mismo store abre menú limpio (`game` y `reward` nulos) sin duplicar créditos. `ProgressionUiTest` 9/9 en CLK-LX3 por USB (2026-09-20).

**Cierre:** la UI distingue los estados acordados; los flujos conservan modo y
semilla; compras y pagos tienen pruebas de sus casos límite; B01 y B05 tienen
decisión registrada y sus ramas correspondientes verificadas.

### C — UI, arte y accesibilidad (P1)

Ubicación: `app/src/main/kotlin/.../ui/`, recursos EN/ES, arte y tests
instrumentados. Depende de A; las pantallas de progreso dependen también de B.

- [x] **C01 — Componentes:** cerrado 2026-09-21 — `MainboardApp.kt` pasó de 1314 a ~110 líneas (solo NavHost, rutas y mapeos de texto); `GameScreen`, `Board`, `MenuScreen`, `SettingsScreen`, `MenuWidgets`, `ScriptCards`, `ScriptDialogs` y `CircuitBackground` en archivos propios con movimientos puros. `ProgressionScreens.kt` (201 líneas) no requiere división.
- [x] **C02 — Accesibilidad:** cerrado 2026-09-21 — `Role.Button` en cartas de script, fichas de mano y celdas del tablero; objetivo «?» 46→48dp; contraste verificado por cálculo (todos los pares ≥4.8:1, AA 4.5); `AccessibilityUiTest` con partida a fuente 1.3x (sin solapes, targets ≥48dp, roles expuestos). Suite instrumental 27/27 en CLK-LX3 por USB. Revisión manual con TalkBack pendiente de dispositivo con lector en mano.
- [x] **C03 — Feedback:** rechazos traducidos (`rejectionText`), banda de ruido pendiente (`pending-trace`), objetivos de scripts resaltados (`GameEngine.scriptTargets`), texto de buff recogido y burst de destrucción de KILL con `KillBurstUiTest`.
- [x] **C04 — Previsualizaciones:** clave de caché con `REVISION = 2` e invalidación por skins (`$REVISION:id:boardSkin:dominoSkin`); `ScenarioArtworkUiTest` y la comprobación en A10.
- [x] **C05 — Cerrar arte existente:** 32 recursos `menu_background|board_|menu_card`, fondo por escenario recordado con fallback `classic`, licencias (`kenney-domino-pack`, sprites y fondos generados) y originales en `assets/`; `ScenarioArtworkUiTest` y la comprobación en A10.
- [x] **C06 — Recursos y ayuda:** 10 lecciones con 47 instrucciones en paridad EN/ES verificada; `tutorial_next_lesson`/`tutorial_lesson_complete` en uso; la auditoría de textos sin uso queda cubierta por lint en A10.

**Cierre:** compila y pasa lint; los flujos afectados tienen validación proporcional
al cambio; hay capturas de cambios visuales y evidencia de revisión con TalkBack
y fuentes grandes. No se regenera arte existente como requisito de esta fase.

### D — Generación y aprendizaje (P2)

Depende de A–C. Cada incremento conserva la reproducibilidad por semilla.

- [x] **D01 — Semillas:** conjunto reproducible en `GameEngineTest` (barrido de 1000 semillas + 7 escenarios × 100 semillas con rutas validadas); las semillas problemáticas del catálogo siguen abiertas como A02.
- [x] **D02 — Equilibrio:** cerrado 2026-09-21 — criterios en `BalanceTest`: referencia limpia (rastreo = 8×turnos, sin trampas ni ruido), 100 % de manos iniciales con jugada legal, headroom mínimo 28 en libre (fantasma cierra en 72), y las referencias de desafío dentro de presupuesto (30 entonces; 100 desde el bloque J; niveles 2, 10 y 20-30 al límite exacto). Medido sobre el conjunto D01 antes de cualquier ajuste de generación.
- [x] **D03 — Tutorial avanzado:** lecciones de KILL (7) y BRIDGE (8) con fixtures deterministas, ruta de entrada incorrecta, textos EN/ES en paridad y pruebas de finalización (`TutorialTest` 5/5, `PuzzleTutorialUiTest` 6/6).
- [x] **D04 — Práctica:** alcance acordado = práctica guiada rejugable (continuar/reiniciar/siguiente explícitos) con progreso aislado (`TutorialController` solo persiste `tutorialLesson`; sin créditos ni récords). Sandbox libre no incluido.

**Cierre:** semillas repetibles y rutas verificadas, informe de equilibrio con
criterios explícitos y nuevas lecciones completables en ambos idiomas.

### E — Ampliaciones propuestas (P2, alcance por decidir)

Depende de D. Seleccionar un incremento antes de desarrollarlo; estas opciones
no son compromisos de implementar todas las ideas del banco.

- [x] **E01 — Scripts y amenazas:** cerrado 2026-09-21 (alcance: lock) — celda visible que exige valor exacto al contactar (`LOCK_TRIPPED` si falla, consume al acertar o cubrir, KILL lo elimina por 3RAM/15ruido); 1 por mapa libre con valor coordinado con la ruta de referencia, desafíos sin locks; `LockRulesTest` 5/5, ayuda y resultado EN/ES, `REVISION=2`, GDD actualizado.
- [x] **E02 — Retos y geometrías:** cerrado 2026-09-21 (alcance: restricciones de mano) — `ChallengeRules.bannedScripts`; niveles 7, 17 y 27 sin SPOOF (el motor nunca lo reparte ahí); aviso en el detalle con textos EN/ES; `ChallengeRestrictionTest` 2/2 y referencias intactas en `BalanceTest`. Geometrías nuevas quedan como incremento futuro.
- [x] **E03 — Puntuación:** cerrado 2026-09-21 (alcance: fórmula + presentación) — `Rewards.victoryScore` (1000 − turnos×40 − rastreo, mín 0; desempate por turnos/rastreo ya existente); derivada de récords sin migrar DataStore; visible en el diálogo de resultado y en el detalle de desafío; textos EN/ES; `ScoreTest` 3/3, dominio 41/41.
- [x] **E04 — Audio, vibración y VFX:** cerrado 2026-09-21 — audio verificado; vibración cableada (`HapticsHost`, `HapticsTest` 3/3); partículas de recogida (destello cian con disciplina de sesión) y flash único al cruzar rastreo 80, ambos finitos y con movimiento reducido. Glitch visual sostenido y estática quedan como ambiente futuro.
- [x] **E05 — Campaña y recompensas:** cerrado 2026-09-21 (alcance: fases + 4 logros) — fases Red local/Enrutamiento/Sobreescritura como etiquetas; logros evaluados en la transacción de `finishMatch` con conjunto aditivo idempotente; avisos en el resultado y lista en desafíos; textos EN/ES; `CampaignTest` 2/2, dominio 43/43.
- [x] **E06 — Metas y mejoras:** cerrado 2026-09-21 (alcance: meta diaria) — victoria del día +10 créditos, una vez por día natural, idempotente dentro de `finishMatch`; aviso en el resultado y estado en el menú; textos EN/ES; dominio 44/44. Semanales, inventario y mejoras de scripts quedan como futuro (ver FUTURO).

**Cierre por incremento:** diseño acordado, implementación, validación relevante,
textos EN/ES y documentación actualizada. Las opciones sin seleccionar siguen
pendientes de definición.

### F — Contenido seleccionado (P2, alcance por incremento)

Depende de E. Cada incremento registra su decisión antes de implementar su rama.

- [x] **F01 — Skins Titanio, Jade y Rubí:** cerrado 2026-09-21 (alcance: 3 PCB premium + 3 fichas) — 6 originales en `assets/skins/originals/` con prompts versionados; 12 exportaciones (`PrepareSkinAsset`: PCB 1024×512 y preview 384×192, fichas 256×512 y preview 96×192); mapeo en `BoardSkinResources`/`dominoShellResource`, colores de punto por material (titanio `#E8F1FF`, jade `#FFD43B`, rubí `#FFB3C1`), galería y textos EN/ES; `premiumSkins` con precio 200 y fichas gratuitas; `REVISION` sin cambios. Evidencia: `SkinCatalogTest` 2/2, dominio 46/46, app 7/7, `ProgressionUiTest` 10/10, lint sin errores, suite instrumental 28/28 en CLK‑LX3 por USB.
- [x] **F02 — Script STEALTH:** cerrado 2026-09-21 (alcance: descartar ruido pendiente; sin tutorial) — `STEALTH(1, 0)` descarta `pendingNoise` completo antes de que `EndTurn` lo sume al rastreo; rechazo nuevo `NO_PENDING_NOISE` sin gastar carta ni RAM cuando no hay ruido; combos SPOOF/BRIDGE/trampa + STEALTH dentro del tope de 3 RAM y KILL(3) sin combinación; mazo autoincluido vía `ScriptType.entries` (11→14 cartas en espera); textos EN/ES de rechazo y ayuda, `SoundCue.Stealth` sintetizado y vibración en `Haptics`; ilustración original en `assets/cards/card_stealth.png` con exportación `script_card_stealth.png` 1254×1254 (prompt en `CARTAS.md`); `REVISION` sin cambios. Evidencia: `ScriptRulesTest` +5 → dominio **51/51**, app **7/7**, lint sin errores, suite instrumental 28/28 en CLK‑LX3 por USB.

- [x] **F03 — Skins Zafiro, Ámbar y Amatista:** cerrado 2026-09-22 (decisión 2026-09-22; alcance: 3 PCB premium + 3 fichas) — mismos términos que F01: PCB premium a 200 créditos vía `Rewards.premiumSkins` con ids `sapphire`, `amber`, `amethyst` y sus tres fichas gratuitas; 6 originales en `assets/skins/originals/` con prompts versionados en `sapphire-amber-amethyst-prompts.md` (tableros 1774×887 opacos con 0 px transparentes verificados, fichas 887×1774 con 20–25 % de alpha fuera de la silueta); 12 exportaciones (`PrepareSkinAsset`: PCB 1024×512 y preview 384×192, fichas 256×512 y preview 96×192); mapeo en `BoardSkinResources`/`dominoShellResource`, colores de punto por material (zafiro `#BFE7FF`, ámbar `#FFAB3D`, amatista `#D9B3FF`), galería y textos EN/ES; `REVISION` sin cambios (la clave ya incluye el id de skin). Evidencia: `SkinCatalogTest` 3/3, dominio **52/52**, app **8/8**, `ProgressionUiTest` 10/10, lint sin errores, `assembleDebug` correcto, suite instrumental **28/28** en CLK‑LX3 por USB.

**Cierre por incremento F:** igual que E — spec acordada, implementación, tests,
textos EN/ES y documentación en el mismo incremento.

### G — Salida de alfa y publicación (P1, release)

Depende de F (bloque cerrado). Objetivo: dejar el sufijo `alpha` atrás,
tener build de release firmado y la documentación lista para Google Play.

- [x] **G01 — Decisión de versión (2026-10-02):** `versionName = "0.1.0"`
  (sin sufijo alfa), `versionCode 12`, tag `v0.1.0`. Play Console exige
  `targetSdk ≥ 36` desde el 31-ago-2026: `targetSdk = 36` ya cumple.
  Evidencia: `app/build.gradle.kts` (`versionName "0.1.0"`, `versionCode 12`).
- [x] **G02 — Build de release:** `keystore.properties` (gitignored) con la
  ruta y claves de un keystore propio fuera del repositorio;
  `signingConfigs.release` + `buildTypes.release` con R8/minify;
  `assembleRelease` (APK firmada para descarga directa) y `bundleRelease`
  (AAB para Google Play, que usa Play App Signing).
  Evidencia (2026-10-02): keystore `~/.android/mainboard-override-upload.jks`
  (alias `mainboard-override`, cert SHA-256 `3af521aaa8202a90da5cbc4169e1ef3ca8fec0dc4d21aecd421976325a5aeeaf`);
  APK 54.457.058 B `504e82b4…796d`, AAB 56.698.614 B `6d0e9379…1387`,
  ambos firmados con la clave de subida; `.gitignore` excluye
  `keystore.properties` y `app/proguard-rules.pro` creado.
- [x] **G03 — QA de cierre:** dominio y app en verde, `lintRelease` sin
  errores, AAB y APK instalados en dispositivo autorizado.
  Evidencia (2026-10-03): `./gradlew test` y `:app:lintRelease` BUILD
  SUCCESSFUL (0 errores, 63 warnings); APK release (R8) instalada y lanzada en
  emulador API 35 — proceso estable, **0 `FATAL EXCEPTION`**, menú principal y
  lista de desafíos renderizados tras interacción (capturas en
  `docs/validation/release/`); AAB firmado con la misma clave. Detalle en
  `docs/validation/release/README.md`.
- [x] **G04 — Ficha de tienda:** textos EN/ES, checklist de assets, *Data
  safety* sin recolección de datos y plantilla de política de privacidad en
  `docs/PLAY.md`. Evidencia (2026-10-02): ficha creada con datos reales del
  build (paquete, SDKs, permiso único `VIBRATE`, hashes de AAB/APK).
- [ ] **G05 — Play Console (externo):** cuenta verificada; subir el AAB a
  **prueba cerrada el día 1** (cuenta personal post-13-nov-2023: 12 testers
  continuos 14 días → solicitar producción, revisión ~7 días) y publicar.
  Bloqueado por acciones externas (pago 25 USD, pasos manuales en consola).
- [x] **G06 — Sync de docs:** README, ESTADO y AGENTS con versión 0.1.0,
  comandos de release y evidencia. Evidencia (2026-10-02): README con secciones
  «Descarga directa» y «Release y publicación» + PLAY.md en el índice; ESTADO
  revisado a 2026-10-02 (decisiones G/H, deuda y comandos); AGENTS con comandos
  de release/deploy y nota de keystore.

### H — Servidor propio y descarga directa (P1)

Depende de G02 (necesita la APK firmada). DNS gestionado en DuckDNS.

- [x] **H01 — Decisión de hosting (2026-10-02):** tres nombres DuckDNS sobre
  la misma IP — `mainboard-override.duckdns.org` (landing y descarga de la
  APK), `nexxxusapp.duckdns.org` (app futura) y `nexxus-api.duckdns.org`
  (API futura de nexxus chat). Ambos últimos quedan **preparados**: certificado
  emitido y `proxy_pass` comentado hasta que exista el servicio.
  (El certificado depende del DNS: ver H03.)
- [x] **H02 — Instancia económica:** reemplazar la instancia on-demand
  `c7i-flex.large` (~66 $/mes) por **`t4g.micro`** + gp3 10 GiB en us-east-2
  (Amazon Linux 2023, SG solo 80/443 + SSM, rol IAM SSM) → **~7,3 $/mes**;
  terminar la instancia vieja. La IP pública cambia: los registros A de
  DuckDNS se re apuntan después (script con token).
  Evidencia (2026-10-02): `i-0bf2980671102b46f` (`t4g.micro`, arm64, AMI
  AL2023, IP `52.14.253.158`, SG `sg-037db6d160d1023d5`), rol
  `mainboard-override-ssm` (+ política `site-deploy-artifacts-read`), instancia
  vieja `i-0bd23db676f3f3ec3` terminada y su volumen de 50 GiB eliminado;
  acceso solo vía SSM (agente 3.3.x Online).
- [x] **H03 — nginx + TLS:** tres server blocks con certbot/Let's Encrypt,
  redirect HTTP→HTTPS, `default_server` que rechaza hosts desconocidos.
  Evidencia (2026-10-03): DNS de los tres nombres ya en la IP nueva; certificado
  Let's Encrypt de 3 SANs emitido con `certbot --nginx` (caduca 2027-01-01) y
  renovación automática verificada con `certbot renew --dry-run` (*all simulated
  renewals succeeded*); configs finales en `ops/nginx/` (`00-ssl.conf` con los
  parámetros TLS a nivel http, puerto 80 con reto ACME + `return 301`, bloque 443
  con `http2 on` por vhost y catch-all 443 con `ssl_reject_handshake on`);
  verificación pública: **HTTPS 200 (HTTP/2, `ssl_verify=0`) en los tres nombres**,
  HTTP→301, APK con MIME correcto por HTTPS y handshake rechazado en hosts
  desconocidos (curl exit 35).
- [x] **H04 — DuckDNS automático:** con token, el despliegue fija la IP en los
  tres nombres.
  Evidencia (2026-10-03): token del usuario validado contra la API real
  (`update?...&ip=52.14.253.158` → `OK` en los tres nombres); guardado en
  `.duckdns_token` (gitignored, permisos 600) y leído por
  `tools/deploy-site.sh --duckdns` si falta la variable de entorno; el script
  siempre envía **la IP pública del servidor** (`ip=$IP`, nunca la del llamador)
  para que los registros no deriven. Verificación pública HTTPS incluida en el
  paso 6 del script.
- [x] **H05 — Publicar APK:** `app-release-0.1.0.apk`, `latest.apk` y
  `SHA256SUMS` en `/var/www/mainboard-override/downloads/`.
  Evidencia (2026-10-02): APK 54.457.058 B en el servidor con
  `sha256sum -c: OK` (`504e82b4…796d`), `SHA256SUMS` publicado, `latest.apk`
  como symlink y MIME `application/vnd.android.package-archive` verificado
  con `curl -I`.
- [x] **H06 — Landing:** página estática en español (descripción, requisitos
  Android 8+ y solo horizontal, botón de descarga con versión, tamaño y
  SHA-256, enlace a itch.io).
  Evidencia (2026-10-02): `web/` publicado y verificado (HTML/CSS/PNG con sus
  MIME correctos); además, página de **Nexus Chat** en
  `nexxxusapp.duckdns.org` (guía de estilo neón del proyecto) y placeholder
  en `nexxus-api`.
- [x] **H07 — Despliegue repetible:** `tools/deploy-site.sh` (APK release +
  checksum + IP DuckDNS + subida vía AWS CLI/SSM).
  Evidencia (2026-10-02): script creado; modos site-only y `--apk` probados
  extremo a extremo (staging, `params.json` válido, sed idempotente de la
  landing) y tramos reales verificados por SSM (descarga S3 con el perfil de
  la instancia `sha256sum -c: OK`, recarga de nginx, verificación 200/444).
- [x] **H08 — Sync de docs:** README «Descarga directa», ESTADO (URLs, coste,
  deuda) y AGENTS (comandos de despliegue). Evidencia (2026-10-02): los tres
  documentos actualizados (URLs, ~7,3 $/mes, pendientes H03/H04/G05, comandos
  de release y deploy).

**Cierre de G/H:** G cierra con la suite verde, AAB y APK firmados, docs y
ficha de tienda; H cierra con los tres vhosts en HTTPS respondiendo, landing
descargable con checksum verificado y despliegue reproducible de una versión
nueva. Los pasos externos de Play (G05) se registran con su fecha.

### I — Ronda de audio, animación y pulido (P1)

Detectado en pruebas sobre el CLK-LX3 por WiFi (2026-10-03): los SFX de botón
suena(n) mudos tras ~1 s y la música del menú se percibe muy baja, además de
las mejoras de UI aprobadas (animaciones de tablero/HUD, tutorial y texturas
suavizadas). Los volúmenes se descartan como causa leyendo el DataStore del
dispositivo (`music_volume=1.0`, `sfx_volume=1.0`, `audio` ausente → true).

- [x] **I01 — SFX mudos:** instrumentar `SoundEffects` (código de `write`,
  `track.state` tras `build()`, cues descartados y por qué) y hacer el
  mezclador **autorecuperable**: si `write` falla sin `release` externo,
  recrear la pista conservando las voces en cola; verificar
  `STATE_INITIALIZED` y no dejar voces varadas en `MAX_CONCURRENT`.
  Evidencia: la pista #107 nació 11:50:29.944 y murió a 1,01 s.
  *Hecho 2026-10-03:* `runMixer()` con re-adquisición (`RETRY_WAIT_MS=250`,
  `MAX_TRACK_FAILURES=5`), verificación de `STATE_INITIALIZED` y logs de
  `write`/`track.state`. Falta confirmación audible con logcat limpio.
- [x] **I02 — Música de menú baja:** subir el nivel del escena `MENU`
  (energía .18 → .34: pads+sub más fuertes y entra el bajo ligero, sin
  batería) y ganancia de pads (.028 → .038); autorecuperación también en
  `AmbientSoundtrack` (la pista #105 murió a 0,3 s con «write failed»).
  *Hecho 2026-10-03:* escena `MENU` a .34, pads .038, sub .026; intercambio
  de pista con contador `generation` bajo lock y `render(track, generation)`
  autorecuperable con logs de `start`/`stop`. Falta confirmación audible.
- [x] **I03 — Animaciones de tablero:** glow pulsante en celdas
  legales/objetivo, daemon con desplazamiento animado entre celdas y flash al
  descubrir honeypot (reusa la ráfaga existente en color Warning).
  *Hecho 2026-10-03:* glow `Warning`/`Terminal` .55→1 en `BoardCell`
  (estático con reduced-motion), overlay `D!` con `animateDpAsState`
  (`daemon-x/y`), `DestructionBurst` `honeypot-burst` al descubrir
  (`board.revealedHoneypots` en diff, sin flash en la primera captura).
- [x] **I04 — HUD y feedback:** contadores animados (RAM, rastreo, saldo) y
  micro-glitch de entrada en el diálogo de rechazo (banco §4/§7: «indicios de
  glitch» y «feedback visual de acciones»).
  *Hecho 2026-10-03:* `animateIntAsState` en turno/RAM/rastreo
  (`GameHeader`) y `animatedCount()` en saldo (`MenuScreen`,
  `ProgressionScreens`); `GameDialog.glitch` con jitter senoidal decaying
  (340 ms) activo solo en el diálogo de error y desactivado con reduced-motion.
- [x] **I05 — Tutorial:** barra de progreso persistente (lección + paso),
  transición con fundido entre lecciones, botón «saltar lección» y resaltado
  pulsante de la celda esperada.
  *Hecho 2026-10-03:* franja `tutorial-lesson-progress` +
  `tutorial-step-progress` siempre visible, fundido `lessonFade` (260 ms) en
  cabecera/franja/tablero, `controller.skip()` + botón
  `tutorial-skip-lesson` (cadenas en `tutorial.xml` de ambos idiomas); el
  resaltado pulsante de la celda esperada lo aporta el glow de I03.
- [x] **I06 — Texturas suavizadas:** decodificación con `inSampleSize` acotada
  al tamaño de pantalla para el arte con reducciones fuertes (sprites de celda
  del tablero y cartas de scripts), porque en Android `FilterQuality` solo
  alterna nearest/bilineal sin mipmaps (fuente: `AndroidPaint.android.kt`).
  *Hecho 2026-10-03:* `ui/ArtworkQuality.kt` (`reduceToDisplay` con
  bipartición progresiva 50 % hasta banda [display, 2×display], `LruCache` de
  16 MB) aplicado a sprites de tablero (`BoardThreatImage`/puentes/buffs/
  puertos), cartas compactas y `DominoImage` con `artMax`;
  `PuzzlePreview.REVISION` 2 → 3.
- [~] **I07 — Validación y cierre:** `./gradlew test`, `:app:lintDebug`,
  prueba manual en CLK-LX3 por WiFi (la realiza el usuario), sync de docs
  (GDD/ESTADO/IMPROVEMENTS) y commits por incremento.
  *2026-10-03:* `./gradlew test` verde, `:app:lintDebug` verde,
  `:app:connectedDebugAndroidTest` **28/28** en `emulator-5554` (los 2 fallos
  de `ContextHelpUiTest` eran entorno: display del AVD a 320dp frente a los
  640×360 que asume el test; corregido con `wm size 640x360`, no es código).
  Pendiente: prueba de audio/animaciones en el CLK-LX3 con logcat limpio
  (usuario). Commits hechos: `fix:` audio, `feat:` texturas,
  `feat:` animaciones/HUD, `feat:` tutorial y `docs:` (este bloque).

**Cierre de I:** los dos bugs de audio verificados con logcat limpio en el
dispositivo (SFX audibles en cada cue, música de menú perceptible a volumen
medio) y las mejoras de UI visibles en la misma prueba.

### J — Campaña a 100, tableros grandes y nodos de datos (P2)

Propuesta y decisiones del usuario (2026-10-03, plan aprobado): la ruta
conectada debe **barrer** los nodos obligatorios (sin interacción nueva),
el tablero más grande solo afecta a desafíos altos (1–30 intactos) y la
campaña pasa a 100 desafíos en cinco fases de 20 (Firewall y Singularity
como fases nuevas).

- [x] **J01 — 100 desafíos y 5 fases:** `ChallengeCatalog.COUNT` 30 → 100,
  `challengePhase` por bandas de 20, bucle de fases `0..4` y claves
  `phase_firewall`/`phase_singularity` en ambos idiomas; curva de reglas
  por banda (31–60: 11 turnos/88 rastreo con nivel exacto cada 5.º;
  61–100: 12/96 con exacto cada 5.º) y SPOOF vetado cada décimo desde el
  37 (37…97).
  *Hecho 2026-10-03:* `GameEngineTest` (COUNT=100, semillas distintas),
  `CampaignTest` (5×20) y `ChallengeRestrictionTest` (10 niveles vetados).
- [x] **J02 — Tablero grande solo en desafíos ≥31:** tramos 11–12 (31–60) y
  13–14 (61–100) con un único draw de RNG (niveles 1–30, modo libre y
  escenarios byte-idénticos; `PuzzlePreview.REVISION` intacto), corredor
  de referencia más largo (10 y 11 dominós) y firewall con tope a media
  celda abierta en las bandas anchas.
  *Hecho 2026-10-03:* constantes `WIDE_GENERATED_BOARD_WIDTH`/`WIDER_…`,
  `maxDominoes()` y `DataNodesTest` (anchuras por tramo).
- [x] **J03 — Nodos de datos obligatorios (desde el 41):** `BoardState.waypoints`
  + `isObjectiveReached` (extracción **y** nodos dentro de
  `reachableFromStart`); el generador los reparte por la ruta de
  referencia con aritmética de índices (sin consumir RNG) y nunca sobre
  firewalls ni honeypots. UI: ◆ ámbar pulsante → ✓ verde al cubrir,
  contador `NODOS k/N` en el header, regla en el diálogo del reto y ayuda
  del tablero en ambos idiomas. Escalado 1/2/3 nodos en 41–60/61–80/81–100.
  *Hecho 2026-10-03:* `DataNodesTest` (4 pruebas) y `BalanceTest`
  revalida la solución de referencia de los 100 niveles con la nueva
  condición de victoria.
- [x] **J04 — Validación y cierre:** `./gradlew test` verde
  (56 dominio + 8 app), `:app:lintDebug` verde,
  `:app:connectedDebugAndroidTest` **28/28** en `emulator-5554`
  (`wm size 640x360`), sync de docs (GDD/README/PLAY/ESTADO) y commits por
  incremento (`feat:` ×3 + `docs:`).

**Cierre de J:** los 100 desafíos generan y resuelven con sus nodos
(`BalanceTest` replayea la referencia de cada nivel), niveles 1–30 y modo
libre sin cambios de semilla, y la UI de fases/nodos cubierta por tests de
dominio y la suite instrumentada completa.

### Seguimiento y entrega

- Empezar por A01–A08; A09 puede investigarse en paralelo.
- Conservar los cambios locales existentes y revisar cada diferencia antes de editar.
- Registrar en cada tarea cerrada la evidencia (prueba, informe o captura) y fecha.
- Si cambia una regla, actualizar GDD y sus pruebas en el mismo incremento.
- Actualizar el resumen de `ESTADO.md` al cerrar un bloque; mantener aquí el desglose.
- Ejecutar `./gradlew :game-domain:test` para dominio y
  `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` para app;
  usar `./gradlew :app:connectedDebugAndroidTest` con dispositivo autorizado.
- Preparar cambios enfocados con validación y capturas cuando corresponda.

### K — Tienda de skins (P1)

- [x] **K01 — Catálogo y precios:** cerrado 2026-10-03 (alcance: reparto 3+3) — `Rewards` con `freeBoardSkins`/`freeDominoSkins`, `boardSkinIds`/`dominoSkinIds`, `isPaid(categoría, id)` total, `skinPrice` invertido (premium → 200, resto de pago → 80) y `ownedKey`; `nebula` resources-only (ramas en `boardSkinResources`/`dominoShellResource`/`dominoPipColor` + assets y provenancia, sin catálogo ni strings). Evidencia: `SkinCatalogTest` 4/4, `SkinCatalogListsTest` 2/2, `DominoResourcesTest` 6/6, dominio 58/58, app 11/11.
- [x] **K02 — Posesión y compra:** namespaced `board:<id>`/`domino:<id>`, normalización idempotente en lectura (sin flag), `ensureEquippedOwned()` otorga solo lo equipado, `buySkin(board, id)` atómico y con guarda `unknown` en el repo, check de propiedad añadido a `setDominoSkin`.
- [x] **K03 — UI Tienda:** `store` (STORE/TIENDA), badge FREE, compra de fichas end-to-end, diálogo de compra con categoría (corrige el `boardSkins.first` que lanzaba `NoSuchElementException` con fichas), textos EN/ES generalizados; borrado de `purchasableSkins`/`affordableSkins`.
- [x] **K04 — Tests y docs:** `ProgressionUiTest` +3 (compra de ficha, otorgamiento del equipado, idempotencia) → 13/13; suite instrumentada 31/31 (1 flake de entorno en `ImmersiveWindowUiTest`, 2/2 en retry aislado); GDD/ESTADO/README raíz sincronizados (las filas de `assets/skins/README.md` quedan para un commit posterior); `REVISION` sin cambios. Evidencia: `./gradlew test` 58/58 dominio + 11/11 app, lint 0 errores (2026-10-03).

## 5. Criterios de éxito

- Ningún cambio de regla sin test y sin actualizar GDD.
- Sin stubs silenciosos: o se comportan como se documenta, o se marcan como pendientes.
- Acciones jugables claras en pantalla antes de añadir complejidad.
- Reproducibilidad de semillas mantenida.
- Localización actualizada en ambos idiomas cuando cambie texto jugable.

## 6. Cómo usar este roadmap

- Usar `docs/IMPROVEMENTS.md` como idea abierta.
- Usar este archivo para decidir orden y dependencias.
- Si una idea no encaja ahora, se deja en el banco y se reevalúa después.
- Cada nueva regla o pantalla debe poder entrar sin romper el flujo actual de `GameEngine.reduce` y `MainViewModel`.

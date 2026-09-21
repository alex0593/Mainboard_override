# Roadmap de desarrollo — Mainboard Override

## 1. Propósito

Este documento recoge, estructura y prioriza las ideas de mejora para el juego, a partir del banco de ideas en `docs/IMPROVEMENTS.md`. No es un plan definitivo, sino una propuesta de ruta para decidir qué hacer primero y qué dejar para después.

**Revisión:** 2026-09-21. Único plan operativo del proyecto: orden,
dependencias, tareas y criterios de cierre. El estado comprobable y las
decisiones viven en [ESTADO.md](ESTADO.md). Ayer (2026-09-20) se cerraron
A01–A09 con causas raíz registradas y B06 con reapertura limpia verificada.
Hoy (2026-09-21) se cerró A10 con la suite instrumental completa 26/26 en verde.
Los hilos temáticos son propuestas; no representan funciones aprobadas
ni tareas terminadas. `[ ]` significa pendiente; marcar `[x]` solo con evidencia
de cierre. Las decisiones de producto se registran antes de implementar sus ramas.

## 2. Estado actual como punto de partida

- Vertical slice con motor puro, generación determinista y validación de ruta.
- Scripts actuales: PING, SPOOF, KILL_PROCESS, BRIDGE.
- Progresión de desafíos desbloqueables y escenarios de modo libre.
- Persistencia con DataStore; compras y recompensas implementadas en el repositorio concreto.
- Última ejecución de `:game-domain:test`: 28 pruebas, 0 fallos (2026-09-20).
- Tutorial con 10 lecciones deterministas, textos EN/ES en paridad (47 instrucciones), progreso visible y avance explícito; `TutorialTest` 5/5 y `PuzzleTutorialUiTest` 6/6 en CLK-LX3 por USB.
- Audio procedural completo: soundtrack synthwave adaptativo, 16 cues de SFX, volúmenes de música/EFX en ajustes, silencio y movimiento reducido cableados. Vibración solo persistida, sin cablear.
- Localización por-app con AppCompat (ES/EN aplican en todos los API), icono adaptativo con capa monocroma y splash propio.
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
- Añadir scripts complementarios: ocultar rastreo temporalmente, dañar daemon, duplicar puerto, alterar puentes, etc.
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
- Conectar audio, vibración y VFX con ajustes ya persistidos. (Hecho audio: motor, cues, volúmenes, silencio y movimiento reducido; pendiente vibración y VFX extra.)
- Añadir indicios sutiles de glitch, estática, synthwave o teclado mecánico. (Hecho synthwave, teclado mecánico y riser de urgencia; pendiente glitch visual al 80 % y estática.)
- Feedback sonoro por evento: colocación, script, trampa, daemon, victoria/derrota. (Hecho: 16 cues procedurales; daemon sin cue propia.)
- Partículas leves para rastreo alto o recogida de buffs.

### 3.8 Calidad técnica y tests
- Aumentar tests de dominio antes de añadir reglas. (Hecho: 28 pruebas con barridos de 1000 semillas + 7×100 por escenario.)
- Añadir pruebas instrumentadas más amplias. (Parcial: 7 clases Compose; suite completa pendiente de A10.)
- Añadir validaciones masivas de semillas. (Hecho en dominio; pendiente en dispositivos reales.)
- Mejorar nombres de tests y claridad.
- Centralizar lógica de validación de UI.
- Prever invalidación de caché de previsualizaciones al cambiar geometría o renderizado. (Hecho: clave con `REVISION`.)

### 3.9 Contenido y skins
- Añadir skins temáticas coherentes con la estética. (Hecho: Cobre, Aurora, grafito, señal + icono adaptativo y splash propio.)
- Separar mejor skins de fichas y de placa. (Hecho: galería separa fichas y PCB.)
- Ampliar textos de ayuda, ejemplos y notas de diseño en GDD. (Hecho ayuda contextual y tutorial reescrito; GDD pendiente de sincronizar audio/tutorial/icono.)
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
- [x] **C04 — Previsualizaciones:** clave de caché con `REVISION = 1` e invalidación por skins (`$REVISION:id:boardSkin:dominoSkin`); `ScenarioArtworkUiTest` existe (validación completa pendiente de A10).
- [x] **C05 — Cerrar arte existente:** 32 recursos `menu_background|board_|menu_card`, fondo por escenario recordado con fallback `classic`, licencias (`kenney-domino-pack`, sprites y fondos generados) y originales en `assets/`; `ScenarioArtworkUiTest` existe (validación completa pendiente de A10).
- [x] **C06 — Recursos y ayuda:** 10 lecciones con 47 instrucciones en paridad EN/ES verificada; `tutorial_next_lesson`/`tutorial_lesson_complete` en uso; la auditoría de textos sin uso queda cubierta por lint en A10.

**Cierre:** compila y pasa lint; los flujos afectados tienen validación proporcional
al cambio; hay capturas de cambios visuales y evidencia de revisión con TalkBack
y fuentes grandes. No se regenera arte existente como requisito de esta fase.

### D — Generación y aprendizaje (P2)

Depende de A–C. Cada incremento conserva la reproducibilidad por semilla.

- [x] **D01 — Semillas:** conjunto reproducible en `GameEngineTest` (barrido de 1000 semillas + 7 escenarios × 100 semillas con rutas validadas); las semillas problemáticas del catálogo siguen abiertas como A02.
- [x] **D02 — Equilibrio:** cerrado 2026-09-21 — criterios en `BalanceTest`: referencia limpia (rastreo = 8×turnos, sin trampas ni ruido), 100 % de manos iniciales con jugada legal, headroom mínimo 28 en libre (fantasma cierra en 72), y las 30 referencias de desafío dentro de presupuesto (niveles 2, 10 y 20-30 al límite exacto). Medido sobre el conjunto D01 antes de cualquier ajuste de generación.
- [x] **D03 — Tutorial avanzado:** lecciones de KILL (7) y BRIDGE (8) con fixtures deterministas, ruta de entrada incorrecta, textos EN/ES en paridad y pruebas de finalización (`TutorialTest` 5/5, `PuzzleTutorialUiTest` 6/6).
- [x] **D04 — Práctica:** alcance acordado = práctica guiada rejugable (continuar/reiniciar/siguiente explícitos) con progreso aislado (`TutorialController` solo persiste `tutorialLesson`; sin créditos ni récords). Sandbox libre no incluido.

**Cierre:** semillas repetibles y rutas verificadas, informe de equilibrio con
criterios explícitos y nuevas lecciones completables en ambos idiomas.

### E — Ampliaciones propuestas (P2, alcance por decidir)

Depende de D. Seleccionar un incremento antes de desarrollarlo; estas opciones
no son compromisos de implementar todas las ideas del banco.

- [ ] **E01 — Scripts y amenazas:** elegir un efecto; definir coste, ruido, objetivos y orden de resolución; implementar en el motor, probar límites y actualizar GDD/ayuda.
- [ ] **E02 — Retos y geometrías:** elegir una restricción o geometría; adaptar generador y renderizado, revisar caché y validar semillas.
- [ ] **E03 — Puntuación:** acordar objetivos y desempates; implementar cálculo, persistencia de récord y presentación.
- [ ] **E04 — Audio, vibración y VFX:** audio cerrado y verificado (soundtrack synthwave adaptativo, 16 cues procedurales, volúmenes música/EFX en ajustes, silencio y movimiento reducido cableados); pendiente vibración (preferencia persistida sin cablear) y partículas para rastreo alto/recogidas.
- [ ] **E05 — Campaña y recompensas:** definir fases, logros y condiciones; implementar progreso con pagos idempotentes y compatibilidad de perfiles.
- [ ] **E06 — Metas y mejoras:** decidir alcance de metas diarias/semanales, inventario y mejoras de scripts; definir calendario y equilibrio antes de implementar.

**Cierre por incremento:** diseño acordado, implementación, validación relevante,
textos EN/ES y documentación actualizada. Las opciones sin seleccionar siguen
pendientes de definición.

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

# Roadmap de desarrollo — Mainboard Override

## 1. Propósito

Este documento recoge, estructura y prioriza las ideas de mejora para el juego, a partir del banco de ideas en `docs/IMPROVEMENTS.md`. No es un plan definitivo, sino una propuesta de ruta para decidir qué hacer primero y qué dejar para después.

**Revisión:** 2026-09-15. El desglose operativo de la sección 4 define el orden
de trabajo. Los hilos temáticos son propuestas; no representan funciones aprobadas
ni tareas terminadas. `[ ]` significa pendiente; marcar `[x]` solo con evidencia
de cierre. Las decisiones de producto se registran antes de implementar sus ramas.

## 2. Estado actual como punto de partida

- Vertical slice con motor puro, generación determinista y validación de ruta.
- Scripts actuales: PING, SPOOF, KILL_PROCESS, BRIDGE.
- Progresión de desafíos desbloqueables y escenarios de modo libre.
- Persistencia con DataStore; compras y recompensas implementadas en el repositorio concreto.
- Última ejecución de `:game-domain:test`: 27 pruebas, 8 fallos (2026-09-15).
- Tutorial con lecciones deterministas y propio StateFlow.
- Textos en español e inglés.
- UI funcional pero concentrada en pocos archivos.

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
- Mejorar feedback de KILL para dejar claro qué celda o ficha se va a eliminar.
- Mejorar PING con más contexto útil y no solo previsualización de ficha.

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
- Añadir vista de cambios pendientes antes de ejecutar turno.
- Actualizar previsualización de puzzles cuando cambian las skins equipadas.
- Unificar coherencia visual entre menú y partida.

### 3.5 Progresión y economía
- Validar la economía implementada: compras, saldo, recompensas e idempotencia.
- Añadir logros/insignias/recompensas por estilo de juego.
- Ampliar campaña con fases temáticas más claras.
- Añadir sistema de objetos/light upgrades para jugar a largo plazo.
- Permitir metas diarias/semanales en modo libre.

### 3.6 Tutorial y onboarding
- Aumentar cobertura del tutorial: scripts avanzados, errores comunes, lectura de tablero.
- Añadir lecciones extra o puntos de referencia desbloqueables.
- Permitir práctica sandbox dentro del tutorial.
- Mejorar flujo entre lecciones y saltar al tema elegido.

### 3.7 Sonido, VFX y atmósfera
- Conectar audio, vibración y VFX con ajustes ya persistidos.
- Añadir indicios sutiles de glitch, estática, synthwave o teclado mecánico.
- Feedback sonoro por evento: colocación, script, trampa, daemon, victoria/derrota.
- Partículas leves para rastreo alto o recogida de buffs.

### 3.8 Calidad técnica y tests
- Aumentar tests de dominio antes de añadir reglas.
- Añadir pruebas instrumentadas más amplias.
- Añadir validaciones masivas de semillas.
- Mejorar nombres de tests y claridad.
- Centralizar lógica de validación de UI.
- Prever invalidación de caché de previsualizaciones al cambiar geometría o renderizado.

### 3.9 Contenido y skins
- Añadir skins temáticas coherentes con la estética.
- Separar mejor skins de fichas y de placa.
- Ampliar textos de ayuda, ejemplos y notas de diseño en GDD.
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

- [ ] **A01 — Contactos:** resolver `external mismatches reject placement but domino halves remain internally connected`.
- [ ] **A02 — Generación:** resolver `challenge catalog contains thirty constrained solvable levels`; conservar nivel y semilla que provocan el fallo.
- [ ] **A03 — Derrota:** resolver `loss takes priority over extraction when trace reaches maximum`.
- [ ] **A04 — Turnos:** resolver `challenge turn limit fails only when extraction is not reached`.
- [ ] **A05 — Rastreo:** resolver `challenge rules allow the exact limit and fail when trace is exceeded`.
- [ ] **A06 — PING:** resolver `three pings preview three tiles then reset at end turn`.
- [ ] **A07 — Scripts y RAM:** resolver `ping then ram pickup allows kill in the same turn`.
- [x] **A08 — Tutorial de dominio:** resuelto `scriptsHaveRealEffects`. Causa: el fixture de la lección 9 (tablero de 8 con ruta hasta x=5) nunca alcanzaba la extracción; se fijó el tablero a ancho 6 con extracción en (6, 2) y se añadieron dos pasos de cierre (derrotas y recompensas).
- [x] **A09 — Tutorial instrumental:** cerrado. Causa doble: (1) el panel de instrucción se autocerraba y el botón de repetir no existía a mitad de lección — repetir persistente en el header y avance explícito entre lecciones; (2) race en `AmbientSoundtrack` (write sobre un track liberado por `stop()`) que mataba el proceso de tests — `try/catch` alrededor del write. Clase `PuzzleTutorialUiTest` 6/6 en CLK-LX3 por USB.
- [ ] **A10 — Cierre de validación:** ejecutar dominio, tests locales de app, compilación debug, lint y suite instrumental; registrar comandos, fecha, dispositivo y resultados.

**Cierre:** A01–A08 pasan y la suite completa de dominio queda verde; A09 pasa
aislada y en suite; A10 no presenta fallos sin resolver. Si falta dispositivo,
registrar el bloqueo de instrumentación y mantener esa validación pendiente.

### B — Progreso, economía y persistencia (P0/P1)

Ubicación: `ProgressionScreens.kt`, `MainViewModel.kt`,
`PlayerPreferencesRepository.kt` y tests de app. Depende de A.

- [x] **B01 — Estados de escenario (P0):** se decidió persistir la victoria por escenario en `completedScenarios`; el desbloqueo sigue dependiendo de desafíos ganados y no implica haber jugado el escenario.
- [x] **B02 — Aplicar los estados (P0):** `ScenarioScreen` separa el conteo de desafíos para desbloqueo de `completedScenarios` para la marca de completado; perfiles existentes reciben el conjunto vacío sin migración retroactiva.
- [ ] **B03 — Flujos de partida (P0):** verificar iniciar desafío, iniciar escenario, reintentar y reiniciar; cubrir conservación de modo, nivel y semilla.
- [x] **B04 — Economía (P1):** las compras siguen dependiendo del resultado persistido de DataStore; se eliminó el estado local optimista de la galería y la prueba de progresión cubre la victoria libre idempotente junto con la persistencia de escenario.
- [ ] **B05 — Reanudación (P1, decisión):** documentar si se soportará restaurar una partida tras muerte del proceso y qué ocurre al volver a abrir la app.
- [ ] **B06 — Aplicar B05 (P1):** si se elige restauración, definir formato/versionado y probar recuperación sin duplicar recompensas; si se elige reinicio, documentar y verificar ese comportamiento.

**Cierre:** la UI distingue los estados acordados; los flujos conservan modo y
semilla; compras y pagos tienen pruebas de sus casos límite; B01 y B05 tienen
decisión registrada y sus ramas correspondientes verificadas.

### C — UI, arte y accesibilidad (P1)

Ubicación: `app/src/main/kotlin/.../ui/`, recursos EN/ES, arte y tests
instrumentados. Depende de A; las pantallas de progreso dependen también de B.

- [ ] **C01 — Componentes:** extraer encabezado, controles e indicadores de `MainboardApp.kt`; dividir pantallas de `ProgressionScreens.kt` preservando acciones y estado.
- [ ] **C02 — Accesibilidad:** revisar orden de TalkBack, descripciones de celdas, objetivos táctiles, fuentes grandes, pantallas pequeñas y contraste; corregir los problemas encontrados.
- [ ] **C03 — Feedback:** aclarar selección, colocación rechazada y objetivo completo de KILL; mostrar cambios de RAM, ruido y rastreo con textos EN/ES.
- [ ] **C04 — Previsualizaciones:** verificar cambio de skins e invalidación de caché; documentar cuándo incrementar la revisión por geometría/renderizado.
- [ ] **C05 — Cerrar arte existente:** revisar los siete fondos locales en tarjetas, partida y menú; comprobar escenario recordado, fallback `classic`, licencias y originales; actualizar evidencia de `ScenarioArtworkUiTest` si cambia el resultado.
- [ ] **C06 — Recursos y ayuda:** comprobar cobertura real de lecciones y equivalencia EN/ES; revisar referencias antes de retirar textos aparentemente sin uso.

**Cierre:** compila y pasa lint; los flujos afectados tienen validación proporcional
al cambio; hay capturas de cambios visuales y evidencia de revisión con TalkBack
y fuentes grandes. No se regenera arte existente como requisito de esta fase.

### D — Generación y aprendizaje (P2)

Depende de A–C. Cada incremento conserva la reproducibilidad por semilla.

- [ ] **D01 — Semillas:** preparar un conjunto reproducible por escenario y desafío; validar rutas y registrar semillas problemáticas como regresiones.
- [ ] **D02 — Equilibrio:** definir métricas y umbrales de dificultad, turnos, rastreo y dependencia de scripts; medir el conjunto de D01 antes de ajustar generación.
- [ ] **D03 — Tutorial avanzado:** identificar huecos de cobertura; diseñar lecciones para KILL, BRIDGE y errores frecuentes, con fixtures deterministas, textos EN/ES y pruebas de finalización.
- [ ] **D04 — Práctica:** definir alcance del modo sandbox y navegación entre lecciones; implementar lo acordado con progreso aislado de recompensas reales.

**Cierre:** semillas repetibles y rutas verificadas, informe de equilibrio con
criterios explícitos y nuevas lecciones completables en ambos idiomas.

### E — Ampliaciones propuestas (P2, alcance por decidir)

Depende de D. Seleccionar un incremento antes de desarrollarlo; estas opciones
no son compromisos de implementar todas las ideas del banco.

- [ ] **E01 — Scripts y amenazas:** elegir un efecto; definir coste, ruido, objetivos y orden de resolución; implementar en el motor, probar límites y actualizar GDD/ayuda.
- [ ] **E02 — Retos y geometrías:** elegir una restricción o geometría; adaptar generador y renderizado, revisar caché y validar semillas.
- [ ] **E03 — Puntuación:** acordar objetivos y desempates; implementar cálculo, persistencia de récord y presentación.
- [ ] **E04 — Audio, vibración y VFX:** elegir eventos y assets con licencia; conectar preferencias, silencio y movimiento reducido; verificar activación/desactivación.
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
- Actualizar el resumen de `CATALOGO.md` al cerrar un bloque; mantener aquí el desglose.
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

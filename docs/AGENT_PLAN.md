# Plan de trabajo para otro agente — Mainboard Override

**Revisión:** 2026-09-15. El orden, las dependencias, las tareas y sus criterios
de cierre están en [ROADMAP.md, sección 4](ROADMAP.md#4-plan-operativo-y-tareas).
Este documento aporta contexto de entrada; empezar por A01–A08 y A09.

## 1. Estado actual del proyecto

- Vertical slice de un juego táctico de tablero estilo dominó + scripts.
- Módulos: `game-domain` (Kotlin puro) y `app` (Android/Compose).
- Motor puro con reduccionismo inmutable, semillas reproducibles y validación de ruta.
- Persistencia con DataStore para preferencias, progreso de desafíos, créditos, skins y tutorial.
- Idiomas: español e inglés.
- Arte: sprites de Kenney + assets procedurales/temáticos.

## 2. Hay que leer primero

### Contratos centrales
- `game-domain/src/main/kotlin/.../domain/Models.kt`
- `game-domain/src/main/kotlin/.../domain/GameEngine.kt`
- `game-domain/src/main/kotlin/.../domain/LevelGenerator.kt`
- `game-domain/src/main/kotlin/.../domain/Challenge.kt`
- `game-domain/src/main/kotlin/.../domain/Progression.kt`
- `app/src/main/kotlin/.../MainViewModel.kt`
- `app/src/main/kotlin/.../ui/MainboardApp.kt`
- `app/src/main/kotlin/.../ui/PuzzlePreview.kt`
- `app/src/main/kotlin/.../data/PlayerPreferencesRepository.kt`

### Documentación de producto y arquitectura
- `docs/GDD.md`
- `docs/ARCHITECTURE.md`
- `CARTS spesificationst.md`
- `AGENTS.md`

## 3. Hallazgos de la auditoría

### 3.1 Fortalezas
- Separación limpia entre dominio y UI.
- Estado inmutable y eventos explícitos.
- Generación determinista con ruta validada.
- Textos localizados en dos idiomas.
- Persistencia aislada del estado de partida activa.
- Tutorial con fixtures deterministas y propio StateFlow.
- Cinemática de ventana y ayuda contextual presentes.

### 3.2 Riesgos y deuda técnica
- UI muy concentrada en `MainboardApp.kt` y `ProgressionScreens.kt`.
- `ScenarioScreen` separa el desbloqueo por desafíos de la marca de completado persistida en `completedScenarios`.
- `buySkin` y `finishMatch` están implementados en el repositorio DataStore; revisar cobertura y confirmación de compra en UI en B04.
- Algunos strings de progresión y recompensas parecen sobrar o no usarse.
- `tutorial.xml` tiene muchos mensajes que no se resuelven en pantalla y arrays grandes; hay que confirmar cobertura real.
- Revisar recursos por sus referencias y cobertura EN/ES en C06; la ausencia de archivos llamados `dimens.xml`, `themes.xml` o `arrays.xml` no demuestra un defecto en Compose.
- La caché tiene invalidación por clave y revisión de renderizado; verificar cambios de skins y mantenimiento de revisión en C04.
- Partidas no reanudan tras muerte del proceso.

### 3.3 Posibles errores o inconsistencias
- `ChallengeScreen` inicia desafíos mediante `startChallenge`; conservar una prueba de ese destino al revisar flujos.
- `ScenarioScreen` usa `challengeBest.size` solo para desbloqueo y `completedScenarios` para la marca de completado.
- Verificar presentación de semillas en ambos idiomas si se detecta un fallo; `%d` admite `Long` y no constituye por sí mismo una incompatibilidad.
- Revisar que `retryLast` y `restartNetwork` no confundan escenario libre con desafío.

## 4. Prioridades

1. **A — P0:** resolver los ocho fallos de dominio y el fallo instrumental registrado del tutorial; cerrar validación.
2. **B — P0/P1:** verificar flujos y decidir el comportamiento de reanudación; B01–B02 y B04 ya están aplicadas.
3. **C — P1:** modularidad, accesibilidad, feedback, caché, arte existente y recursos EN/ES.
4. **D — P2:** validar semillas y equilibrio; ampliar tutorial y definir práctica.
5. **E — P2:** seleccionar y desarrollar incrementos de contenido después de validar la base.

## 5. Qué hacer primero

- Usar como base la ejecución del 2026-09-15: 27 pruebas de dominio, 8 fallos.
- Reproducir los casos de A01–A08 y contrastarlos con GDD; corregir causa y verificar la suite completa.
- Ejecutar tests locales de app, compilación debug, lint e instrumentación para cerrar A10.
- revisar `ScenarioScreen` y `ChallengeScreen` con estados reales
- contrastar `GDD.md` con comportamiento actual

## 6. Entregables sugeridos

- Corrección de progreso/desbloqueo en UI.
- Evidencia de validación de economía y decisión documentada sobre reanudación.
- Limpiar strings sobrantes o añadirlos a localización.
- Mejorar modularidad de pantallas antes de ampliar contenido.

## 7. Validación

- Pruebas de dominio antes que reglas.
- Build de debug estable.
- Comparar texto EN/ES en pantalla.
- Confirmar reproducibilidad de semillas.

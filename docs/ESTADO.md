# Estado del proyecto — Mainboard Override

Índice operativo: resume el estado comprobable, enlaza las fuentes de detalle
y conserva las decisiones que afectan al trabajo siguiente. El plan de trabajo
vive únicamente en [ROADMAP.md](ROADMAP.md).

**Revisión:** 2026-09-21.

## Fuentes de verdad

- [GDD](GDD.md) — diseño de juego, reglas y experiencia prevista.
- [Arquitectura](ARCHITECTURE.md) — flujo de estado, contratos y mantenimiento.
- [Roadmap](ROADMAP.md) — único plan operativo: prioridades, dependencias y criterios de cierre.
- [Banco de ideas](IMPROVEMENTS.md) — opciones de producto sin compromiso de entrega.
- [Guía visual de cartas](CARTAS.md) — especificación de cartas de script e iconos de recogidas.
- [Fondos de escenarios](../assets/scenarios/README.md) — prompts, originales y exportaciones.
- [Validación visual de escenarios](validation/scenarios/README.md) — capturas y resultados instrumentados.
- [Validación de pantalla completa](validation/immersive/README.md) — evidencia de modo inmersivo.

Los contratos técnicos viven en el código; los cambios de reglas deben
reflejarse en el GDD y en los tests del dominio. Este archivo no duplica sus detalles.

## Dónde quedamos ayer (2026-09-20)

- `fix: close A01-A07 root causes, domain suite 28/28 green` — los siete fallos
  de dominio eran fixtures desconectados y un conteo fijo de dominós, no
  semántica del motor; se corrigieron las causas y la suite quedó verde.
- `feat: verify B06 clean-menu reopen without duplicate rewards` — reabrir tras
  cerrar el proceso parte del menú limpio, sin duplicar créditos.
- Estado verificado (2026-09-21): `:game-domain:test` 28/28, `:app:testDebugUnitTest` 3/3,
  `:app:lintDebug` verde, suite instrumental completa 26/26 en CLK-LX3 por USB,
  `:app:installDebug` verificado en dispositivo. Bloque A del roadmap cerrado.
- Cambio local sin commitear (pendiente de revisar): ajuste de `lineHeight` y
  alturas mínimas en `GameHeader.kt` y `MainboardApp.kt` para fuentes ampliadas.

## Estado comprobado

| Área | Estado actual | Fuente principal |
| --- | --- | --- |
| Motor | Estado inmutable, acciones explícitas y `GameEngine.reduce`. | [Arquitectura](ARCHITECTURE.md) |
| Generación | Semillas reproducibles; niveles libres, escenarios y desafíos catalogados; suite de dominio 28/28 verde. | [Arquitectura](ARCHITECTURE.md) |
| Scripts | `PING`, `SPOOF`, `KILL` (`KILL_PROCESS` internamente) y `BRIDGE`. | [GDD](GDD.md) |
| Progresión | Desafíos desbloquean escenarios por victorias distintas; récords y créditos persisten. | `PlayerPreferencesRepository` |
| Economía | Compras y recompensas con transacciones DataStore e idempotencia; reapertura limpia sin pagos duplicados. | `PlayerPreferencesRepository` |
| Escenarios | Siete fondos generados, empaquetados y usados en tarjetas, partidas y menú. | [README de escenarios](../assets/scenarios/README.md) |
| Tutorial | 10 lecciones deterministas, flujo separado y progreso persistido; `TutorialTest` 5/5 y `PuzzleTutorialUiTest` 6/6. | [Arquitectura](ARCHITECTURE.md) |
| Audio | Soundtrack synthwave adaptativo, 16 cues procedurales, volúmenes música/EFX, silencio y movimiento reducido cableados. Vibración solo persistida, sin cablear. | [Roadmap](ROADMAP.md) |
| Localización | Recursos españoles e ingleses para la UI y el tutorial (47 instrucciones en paridad). | `app/src/main/res/values*` |
| Accesibilidad | Descripciones semánticas y objetivos táctiles mínimos en controles principales; sin revisión TalkBack registrada. | [Arquitectura](ARCHITECTURE.md) |

## Contratos de entrada (para retomar el trabajo)

- `game-domain/.../domain/Models.kt`, `GameEngine.kt`, `LevelGenerator.kt`,
  `Challenge.kt`, `Progression.kt`
- `app/.../MainViewModel.kt`, `app/.../ui/MainboardApp.kt`,
  `app/.../ui/PuzzlePreview.kt`, `app/.../data/PlayerPreferencesRepository.kt`
- Reglas de contribución en `AGENTS.md` (raíz del repositorio).

## Decisiones cerradas

- **B01/B02:** la victoria por escenario persiste en `completedScenarios`; el
  desbloqueo depende de desafíos ganados y no implica haber jugado el escenario.
  Perfiles existentes reciben el conjunto vacío, sin créditos retroactivos.
- **B05/B06:** no se reanuda la partida tras cerrar el proceso; al reabrir se
  parte del menú limpio, sin recompensas duplicadas.
- **D04:** práctica del tutorial = lecciones guiadas rejugables
  (continuar/reiniciar/siguiente); sin sandbox libre.
- Los fondos `classic`, `lab`, `data`, `industry`, `archive`, `core` y `ghost`
  son independientes de las skins de tablero y fichas; el menú usa el fondo del
  último escenario libre (`preferences.lastScenario`, `classic` por defecto).
- Las previsualizaciones de selección usan la semilla fija `42` y se etiquetan
  como ejemplo; la caché se invalida con `REVISION` y skins en la clave.

## Deuda activa

- **C01:** cerrado 2026-09-21 — `MainboardApp.kt` (~110 líneas, solo NavHost);
  `GameScreen`, `Board`, `MenuScreen`, `SettingsScreen` y widgets en archivos propios.
- **C02:** cerrado 2026-09-21 — roles expuestos, target mínimo 48dp, contraste AA
  verificado, test a fuente 1.3x; suite 27/27. TalkBack manual pendiente.
- **D02:** sin métricas ni umbrales de equilibrio definidos.
- **E01–E06:** ampliaciones pendientes de selección de alcance.
- **Vibración:** preferencia persistida sin cablear; partículas para rastreo
  alto/recogidas pendientes.

## Cómo verificar el estado

Desde la raíz del repositorio, con JDK 17 y el SDK configurado:

```sh
./gradlew :game-domain:test
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

## Criterios de mantenimiento

- Ningún cambio de regla entra sin test de dominio y actualización del GDD.
- Todo stub debe comportarse como se documenta o aparecer como deuda explícita.
- Toda cadena visible nueva se añade en `values/` y `values-es/`.
- Toda semilla usada para validar un nivel debe conservar reproducibilidad.
- Una nueva prioridad se añade al roadmap y aquí solo se resume con su estado.

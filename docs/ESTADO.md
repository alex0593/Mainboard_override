# Estado del proyecto — Mainboard Override

Índice operativo: resume el estado comprobable, enlaza las fuentes de detalle
y conserva las decisiones que afectan al trabajo siguiente. El plan de trabajo
vive únicamente en [ROADMAP.md](ROADMAP.md).

**Revisión:** 2026-09-22.

## Fuentes de verdad

- [GDD](GDD.md) — diseño de juego, reglas y experiencia prevista.
- [Arquitectura](ARCHITECTURE.md) — flujo de estado, contratos y mantenimiento.
- [Roadmap](ROADMAP.md) — único plan operativo: prioridades, dependencias y criterios de cierre.
- [Banco de ideas](IMPROVEMENTS.md) — opciones de producto sin compromiso de entrega.
- [Futuro](FUTURO.md) — alcances diferidos durante los incrementos E.
- [Guía visual de cartas](CARTAS.md) — especificación de cartas de script e iconos de recogidas.
- [Fondos de escenarios](../assets/scenarios/README.md) — prompts, originales y exportaciones.
- [Logo e icono](../assets/logo/procedencia.md) — original del logo, exportación del icono del launcher y su procedencia.
- [Validación visual de escenarios](validation/scenarios/README.md) — capturas y resultados instrumentados.
- [Validación de pantalla completa](validation/immersive/README.md) — evidencia de modo inmersivo.

Los contratos técnicos viven en el código; los cambios de reglas deben
reflejarse en el GDD y en los tests del dominio. Este archivo no duplica sus detalles.

## Dónde quedamos (última revisión 2026-09-21)

- `fix: close A01-A07 root causes, domain suite 28/28 green` — los siete fallos
  de dominio eran fixtures desconectados y un conteo fijo de dominós, no
  semántica del motor; se corrigieron las causas y la suite quedó verde.
- `feat: verify B06 clean-menu reopen without duplicate rewards` — reabrir tras
  cerrar el proceso parte del menú limpio, sin duplicar créditos.
- Bloques A–E con los incrementos seleccionados cerrados (A01–A10, B01–B06,
  C01–C06, D01–D04, E01–E06) y F01–F03 cerrados (F01/F02 el 2026-09-21, F03
  el 2026-09-22); el detalle con evidencia está en el [Roadmap](ROADMAP.md).
- Estado verificado (2026-09-22): `:game-domain:test` **52/52**,
  `:app:testDebugUnitTest` **8/8**, `:app:lintDebug` sin errores, suite
  instrumental completa **28/28** en CLK‑LX3 por USB, `:app:assembleDebug`
  correcto.
- **F01 — Skins Titanio, Jade y Rubí (2026-09-21):** 6 originales versionados,
  12 exportaciones empaquetadas, selector y galería ampliados, textos EN/ES,
  `SkinCatalogTest` 2/2 y `ProgressionUiTest` 10/10 (compra y equipo de jade).
- **F02 — Script STEALTH (2026-09-21):** `STEALTH(1, 0)` descarta el ruido
  pendiente del turno; rechazo nuevo `NO_PENDING_NOISE`; mazo ampliado a 15
  cartas vía `ScriptType.entries`; textos EN/ES de rechazo y ayuda; cue
  sintetizado y vibración; ilustración de la carta versionada en
  `assets/cards/card_stealth.png` y exportada a `script_card_stealth.png`;
  `ScriptRulesTest` +5 → dominio 51/51.
- **F03 — Skins Zafiro, Ámbar y Amatista (2026-09-22):** 6 originales
  versionados con prompts, 12 exportaciones empaquetadas, mapeo y colores de
  punto por material, galería y textos EN/ES; `SkinCatalogTest` 3/3 y
  `DominoResourcesTest` 4/4.
- Organización y icono (2026-09-21): arte de la raíz movido a `assets/reference`
  y `assets/kenney`, `assets/logo/` versionado, y el foreground del icono
  adaptativo pasa a ser la exportación del logo (`logo_foreground.png`).
- No hay cambios locales sin commitear.

## Estado comprobado

| Área | Estado actual | Fuente principal |
| --- | --- | --- |
| Motor | Estado inmutable, acciones explícitas y `GameEngine.reduce`. | [Arquitectura](ARCHITECTURE.md) |
| Generación | Semillas reproducibles; niveles libres, escenarios y desafíos catalogados; suite de dominio 52/52 verde. | [Arquitectura](ARCHITECTURE.md) |
| Skins | 18 fichas y 17 PCB en galería; Cobre, Aurora, Titanio, Jade, Rubí, Zafiro, Ámbar y Amatista premium (200), grafito y señal a 80, resto gratis. | [README de skins](../assets/skins/README.md) |
| Scripts | `PING`, `SPOOF`, `KILL` (`KILL_PROCESS` internamente), `BRIDGE` y `STEALTH` (F02, descarta el ruido pendiente). | [GDD](GDD.md) |
| Progresión | Desafíos desbloquean escenarios por victorias distintas; récords y créditos persisten. | `PlayerPreferencesRepository` |
| Economía | Compras y recompensas con transacciones DataStore e idempotencia; reapertura limpia sin pagos duplicados. | `PlayerPreferencesRepository` |
| Escenarios | Siete fondos generados, empaquetados y usados en tarjetas, partidas y menú. | [README de escenarios](../assets/scenarios/README.md) |
| Tutorial | 10 lecciones deterministas, flujo separado y progreso persistido; `TutorialTest` 5/5 y `PuzzleTutorialUiTest` 6/6. | [Arquitectura](ARCHITECTURE.md) |
| Audio | Soundtrack synthwave adaptativo, 16 cues procedurales, volúmenes música/EFX, silencio, vibración cableada y movimiento reducido (E04). | [Roadmap](ROADMAP.md) |
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
- **F01:** las PCB Titanio, Jade y Rubí son premium de compra única a 200
  créditos (`Rewards.premiumSkins`); sus fichas correspondientes son gratuitas,
  porque la galería solo aplica precio a las PCB. `REVISION` no cambia: la clave
  de previsualización ya incluye el id de skin y el render es el mismo.
- **F02:** STEALTH cuesta 1 RAM y añade 0 de ruido; descarta `pendingNoise`
  completo y se rechaza (`NO_PENDING_NOISE`) sin gastar nada si no hay ruido
  pendiente. Cuesta 1 RAM para que quepa el combo SPOOF/BRIDGE + STEALTH dentro
  del tope de 3, mientras KILL (3 RAM) conserva su ruido sin combinación. Sin
  lección de tutorial y sin efecto sobre kernel panic.
- **F03:** las PCB Zafiro, Ámbar y Amatista siguen el molde de F01: premium
  de compra única a 200 créditos (`Rewards.premiumSkins`, ids `sapphire`,
  `amber`, `amethyst`); sus tres fichas correspondientes son gratuitas.
  `REVISION` no cambia: la clave de previsualización ya incluye el id de skin
  y el render es el mismo.
- **Icono del launcher:** el foreground del icono adaptativo es la exportación
  reducida `drawable-nodpi/logo_foreground.png` del original en `assets/logo/`;
  el fondo `#07110F` y la capa `monochrome` se conservan, y cualquier regeneración
  parte del original documentado en `assets/logo/procedencia.md`.

## Deuda activa

- **TalkBack:** revisión manual con lector en mano pendiente; la evidencia
  actual es automatizada (roles, targets ≥48dp, fuente 1.3x, contraste AA).
- **Pantallas pequeñas e horizontal invertido** sin verificación física;
  validación masiva de semillas pendiente en dispositivos reales (GDD).
- **Atmósfera (E04):** glitch visual sostenido al 80 % de rastreo y estática
  ambiental diferidos a `FUTURO.md`.

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

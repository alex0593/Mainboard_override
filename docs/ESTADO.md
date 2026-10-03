# Estado del proyecto — Mainboard Override

Índice operativo: resume el estado comprobable, enlaza las fuentes de detalle
y conserva las decisiones que afectan al trabajo siguiente. El plan de trabajo
vive únicamente en [ROADMAP.md](ROADMAP.md).

**Revisión:** 2026-10-02.

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

## Dónde quedamos (última revisión 2026-10-02)

- `fix: close A01-A07 root causes, domain suite 28/28 green` — los siete fallos
  de dominio eran fixtures desconectados y un conteo fijo de dominós, no
  semántica del motor; se corrigieron las causas y la suite quedó verde.
- `feat: verify B06 clean-menu reopen without duplicate rewards` — reabrir tras
  cerrar el proceso parte del menú limpio, sin duplicar créditos.
- Bloques A–E con los incrementos seleccionados cerrados (A01–A10, B01–B06,
  C01–C06, D01–D04, E01–E06) y F01–F03 cerrados (F01/F02 el 2026-09-21, F03
  el 2026-09-22); el detalle con evidencia está en el [Roadmap](ROADMAP.md).
- Estado verificado (2026-10-02): `:game-domain:test` **52/52**,
  `:app:testDebugUnitTest` **8/8**, `:app:lintDebug` y `:app:lintRelease` sin
  errores, `:app:assembleDebug`, `assembleRelease` y `bundleRelease`
  correctos; suite instrumental completa **28/28** en CLK‑LX3 por USB
  (última ejecución 2026-09-22).
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
- **Release 0.1.0 (2026-10-02):** `versionName 0.1.0` / `versionCode 12` con
  `signingConfigs.release` y R8/minify en `buildTypes.release`; keystore de
  subida fuera del repositorio (`keystore.properties` gitignored). APK
  (54.457.058 B, SHA-256 `504e82b4…796d`) y AAB (56.698.614 B, SHA-256
  `6d0e9379…1387`) construidos, `lintRelease` sin errores y ficha de tienda
  en [PLAY.md](PLAY.md). Ver detalle en el [Roadmap](ROADMAP.md) (bloque G).
- **Servidor y sitio (2026-10-02):** instancia nueva `t4g.micro` + gp3 10 GiB
  en us-east-2 (`i-0bf2980671102b46f`, IP `52.14.253.158`, ~7,3 $/mes) en
  lugar de `c7i-flex.large` (~66 $/mes); la vieja terminada. nginx con tres
  vhosts (catch-all 444), landing de descarga publicada, APK con checksum
  verificado en el servidor, página de Nexus Chat en `nexxxusapp` y
  `tools/deploy-site.sh` para despliegues repetibles vía S3 + SSM.
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
| Release | `0.1.0` / `versionCode 12`, firma release + R8; APK y AAB construidos, `lintRelease` sin errores; ficha de tienda lista. | [PLAY.md](PLAY.md) |
| Sitio y descarga | Landing en `mainboard-override.duckdns.org` (APK + `latest.apk` + `SHA256SUMS`), Nexus Chat en `nexxxusapp.duckdns.org`, API reservada; HTTP hoy, HTTPS pendiente de DNS/token. | [Roadmap](ROADMAP.md) (bloque H) |
| Servidor | `t4g.micro` + 10 GiB en us-east-2 (~7,3 $/mes), acceso solo por SSM (SG: 80/443 + 22 restringido), bucket `mainboard-override-artifacts-689217346963`. | [Roadmap](ROADMAP.md) (H02) |

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
- **G01/G02 (release):** versión `0.1.0` sin sufijo alfa (`versionCode 12`,
  tag `v0.1.0`); el keystore de subida vive en `~/.android/`
  (`mainboard-override-upload.jks`, alias `mainboard-override`) y solo su ruta
  y claves van en el gitignored `keystore.properties`; sin ese fichero el
  release falla en la tarea de firma en vez de emitir un artefacto firmado por
  error. Play recibe el AAB; la APK firmada es para descarga directa e itch.io.
- **H01/H02 (hosting):** tres nombres DuckDNS sobre la misma IP — landing en
  `mainboard-override.duckdns.org`, Nexus Chat en `nexxxusapp.duckdns.org` y
  API reservada en `nexxus-api.duckdns.org` (proxy comentado hasta que exista
  el servicio). Servidor `t4g.micro` + gp3 10 GiB en us-east-2 con Amazon
  Linux 2023; la IP pública cambió a `52.14.253.158`, así que los registros A
  se re apuntan con token DuckDNS (H04). Acceso de gestión **solo por SSM**
  (el puerto 22 queda restringido a la IP del desarrollador).
- **H06 (landing):** página estática en español servida por nginx, con
  versión/tamaño/SHA-256 del release y enlace a itch.io; `latest.apk` es un
  symlink que mueve el deploy (no hay que tocar nginx por versión). La landing
  de Nexus Chat usa su guía de estilo real (negro/blanco/neón `#FFFF05`) y el
  icono de `Nexus-chat_android`; el moodboard generado no se usa por tener
  texto defectuoso.

## Deuda activa

- **HTTPS pendiente (H03/H04):** certbot no puede emitir hasta que los tres
  nombres DuckDNS apunten a `52.14.253.158`; falta el **token de DuckDNS**
  (y el correo para el registro en Let's Encrypt). Mientras, los vhosts
  responden por HTTP.
- **Play Console (G05):** pasos externos — verificación de cuenta (25 USD),
  subida del AAB a prueba cerrada y los 12 probadores × 14 días; textos y
  checklist en [PLAY.md](PLAY.md).
- **Smoke test de la APK release en dispositivo:** con R8 activo conviene
  verificar arranque real en hardware; la última suite instrumental completa
  (28/28) corresponde al build debug del 2026-09-22.
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

Release y despliegue (JDK 17; `keystore.properties` y el keystore solo en la
máquina del desarrollador, credenciales AWS en el perfil `default`):

```sh
./gradlew :app:assembleRelease :app:bundleRelease :app:lintRelease
./tools/deploy-site.sh --apk              # sitio + APK + SHA256SUMS (S3 + SSM)
DUCKDNS_TOKEN=<token> ./tools/deploy-site.sh --duckdns   # refresca los registros A
```

## Criterios de mantenimiento

- Ningún cambio de regla entra sin test de dominio y actualización del GDD.
- Todo stub debe comportarse como se documenta o aparecer como deuda explícita.
- Toda cadena visible nueva se añade en `values/` y `values-es/`.
- Toda semilla usada para validar un nivel debe conservar reproducibilidad.
- Una nueva prioridad se añade al roadmap y aquí solo se resume con su estado.

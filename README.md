# Mainboard Override

Juego táctico de lógica para Android: el jugador conecta fichas de dominó
sobre una placa, ejecuta scripts con RAM limitada y debe alcanzar el nodo de
extracción antes de que el rastreo llegue al 100 %. Solo horizontal, offline
y bilingüe (ES/EN).

## Estado

Última release publicada: **0.1.0** (firmado, `versionCode 12`, etiqueta `v0.1.0`).
Versión en desarrollo: **0.1.2** (`versionCode 14`), todavía sin etiquetar.

Vertical slice jugable: motor Kotlin puro, app Jetpack Compose, generación
determinista por semillas, persistencia local de ajustes y récords, y pruebas
unitarias del bucle principal.

Estado verificado (2026-10-15): bloques A–F cerrados (A01–A10, B01–B06,
C01–C06, D01–D04, E01–E06, F01–F03), bloques J (campaña a 100) y K (tienda)
cerrados, y salida de alfa completada: build de release firmado (APK + AAB,
etiqueta `v0.1.0`), ficha de tienda en `docs/PLAY.md` y sitio publicado con
**HTTPS** en sus tres nombres. Los siete scripts de campaña se desbloquean por
bandas de nivel (1–30 / 31–60 / 61–100) y el flag debug `unlockAll` abre todo en
depuración sin tocar el release. Suite de dominio **72/72** y
`:app:testDebugUnitTest` **21/21** en verde, lint sin errores (66 avisos,
ninguno nuevo) y suite instrumental **38/38** en 8 clases sobre el Huawei
CLK-LX3 por USB (leído del XML, 0 fallos). Smoke test de la APK release (R8)
sin excepciones en emulador API 35. Pendiente externo:
Play Console — detallado en el [Roadmap](docs/ROADMAP.md).

## Cómo se juega

- **Progresión:** 100 desafíos secuenciales en cinco fases, con nodos de
  datos obligatorios desde el 41. Cada 5 desafíos distintos
  completados desbloquea un escenario del modo libre: Laboratorio, Centro de
  datos, Red industrial, Archivo profundo, Núcleo blindado y Red fantasma.
  Cada escenario tiene dimensiones y obstáculos propios, con generación
  reproducible por semilla y solución comprobada por el motor.
- **Economía:** cada victoria otorga 20 créditos y la primera victoria de cada
  desafío añade 40. Repetir desafíos concede solo los 20 de victoria; perder y
  abandonar no dan créditos. La Tienda vende placas y fichas: tres placas
  (pcb, plano azul, industrial) y tres fichas (Kenney, oscura, jengibre) son
  gratuitas; las ocho premium (Cobre, Aurora, Titanio, Jade, Rubí, Zafiro,
  Ámbar y Amatista, placa o ficha) cuestan 200 y el resto de pago 80, con
  posesión por categoría. No hay créditos retroactivos.
- **Tutorial:** diez lecciones jugables sobre conexiones, rotación, turnos,
  peligros y los siete scripts (PING, SPOOF, KILL, BRIDGE, STEALTH, CLOAK y
  STUN). Guarda la última
  lección para continuar o reiniciar, sin alterar créditos, récords ni partidas.
- **Scripts:** PING (revela honeypots y amplía la vista de la próxima ficha),
  SPOOF (reescribe un puerto de una ficha en mano), KILL (elimina firewall,
  honeypot visible o ficha completa), BRIDGE (copia un puerto a través de un
  firewall), STEALTH (descarta el ruido pendiente del turno por 1 RAM), CLOAK
  (congela el rastreo base de 8 durante dos turnos) y STUN (detiene el avance
  del daemon una fase). Los
  botones «?» explican indicadores, scripts y controles.
- **Detalles:** las tarjetas de desafíos y escenarios abren un detalle con
  previsualización (ejemplo con semilla fija en modo libre) antes de jugar.
  Al finalizar hay resumen con revisión del tablero, reintento y avance al
  siguiente desafío. El menú respeta movimiento reducido.

## Compilar y validar

Requisitos: JDK 17 con `javac` y Android SDK 37.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # si el java por defecto es un JRE sin javac
./gradlew test
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Las pruebas
de interfaz (`./gradlew :app:connectedDebugAndroidTest`) requieren un
dispositivo o emulador autorizado; por USB en vez de adb WiFi.

## Descarga directa

La landing publicada en
[mainboard-override.duckdns.org](https://mainboard-override.duckdns.org)
ofrece la APK de release con versión, tamaño y SHA-256 (también `latest.apk`
y `SHA256SUMS`). En el servidor (nginx + Let's Encrypt sobre una `t4g.micro`
de AWS, ~7,3 $/mes) hay además dos vhosts preparados:
`nexxxusapp.duckdns.org` (landing de Nexus Chat) y `nexxus-api.duckdns.org`
(API, proxy comentado). Ver [docs/ESTADO.md](docs/ESTADO.md).

## Release y publicación

Build de release (APK firmada + AAB para Play). Requiere un
`keystore.properties` **gitignored** en la raíz con `storeFile`,
`storePassword`, `keyAlias` y `keyPassword`; el keystore vive fuera del
repositorio (nunca se commitea):

```bash
./gradlew :app:assembleRelease :app:bundleRelease :app:lintRelease
```

- **Descarga directa + sitio:** `./tools/deploy-site.sh --apk` sube la APK,
  actualiza `SHA256SUMS`, el symlink `latest.apk` y la landing (usa el AWS CLI
  + SSM; `--duckdns` con `DUCKDNS_TOKEN` refresca los registros A).
- **itch.io:** `tools/publish-itch.sh` (ver abajo).
- **Google Play:** AAB + textos y checklist en [docs/PLAY.md](docs/PLAY.md).

## Publicar en itch.io

`tools/publish-itch.sh` compila el APK y lo sube con
[butler](https://itch.io/docs/butler) al canal `android` de
[aela-0593/mainboard-override](https://aela-0593.itch.io/mainboard-override),
usando `versionName` como `--userversion`. Por defecto sube la debug; con
`--release` sube la APK firmada de release (requiere `keystore.properties`).
Necesita `BUTLER_API_KEY` en el entorno (clave con permiso de subida desde tu
página de API keys de itch.io):

```bash
BUTLER_API_KEY=<clave> ./tools/publish-itch.sh [--release] [canal]
```

## Documentación

| Documento | Contenido |
| --- | --- |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Único plan operativo: orden, dependencias y criterios de cierre |
| [docs/ESTADO.md](docs/ESTADO.md) | Estado comprobable, decisiones cerradas y deuda activa |
| [docs/PLAY.md](docs/PLAY.md) | Ficha de Google Play: textos ES/EN, assets, Data safety y checklist |
| [docs/GDD.md](docs/GDD.md) | Reglas, progresión y seguimiento del diseño |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Flujo de estado, contratos y mantenimiento |
| [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md) | Banco de ideas sin compromiso de entrega |
| [docs/CARTAS.md](docs/CARTAS.md) | Especificación visual de cartas e iconos |

## Arte y licencias

Las fichas usan los gráficos de Kenney (`assets/kenney/kenney_domino-pack.zip`, CC0).
Las skins y los fondos de escenario se generaron con la herramienta integrada
`imagegen`; prompts y originales están en `assets/`, y solo las exportaciones
reducidas se empaquetan en `drawable-nodpi`. Licencias en
`app/src/main/assets/licenses/`. El icono del launcher usa la exportación
reducida (`logo_foreground.png`) del original en `assets/logo/`, con procedencia
documentada en `assets/logo/procedencia.md`.

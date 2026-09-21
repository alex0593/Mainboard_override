# Mainboard Override

Juego táctico de lógica para Android: el jugador conecta fichas de dominó
sobre una placa, ejecuta scripts con RAM limitada y debe alcanzar el nodo de
extracción antes de que el rastreo llegue al 100 %. Solo horizontal, offline
y bilingüe (ES/EN).

## Estado

Versión actual: **0.1.0-alpha.20**.

Vertical slice jugable: motor Kotlin puro, app Jetpack Compose, generación
determinista por semillas, persistencia local de ajustes y récords, y pruebas
unitarias del bucle principal.

Ayer (2026-09-20) se cerraron las causas raíz de los fallos de dominio
(A01–A07, suite 28/28 verde) y la reapertura limpia sin recompensas duplicadas
(B06). Tutorial con 10 lecciones verificado en dispositivo (`PuzzleTutorialUiTest`
6/6 por USB). Hay un ajuste local de alturas de texto pendiente de revisar
(`GameHeader.kt`, `MainboardApp.kt`).

## Cómo se juega

- **Progresión:** 30 desafíos secuenciales. Cada 5 desafíos distintos
  completados desbloquea un escenario del modo libre: Laboratorio, Centro de
  datos, Red industrial, Archivo profundo, Núcleo blindado y Red fantasma.
  Cada escenario tiene dimensiones y obstáculos propios, con generación
  reproducible por semilla y solución comprobada por el motor.
- **Economía:** cada victoria otorga 20 créditos y la primera victoria de cada
  desafío añade 40. Repetir desafíos concede solo los 20 de victoria; perder y
  abandonar no dan créditos. Las PCB Cobre y Aurora cuestan 200 cada una, son
  de compra única y se equipan desde la galería. No hay créditos retroactivos.
- **Tutorial:** diez lecciones jugables sobre conexiones, rotación, turnos,
  peligros y los cuatro scripts. Guarda la última lección para continuar o
  reiniciar, sin alterar créditos, récords ni partidas.
- **Scripts:** PING (revela honeypots y amplía la vista de la próxima ficha),
  SPOOF (reescribe un puerto de una ficha en mano), KILL (elimina firewall,
  honeypot visible o ficha completa) y BRIDGE (copia un puerto a través de un
  firewall). Los botones «?» explican indicadores, scripts y controles.
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

## Publicar en itch.io

`tools/publish-itch.sh` compila el APK y lo sube con
[butler](https://itch.io/docs/butler) al canal `android` de
[aela-0593/mainboard-override](https://aela-0593.itch.io/mainboard-override),
usando `versionName` como `--userversion`. Necesita `BUTLER_API_KEY` en el
entorno (clave con permiso de subida desde tu página de API keys de itch.io):

```bash
BUTLER_API_KEY=<clave> ./tools/publish-itch.sh [canal]
```

## Documentación

| Documento | Contenido |
| --- | --- |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Único plan operativo: orden, dependencias y criterios de cierre |
| [docs/ESTADO.md](docs/ESTADO.md) | Estado comprobable, decisiones cerradas y deuda activa |
| [docs/GDD.md](docs/GDD.md) | Reglas, progresión y seguimiento del diseño |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Flujo de estado, contratos y mantenimiento |
| [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md) | Banco de ideas sin compromiso de entrega |
| [docs/CARTAS.md](docs/CARTAS.md) | Especificación visual de cartas e iconos |

## Arte y licencias

Las fichas usan los gráficos de Kenney (`kenney_domino-pack.zip`, CC0).
Las skins y los fondos de escenario se generaron con la herramienta integrada
`imagegen`; prompts y originales están en `assets/`, y solo las exportaciones
reducidas se empaquetan en `drawable-nodpi`. Licencias en
`app/src/main/assets/licenses/`.

# Mainboard Override

Vertical slice de un juego táctico de lógica para Android. El jugador conecta fichas de dominó sobre una placa, ejecuta scripts con RAM limitada y debe alcanzar el nodo de extracción antes de que el rastreo llegue al 100 %.

## Estado

El repositorio contiene el motor Kotlin puro, una aplicación Jetpack Compose horizontal, generación determinista por semillas, persistencia local de ajustes y récords, recursos en español e inglés y pruebas unitarias del bucle principal.

Consulta [el GDD y seguimiento](docs/GDD.md) para conocer las reglas, lo terminado y el trabajo pendiente. La separación interna y el flujo de estado están descritos en [la documentación técnica](docs/ARCHITECTURE.md).

## Compilar

Requisitos: JDK 17 y Android SDK 37.

```bash
./gradlew test
./gradlew :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Fichas, ayuda y controles

Las fichas usan los gráficos claros de Kenney, con puntos en la mano, el tablero y las vistas previas. Los botones «?» dentro de la partida explican indicadores, scripts, controles y símbolos del tablero sin gastar recursos. Sustituyen el acceso al tutorial del menú. Consulta [la guía de ayuda y controles](docs/TUTORIAL.md) para conocer accesibilidad, compatibilidad y pruebas. SPOOF permite editar ambos puertos con previsualización y cancelación; BRIDGE admite dos orientaciones y PING muestra las próximas fichas.

Antes de entregar cambios, ejecutar también `./gradlew :app:lintDebug`. Las pruebas de interfaz se ejecutan con `./gradlew :app:connectedDebugAndroidTest` y requieren un dispositivo o emulador autorizado.

Los gráficos proceden de `kenney_domino-pack.zip` (Kenney, CC0). La licencia se incluye en `app/src/main/assets/licenses/kenney-domino-pack.txt`.

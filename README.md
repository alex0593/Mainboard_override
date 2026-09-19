# Mainboard Override

Vertical slice de un juego táctico de lógica para Android. El jugador conecta fichas de dominó sobre una placa, ejecuta scripts con RAM limitada y debe alcanzar el nodo de extracción antes de que el rastreo llegue al 100 %.

## Estado

Versión actual: **0.1.0-alpha.19**.

El repositorio contiene el motor Kotlin puro, una aplicación Jetpack Compose horizontal, generación determinista por semillas, persistencia local de ajustes y récords, recursos en español e inglés y pruebas unitarias del bucle principal.

Consulta [el GDD y seguimiento](docs/GDD.md) para conocer las reglas, lo terminado y el trabajo pendiente. La separación interna y el flujo de estado están descritos en [la documentación técnica](docs/ARCHITECTURE.md).

## Progresión y personalización

Hay 30 desafíos secuenciales. Cada 5 desafíos distintos completados se desbloquea un escenario del modo libre: Laboratorio, Centro de datos, Red industrial, Archivo profundo, Núcleo blindado y Red fantasma. Cada escenario tiene dimensiones y obstáculos propios, con generación reproducible por semilla y solución comprobada por el motor.

Una victoria otorga 20 créditos y la primera victoria de cada desafío añade 40. Repetir desafíos concede solo los 20 de victoria; perder y abandonar no dan créditos. Los créditos permiten comprar las PCB Cobre y Aurora por 200 cada una. Las compras son permanentes y se equipan desde la galería. También hay dos skins gratuitas de fichas y placa (Circuito obsidiana y Circuito cerámico), exportadas a resolución reducida para limitar el uso de memoria. El progreso anterior desbloquea escenarios sin otorgar créditos retroactivos.

El resumen permite revisar el tablero final, volver a los resultados, reintentar o avanzar al siguiente desafío. El menú tiene pulsos animados que se desactivan con movimiento reducido. Las pruebas de progresión usan un DataStore aislado en caché y no modifican el perfil real.

Las tarjetas de desafíos y dificultades abren detalles antes de iniciar la partida. Las previsualizaciones se generan bajo demanda y se guardan en una caché limitada; el modo libre muestra un ejemplo etiquetado, no el futuro tablero aleatorio. Las dificultades bloqueadas muestran candado y no permiten entrar.

El botón Tutorial del menú ofrece diez lecciones jugables sobre conexiones, rotación, turnos, peligros y los cuatro scripts. Guarda la última lección para continuar o reiniciar, sin alterar créditos, récords ni partidas. Los controles de partida tienen espacio propio y las tarjetas de skins son compactas y centradas.

## Compilar

Requisitos: JDK 17 y Android SDK 37.

Gradle necesita un JDK completo con `javac`. Si el Java predeterminado apunta al runtime de Minecrack, selecciona el JDK instalado antes de compilar: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (ruta de este equipo).

```bash
./gradlew test
./gradlew :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Fichas, ayuda y controles

Las fichas usan los gráficos claros de Kenney, con puntos en la mano, el tablero y las vistas previas. Los botones «?» explican indicadores, scripts y controles sin gastar recursos. SPOOF permite editar ambos puertos; BRIDGE copia un puerto de una ficha a través de un firewall y deja libre la salida para continuar con una ficha compatible. PING muestra solo la siguiente ficha y revela un honeypot oculto al azar.

Los mapas libres tienen 6 filas y anchos progresivos de 8, 9, 9, 10, 10, 11 y 11 columnas. La geometría de los desafíos se conserva; las semillas del modo libre cambian de mapa con esta actualización.

Los firewalls usan el icono de chip verde y los honeypots revelados el de circuitos con flecha hacia abajo, también en las vistas previas. Sobre fichas, el honeypot aparece como insignia pequeña. Los PNG proporcionados se incluyen como `board_firewall.png` y `board_honeypot.png`; el componente recorta los márgenes transparentes al cargarlos y comparte las imágenes en memoria entre casillas, conservando los archivos originales.

Antes de entregar cambios, ejecutar también `./gradlew :app:lintDebug`. Las pruebas de interfaz se ejecutan con `./gradlew :app:connectedDebugAndroidTest` y requieren un dispositivo o emulador autorizado.

Los gráficos proceden de `kenney_domino-pack.zip` (Kenney, CC0). Las skins tecnológicas se generaron con la herramienta integrada `imagegen`; sus prompts y originales están en `assets/skins/`. La licencia se incluye en `app/src/main/assets/licenses/kenney-domino-pack.txt`.

# Validación de pantalla completa — 2026-09-13

## Resultado

Compilación e instalación correctas con JDK 17 y SDK 37. `test` pasó
29 pruebas locales; `connectedDebugAndroidTest`, 20 pruebas sin fallos.
`lintDebug` terminó sin errores y con 57 advertencias.

El HONOR CLK-LX3 con Android 14/API 34 pasó las comprobaciones de barras ocultas
al iniciar, reanudar, recrear la actividad y abrir/cerrar el diálogo de salida.
Las pruebas existentes cubrieron ayuda, tutorial, selección de niveles y resultados.
Se corrigió una prueba del tutorial que intentaba desplazar el control fijo de Bridge.

Se repitieron las dos pruebas de inmersión con los overlays de navegación
`threebutton` y `gestural` habilitados temporalmente: ambos pasaron. Sus registros
están en los archivos `*-tests.txt`. Se restauró el overlay original `navbar.hide`.
Estos cambios de overlay no equivalen a certificar todas las preferencias de
navegación específicas de HONOR; no se modificó su ajuste persistente `navigation_mode`.

Las capturas `*-hidden-again.png` muestran el menú sin barras. `dialog-hidden.png`
muestra el diálogo después de ocultarse las barras transitorias. Se observó la
aparición temporal de la barra superior durante un gesto y su ocultación posterior.
Las capturas de arranque se descartaron por mostrar aún el splash del sistema.
La aplicación quedó instalada, abierta en el menú y con la navegación original restaurada.

## Matriz y límites

| Entorno | Estado |
| --- | --- |
| HONOR CLK-LX3, Android 14, horizontal | Validado físicamente |
| Overlays de tres botones y gestos en ese HONOR | Dos pruebas de inmersión aprobadas por variante |
| Android 8/9, 10/11 y 15/16 | Sin dispositivo o emulador disponible; no verificado |
| Otros fabricantes, horizontal invertido, bloqueo/desbloqueo físico y escritorio | No verificado físicamente |

La causa encontrada es que la aplicación no solicitaba ocultar navegación:
`enableEdgeToEdge` y el tema fullscreen no proporcionaban por sí solos esa política.
No se ha identificado una lista de modelos incompatibles. La solución usa AndroidX
desde el mínimo soportado API 26; la compatibilidad de API no sustituye pruebas
físicas en otras versiones y fabricantes.

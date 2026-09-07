# Tutorial interactivo

## Entrada y alcance

Desde el menú, **EJECUTAR TUTORIAL** inicia siempre la introducción. Está disponible también después de completarlo. El tutorial usa las reglas normales de colocación, scripts, RAM y rastreo; la guía restringe las acciones durante los ejercicios para mantenerlos resolubles. La práctica final permite jugar libremente.

El panel amarillo muestra el paso actual, el objetivo y el progreso. Los controles o celdas relevantes se resaltan en amarillo; el verde indica colocaciones válidas para la ficha y orientación seleccionadas. **Continuar** aparece solo en explicaciones y resultados de ejercicios: no permite saltarse una acción pendiente. Una acción rechazada conserva el paso y los recursos.

**Reiniciar lección** restaura el escenario de la lección actual, incluida mano, tablero, RAM, selecciones y orientación. En la práctica final reinicia esa partida. **Menú principal** permite salir. No se guarda una sesión activa al cerrar el proceso: volver a ejecutar el tutorial comienza desde la introducción.

## Recorrido

Las coordenadas visibles empiezan en 1; el motor utiliza coordenadas desde 0.

| Paso | Acción o aprendizaje | Condición de avance |
|---|---|---|
| Introducción | Inicio 0, extracción 6, conexión interna y contactos externos iguales | Continuar |
| Selección | Seleccionar `[0 | 2]` | Selección de la ficha existente |
| Rotación | Pulsar ROTAR una vez; revisar orientación y orden de puertos | Rotación realizada |
| Colocación | Colocar verticalmente en columna 2, fila 4 | Colocación exacta aceptada por el motor |
| Fin de turno | Colocar exactamente una ficha y ejecutar el turno | Fin de turno aceptado |
| Sistema | RAM 3, rastreo +8 y ruido, honeypots, daemon y prioridad de derrotas | Continuar |
| PING | Ejecutar la carta preparada | Script aceptado |
| Resultado PING | Observar trampa revelada y próximas tres fichas | Continuar |
| SPOOF | Seleccionar carta y `[3 | 2]`; primer puerto, valor 0, Aplicar | Cambio esperado aceptado |
| Resultado SPOOF | Observar `[0 | 2]` y explicar cambio permanente | Continuar |
| KILL_PROCESS | Seleccionar carta y firewall en columna 2, fila 4 | Eliminación aceptada |
| Resultado KILL | Observar celda libre y RAM agotada | Continuar |
| BRIDGE | Seleccionar carta y orientación horizontal; firewall en columna 4, fila 4 | Puente entre los dos puertos 2 aceptado |
| Resultado BRIDGE | Observar puente; el firewall permanece | Continuar |
| Práctica final | Resolver la semilla tutorial sin restricciones de acciones | Victoria tras resolver el sistema |
| Completado | Mensaje final y acceso al menú o reintento | Preferencia de finalización persistida |

Cada script tiene un escenario independiente con recursos suficientes. No depende del orden aleatorio de cartas de una partida normal. Los pasos de resultado conservan el efecto visible hasta pulsar Continuar.

### Ruta de referencia para la práctica final

La semilla sigue siendo `LevelGenerator.TUTORIAL_SEED`. Para verificar una solución, colocar `route-1`, `route-2` y `route-3` horizontalmente en los orígenes internos `(1,3)`, `(3,3)` y `(5,3)`; colocar `route-4` verticalmente en `(7,2)`. Ejecutar el turno después de cada colocación. La victoria llega con 32 de rastreo si no se ejecutan scripts ni se activan trampas. Los identificadores de ruta son internos; la interfaz muestra los valores de las fichas.

## Controles compartidos con partidas normales

- **SPOOF:** seleccionar carta y ficha, elegir primer o segundo puerto y valor 0–6, revisar la vista antes/después y pulsar Aplicar. Cancelar o cerrar el diálogo no consume carta ni RAM. En el ejercicio guiado se exige primer puerto y valor 0.
- **BRIDGE:** seleccionar carta, alternar Horizontal/Vertical y pulsar el firewall. Los puertos a ambos lados deben existir y tener el mismo valor. `═` y `║` representan los puentes instalados sin ocultar su orientación a accesibilidad.
- **PING:** ejecutar la carta muestra hasta tres próximas fichas en el panel. La vista desaparece al resolver el turno; los honeypots permanecen revelados.
- **Rotación:** la vista indica orientación y orden de los dos puertos. El resaltado de colocaciones desaparece después de colocar la ficha del turno. Las fichas dobles mantienen destinos correctos con las cuatro rotaciones.
- **Errores:** se muestran instrucciones traducidas para corregir el problema, en vez del nombre interno de `RejectReason`.
- **Protocolo:** accesible durante la partida sin perder el estado; explica reglas, amenazas y scripts.

## Accesibilidad y presentación

Los textos están en recursos españoles e ingleses. Las celdas describen coordenadas, valores, inicio, extracción, firewalls, puentes, daemon, trampas reveladas y objetivo de la lección. Las trampas ocultas nunca se incluyen en sus descripciones. Fichas y cartas exponen selección; el panel de guía usa anuncios de región viva moderados. Los paneles y diálogos se desplazan verticalmente; las opciones de SPOOF y la cabecera se distribuyen en varias filas cuando es necesario.

Las celdas se posicionan explícitamente según sus coordenadas, evitando la superposición en el origen del tablero. El tamaño de las celdas depende del espacio disponible: es necesario comprobar la precisión de pulsación y navegación TalkBack en dispositivos pequeños. No hay animaciones nuevas ni dependencia de audio o vibración para entender la guía.

## Persistencia

El tutorial no actualiza la última semilla normal ni los mejores turnos/rastreo. Solo una transición de partida activa a victoria en el paso final marca `tutorialComplete`. Acciones posteriores al resultado no vuelven a registrar la victoria. Los valores existentes de DataStore se conservan, sin migración. El estado intermedio de las lecciones vive en el ViewModel y no sobrevive a la muerte del proceso.

## Verificación y mantenimiento

`TutorialTest` cubre colocación y fin de turno, cuatro escenarios deterministas, efectos de scripts, rechazos, acciones ajenas al objetivo, puntos de reinicio, victoria final, segundo puerto de SPOOF y puente vertical. `TutorialUiTest` también ejercita selección, rotación y colocación desde la pantalla completa en español e inglés, con un área de 640 × 360 y escala de fuente 1,3; comprueba que las celdas tienen posiciones diferentes. Cubre vista previa y cancelación de SPOOF, selección de puertos, orientación de BRIDGE, vista PING y un recorrido mediante ViewModel observado por Compose que verifica finalización única y aislamiento de récords.

```bash
./gradlew :game-domain:test :app:testDebugUnitTest
./gradlew :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

El último comando requiere un dispositivo o emulador Android conectado y autorizado; instala el APK de depuración y el de pruebas. Informes: `game-domain/build/reports/tests/test/`, `app/build/reports/lint-results-debug.html` y `app/build/reports/androidTests/connected/`.

Para cambiar una lección: actualizar el enum y las transiciones de `Tutorial`, el escenario y objetivo, sus recursos en ambos idiomas, los resaltados/controles, las pruebas y esta tabla. `Tutorial.after` debe seguir exigiendo aceptación del motor. No introducir recursos Android en `game-domain`.

### Comprobaciones manuales

Recorrer el tutorial en español e inglés; comprobar textos ampliados, pantalla horizontal pequeña, desplazamiento hasta todos los botones, cancelación de SPOOF, cambio de orientación del puente, regreso al menú y repetición. Con TalkBack, verificar orden de foco, anuncio de pasos y ausencia de información de trampas ocultas. Cerrar el proceso y confirmar que una nueva entrada comienza desde la introducción mientras se conserva la marca de completado.


### Resultado de esta entrega (7 de septiembre de 2026)

- Dominio: 12 pruebas correctas (7 del motor y 5 del tutorial), sin errores ni omisiones.
- APK de depuración: compilación correcta; `app/build/outputs/apk/debug/app-debug.apk`.
- Interfaz: 6 pruebas correctas en dispositivo CLK-LX3 con Android 14, sin errores ni omisiones. Incluyen español/inglés, texto ampliado, recorrido de ViewModel, controles de scripts y posición de celdas.
- `:app:testDebugUnitTest`: sin fuentes de pruebas JVM en el módulo Android; el dominio se prueba en JVM y el ViewModel en la suite instrumentada.
- TalkBack y cierre real del proceso: pendientes de verificación manual. Las pruebas de semántica y de área reducida no sustituyen una revisión de uso con lector de pantalla.
- La primera ejecución sin red no pudo resolver `com.android.tools.utp:gradle-work-action:32.4.0`; la ejecución con red descargó lo necesario y permitió completar las seis pruebas del dispositivo.

- Lint final: 0 errores y 3 advertencias preexistentes (target SDK 36, versión de Gradle y orientación fija). No se añadieron supresiones ni baselines. Se eliminaron los recursos antiguos de pista/error y se ajustó el texto de ruido detectado por lint. El APK se regeneró después de esta limpieza.

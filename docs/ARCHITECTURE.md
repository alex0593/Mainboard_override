# Arquitectura del código

## Pantalla completa y barras del sistema

`MainActivity` y las ventanas de `GameDialog` comparten `bindGameImmersion`:
ocultan navegación y estado mediante `WindowInsetsControllerCompat`, al adjuntar
la ventana y recuperar su foco. La actividad también lo reaplica al reanudarse.
Cada propietario retira sus callbacks al destruirse o salir de composición.
`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` permite recuperar temporalmente las barras;
no se usan bucles que interfieran con los gestos del sistema. El popup de SPOOF
no toma foco y conserva la política de la ventana principal.

El contenido navegable y los diálogos respetan `WindowInsets.safeDrawing`, que
protege controles frente a recortes y barras persistentes sin reservar el tamaño
de las barras ocultas. El fondo exterior permanece oscuro. La política aplica
a menús, partida y tutorial; no depende del fabricante ni cambia preferencias
de navegación del teléfono. El modo de escritorio o políticas del sistema pueden
mantener controles visibles: en ese caso se respetan sus insets.

Referencia: [modo inmersivo de Android](https://developer.android.com/develop/ui/views/layout/immersive).

## Módulos

- `game-domain`: Kotlin puro y sin dependencias Android. Contiene tipos inmutables, reglas, generación de niveles y la máquina de estados.
- `app`: actividad, navegación Compose, presentación, localización y DataStore.

## Flujo de estado

La UI observa un único `GameUiState` publicado por `MainViewModel`. Cada gesto se convierte en un `GameAction` y se entrega a `GameEngine.reduce`. El motor devuelve un `Transition` con el nuevo `GameState` y eventos efímeros; la UI nunca modifica el tablero directamente.

```text
Compose -> GameAction -> GameEngine -> Transition -> StateFlow -> Compose
```

`LevelGenerator.generate(seed)` es determinista. La semilla y toda la lógica viven en el dominio, por lo que una partida puede reproducirse sin Android.

La progresión local, el saldo y las compras viven en `PlayerPreferencesRepository` y DataStore. `finishMatch` guarda récord, recompensa e identificador de partida en una sola transacción; repetir el identificador no paga de nuevo. Las compras validan saldo y propiedad dentro de la misma transacción. Las victorias de modo libre añaden `scenarioId` a `completedScenarios` en esa misma transacción, por lo que desbloquear un escenario mediante desafíos no lo marca como jugado. `ScenarioCatalog` y `Rewards` no dependen de Android. Una economía futura con inventarios más complejos podrá migrar a Room.

`LevelGenerator.generateScenario(seed, scenarioId)` aplica dimensiones, longitud de ruta y obstáculos del escenario y valida la solución mediante el motor. El modo original conserva `generate(seed)`. El ViewModel valida los desbloqueos antes de generar una partida. Jugar de nuevo conserva el escenario y genera una semilla distinta en modo libre; los desafíos conservan la semilla del catálogo. Recuperar la última semilla desde el menú sigue siendo una acción explícita. La UI deriva los escenarios disponibles del número de desafíos distintos ganados y marca como completados solo los escenarios presentes en `completedScenarios`.

`GameUiState` identifica cada partida con UUID y conserva su recompensa y el estado de revisión del tablero. La revisión no altera `GameState`; el motor sigue rechazando acciones después de terminar. La galería comparte `Board` y `DominoImage` con la partida para que las vistas previas coincidan con las skins equipadas.

La partida presenta los scripts en una banda horizontal superior junto a los indicadores de RAM y rastreo. El panel lateral conserva fichas, previsualización y controles; el botón de ejecutar turno se mantiene al final del panel y se resalta con `endTurnHint` cuando se intenta colocar una segunda ficha. SPOOF usa una lista vertical desplazable de valores 0–6 y conserva la selección de mitad y la previsualización.

Los escenarios de modo libre pueden generar `BoardBuff.TRACE_COOLER` y `BoardBuff.RAM_RESERVE`. Cubrir una casilla consume el buff y emite `GameEvent.BuffCollected`; el primero reduce el rastreo ocho puntos y el segundo recupera una RAM, ambos con límites. Los desafíos no generan buffs.

## Contratos y mantenimiento

Las clases públicas centrales incluyen KDoc. Al cambiar una regla se debe actualizar primero el modelo o `GameAction`, cubrir el caso en `GameEngineTest` y reflejar cualquier diferencia de diseño en `GDD.md`. Los rechazos esperables usan `RejectReason`; no deben convertirse en excepciones de interfaz.

El proyecto fija AGP, Kotlin, Compose BOM y Gradle para que una semilla y una revisión sean reproducibles. `local.properties` es configuración local ignorada por control de versiones.


## Ayuda contextual, fichas y escenarios internos

`HelpTopic` centraliza títulos, explicaciones y el tipo de script para obtener costes del dominio. `GameScreen` conserva únicamente el tema abierto como estado de presentación; la ayuda contextual sigue disponible durante la partida. El menú ofrece TUTORIAL en lugar de PROTOCOLO.

`DominoImage` usa los 28 PNG claros de Kenney en `drawable-nodpi`: normaliza el par para elegir imagen y transforma el dibujo para conservar el primer puerto a la izquierda o arriba. Las imágenes colocadas se dibujan bajo las celdas táctiles y sus insignias; las descripciones de celda siguen usando los valores del motor. La licencia viaja en los assets del APK.

Las partidas usan directamente el generador de escenarios y el reductor del dominio. `GameState` y `GameAction` conservan sus contratos para partidas libres y desafíos. `BoardState.start` y `BoardState.extraction` son nodos virtuales fuera de la cuadrícula; `neighbors` los conecta con la primera y última columna sin tratarlos como celdas ocupables.

`MainViewModel` conserva el constructor Android con `Application` y añade uno con `PlayerPreferencesRepository` inyectable para pruebas. Captura el tipo de sesión antes de iniciar la escritura asíncrona de resultados y solo persiste una transición inicial a victoria.

Compose presenta `SpoofDialog`, `BridgeControl` y `PingPreview`. `PingPreview`
reutiliza `RotationPreview` para mostrar cada ficha con el mismo panel,
orientación, skin y valores que la ficha seleccionada para rotar. Las
asignaciones exhaustivas de enums a recursos obligan a considerar los textos al
añadir errores o resultados. Las descripciones accesibles se construyen desde
información visible del tablero.

Durante una partida, los rechazos del motor se muestran en una ventana descartable y no se renderizan dentro del inventario. El editor SPOOF queda como panel flotante para conservar accesibles los controles inferiores; Cancelar ocupa temporalmente el lugar de Rotar. El inventario de hardware usa una fila compacta de fichas verticales y los controles de Rotar/Ejecutar turno permanecen juntos al pie del panel.

## Selección y previsualizaciones

Las tarjetas de desafíos y modo libre no generan tableros. Solo una selección desbloqueada abre un diálogo con detalles y una imagen estática; jugar requiere confirmación explícita. La imagen del modo libre usa la semilla fija 42 y se etiqueta como ejemplo, no como la futura partida aleatoria.

El diálogo de escenarios muestra únicamente las dimensiones, ruta, obstáculos y requisito de desbloqueo; las previsualizaciones se mantienen fuera del diálogo, en las tarjetas de selección.

`PuzzlePreviewCache` genera fuera del hilo principal y guarda PNG de 640 × 400: LRU de 8 MiB en memoria y límite de 24 MiB en disco. La clave SHA-256 incluye modo, identificador, semilla, revisión del generador/renderizado y ambas skins. Hay carga, reintento, recuperación de archivos inválidos y exclusión mutua para evitar generación duplicada. Las trampas ocultas y soluciones nunca se dibujan. Incrementar REVISION al cambiar la geometría del generador o el renderizado.

El panel de partida mide sus controles al pie por separado del inventario desplazable. Cada control compacto conserva un objetivo táctil de al menos 48 dp. Todas las superficies táctiles —botones, cartas de script, fichas de la mano, tarjetas de escenario y desafío, chips de skins y campos de SPOOF— comparten `Modifier.pressFeedback` (`PressFeedback.kt`): mientras el dedo está abajo la superficie se hunde con un muelle y se atenúa, y vuelve al soltar. `Modifier.pressable` combina `clickable` con esa respuesta para los componentes propios, y los de Material3 reciben el mismo `MutableInteractionSource` para conservar el ripple. Con `LocalReducedMotion` activo se omite el hundimiento y queda solo el atenuado. Las casillas del tablero no la usan: su respuesta es el resaltado de destinos legales y la animación de colocación. Las cards exclusivas de skins tienen altura mínima de 240 dp, contenido centrado y crecimiento libre para fuentes ampliadas. El header separa los indicadores de turno/RAM/rastreo de los scripts con una línea vertical; bajo él reserva 14 dp (`pending-trace-band`), solo para la línea de ruido y rastreo pendiente antes de ejecutar turno, de modo que la PCB quede pegada al header sin desplazarse cuando aparece el aviso. La banda mantiene 14 dp fijos y la línea se mide con `wrapContentHeight(unbounded)` dentro de ella: con fuentes ampliadas el texto se desborda sobre su propio hueco en vez de estirar la banda y mover la PCB. Cuando el rastreo aumenta, `TraceIndicator` anima el incremento real hacia arriba junto al indicador sin cambiar la altura del header.

BRIDGE mantiene su orientación junto a Cancelar en los controles fijos, también en el tutorial. `GameEngine.scriptTargets` deriva los objetivos resaltados de la validación real de BRIDGE y KILL. PING amplía `pingPreview` por cada uso y conserva las trampas reveladas. En la partida, `PingHeader` sustituye al header durante cinco segundos: oculta los indicadores de turno/RAM/rastreo, la banda de scripts, el botón «?» y el arte del header, y deja visibles las fichas acumuladas en una fila horizontal con el rótulo y la barra de cuenta atrás, todos con el acento del escenario activo; al agotarse el tiempo vuelve el header normal. El panel es un hermano excluyente de `GameHeader`, no una capa encima: no usa `fillMaxSize`, que estiraría la banda hasta el alto disponible y desplazaría la PCB, y respeta el mínimo de 64 dp del header. El tutorial sigue usando `PingPreview` en el panel lateral. KILL puede eliminar inmediatamente cualquier honeypot visible o una ficha completa seleccionando cualquiera de sus mitades, con RAM suficiente. La destrucción se dibuja dentro de `Board`: solo KILL quita firewalls, honeypots revelados y fichas del estado, así que la UI compara con la instantánea anterior y lanza `DestructionBurst` sobre la huella destruida —dos celdas cuando cae una ficha— durante 460 ms, con destello, barras glitch, dos anillos y chispas; con movimiento reducido queda solo el destello. `sessionKey` (el `matchId` de la partida y lección con repetición en el tutorial) evita que reiniciar o repetir dispare la ráfaga sobre un tablero nuevo. PING sin información nueva se rechaza sin consumir recursos. Destruir un honeypot limpia sus tres conjuntos del tablero; eliminar una ficha retira el dominó completo. La UI elimina las insignias al observar el estado.

## Fondos de escenarios

Los fondos de escenarios se resuelven en UI mediante `scenarioBackgroundResource`.
`CircuitBackground` acepta un recurso opcional: `GameScreen` usa `scenarioId`
solo en partidas libres (`challengeLevel == null`); los desafíos conservan el
fondo compartido. Las tarjetas usan el mismo mapeo detrás de su miniatura y
etiquetas. Estos recursos no forman parte de las skins ni del estado persistido.
Los originales y prompts viven en `assets/scenarios/`; Android empaqueta solo
las exportaciones de 1280×640 en `drawable-nodpi`.
El menú principal también usa ese mapeo con `preferences.lastScenario`, que se
actualiza al iniciar un escenario libre y conserva `classic` para perfiles nuevos
o identificadores desconocidos. Los desafíos no cambian este valor.

## Tutorial aislado

La rotación del tutorial comparte `RotationPreview` con la partida: ventana flotante junto al hardware, orientación, ficha y valores de puertos con los mismos tamaños. Rotar utiliza el mismo control de partida y sigue pasando por el reductor guiado del tutorial.

La partida y el tutorial comparten `GameHeader`: fondo, indicadores de turno/RAM/rastreo y banda de scripts. El título y número de lección viven en la explicación. Siguiente, Repetir y la ayuda «?» pertenecen a esa ventana y desaparecen al cerrarla; no hay barra inferior permanente. Solo el texto se desplaza, conservando sus acciones visibles. Las pausas se reservan para las explicaciones clave; las acciones encadenadas quedan disponibles sin una confirmación intermedia. Cerrar la explicación no desmonta el tablero ni reinicia el estado de la práctica.

El tutorial reutiliza `SpoofDialog` de la partida y conecta sus selecciones al reductor del tutorial. Se muestra al cerrar la explicación y seleccionar una ficha; conserva el resaltado de la acción esperada. El editor limita su altura disponible, desplaza su contenido y reserva una fila fija para Aplicar y Cancelar.

El tutorial utiliza la distribución de partida: indicadores y scripts arriba, tablero a la izquierda y hardware con controles fijos a la derecha. `HardwareHand` comparte fichas de 48 dp de ancho con la partida y ajusta el marco al contenido. La explicación se superpone dentro de la pantalla, conserva visible parte del tablero y no abre un diálogo modal. Siguiente cierra el objetivo sin saltar acciones; solo el reductor valida cada práctica. La pantalla abre el objetivo del siguiente paso y avanza automáticamente a la siguiente lección al terminar. Repetir vuelve al inicio de la lección y vuelve a mostrar su objetivo.

`TutorialCatalog` contiene diez fixtures deterministas y un reductor que solo acepta la acción esperada de cada paso. Las acciones de juego pasan por `GameEngine`; los intentos incorrectos no cambian la práctica. Las explicaciones están en arrays de recursos españoles e ingleses.

`TutorialController` tiene su propio StateFlow y no llama al flujo de partida ni a `finishMatch`. Solo persiste la última lección y la finalización mediante DataStore. Continuar y repetir reconstruyen el fixture inicial de la lección. La práctica no modifica récords, desbloqueos, saldo ni una partida en curso.

# Arquitectura del código

## Módulos

- `game-domain`: Kotlin puro y sin dependencias Android. Contiene tipos inmutables, reglas, generación de niveles y la máquina de estados.
- `app`: actividad, navegación Compose, presentación, localización y DataStore.

## Flujo de estado

La UI observa un único `GameUiState` publicado por `MainViewModel`. Cada gesto se convierte en un `GameAction` y se entrega a `GameEngine.reduce`. El motor devuelve un `Transition` con el nuevo `GameState` y eventos efímeros; la UI nunca modifica el tablero directamente.

```text
Compose -> GameAction -> GameEngine -> Transition -> StateFlow -> Compose
```

`LevelGenerator.generate(seed)` es determinista. La semilla y toda la lógica viven en el dominio, por lo que una partida puede reproducirse sin Android.

La progresión local, el saldo y las compras viven en `PlayerPreferencesRepository` y DataStore. `finishMatch` guarda récord, recompensa e identificador de partida en una sola transacción; repetir el identificador no paga de nuevo. Las compras validan saldo y propiedad dentro de la misma transacción. `ScenarioCatalog` y `Rewards` no dependen de Android. Una economía futura con inventarios más complejos podrá migrar a Room.

`LevelGenerator.generateScenario(seed, scenarioId)` aplica dimensiones, longitud de ruta y obstáculos del escenario y valida la solución mediante el motor. El modo original conserva `generate(seed)`. El ViewModel valida los desbloqueos antes de generar una partida y conserva escenario y semilla al reintentar. La UI deriva los escenarios disponibles del número de desafíos distintos ganados.

`GameUiState` identifica cada partida con UUID y conserva su recompensa y el estado de revisión del tablero. La revisión no altera `GameState`; el motor sigue rechazando acciones después de terminar. La galería comparte `Board` y `DominoImage` con la partida para que las vistas previas coincidan con las skins equipadas.

La partida presenta los scripts en una banda horizontal superior junto a los indicadores de RAM y rastreo. El panel lateral conserva fichas, previsualización y controles; el botón de ejecutar turno se mantiene al final del panel y se resalta con `endTurnHint` cuando se intenta colocar una segunda ficha. SPOOF usa una lista vertical desplazable de valores 0–6 y conserva la selección de mitad y la previsualización.

Los escenarios de modo libre pueden generar `BoardBuff.TRACE_COOLER` y `BoardBuff.RAM_RESERVE`. Cubrir una casilla consume el buff y emite `GameEvent.BuffCollected`; el primero reduce el rastreo ocho puntos y el segundo recupera una RAM, ambos con límites. Los desafíos no generan buffs.

## Contratos y mantenimiento

Las clases públicas centrales incluyen KDoc. Al cambiar una regla se debe actualizar primero el modelo o `GameAction`, cubrir el caso en `GameEngineTest` y reflejar cualquier diferencia de diseño en `GDD.md`. Los rechazos esperables usan `RejectReason`; no deben convertirse en excepciones de interfaz.

El proyecto fija AGP, Kotlin, Compose BOM y Gradle para que una semilla y una revisión sean reproducibles. `local.properties` es configuración local ignorada por control de versiones.


## Ayuda contextual, fichas y escenarios internos

`HelpTopic` centraliza títulos, explicaciones y el tipo de script para obtener costes del dominio. `GameScreen` conserva únicamente el tema abierto como estado de presentación; la ayuda contextual sigue disponible durante la partida. El menú ofrece TUTORIAL en lugar de PROTOCOLO.

`DominoImage` usa los 28 PNG claros de Kenney en `drawable-nodpi`: normaliza el par para elegir imagen y transforma el dibujo para conservar el primer puerto a la izquierda o arriba. Las imágenes colocadas se dibujan bajo las celdas táctiles y sus insignias; las descripciones de celda siguen usando los valores del motor. La licencia viaja en los assets del APK.

Las partidas usan directamente el generador de escenarios y el reductor del dominio. `GameState` y `GameAction` conservan sus contratos para partidas libres y desafíos.

`MainViewModel` conserva el constructor Android con `Application` y añade uno con `PlayerPreferencesRepository` inyectable para pruebas. Captura el tipo de sesión antes de iniciar la escritura asíncrona de resultados y solo persiste una transición inicial a victoria.

Compose presenta `SpoofDialog`, `BridgeControl` y `PingPreview`. Las asignaciones exhaustivas de enums a recursos obligan a considerar los textos al añadir errores o resultados. Las descripciones accesibles se construyen desde información visible del tablero.

Durante una partida, los rechazos del motor se muestran en una ventana descartable y no se renderizan dentro del inventario. El editor SPOOF queda como panel flotante para conservar accesibles los controles inferiores; Cancelar ocupa temporalmente el lugar de Rotar. El inventario de hardware usa una fila compacta de fichas verticales y los controles de Rotar/Ejecutar turno permanecen juntos al pie del panel.

## Selección y previsualizaciones

Las tarjetas de desafíos y modo libre no generan tableros. Solo una selección desbloqueada abre un diálogo con detalles y una imagen estática; jugar requiere confirmación explícita. La imagen del modo libre usa la semilla fija 42 y se etiqueta como ejemplo, no como la futura partida aleatoria.

El diálogo de escenarios muestra únicamente las dimensiones, ruta, obstáculos y requisito de desbloqueo; las previsualizaciones se mantienen fuera del diálogo, en las tarjetas de selección.

`PuzzlePreviewCache` genera fuera del hilo principal y guarda PNG de 640 × 400: LRU de 8 MiB en memoria y límite de 24 MiB en disco. La clave SHA-256 incluye modo, identificador, semilla, revisión del generador/renderizado y ambas skins. Hay carga, reintento, recuperación de archivos inválidos y exclusión mutua para evitar generación duplicada. Las trampas ocultas y soluciones nunca se dibujan. Incrementar REVISION al cambiar la geometría del generador o el renderizado.

El panel de partida mide sus controles al pie por separado del inventario desplazable. Cada control compacto conserva un objetivo táctil de al menos 48 dp. Las cards exclusivas de skins tienen altura mínima de 240 dp, contenido centrado y crecimiento libre para fuentes ampliadas.

## Tutorial aislado

El tutorial utiliza la distribución de partida: indicadores y scripts arriba, tablero a la izquierda y hardware con controles fijos a la derecha. `HardwareHand` comparte fichas de 48 dp de ancho con la partida y ajusta el marco al contenido. La explicación se superpone dentro de la pantalla, conserva visible parte del tablero y no abre un diálogo modal. Siguiente cierra el objetivo sin saltar acciones; solo el reductor valida cada práctica. La pantalla abre el objetivo del siguiente paso y avanza automáticamente a la siguiente lección al terminar. Repetir vuelve al inicio de la lección y vuelve a mostrar su objetivo.

`TutorialCatalog` contiene diez fixtures deterministas y un reductor que solo acepta la acción esperada de cada paso. Las acciones de juego pasan por `GameEngine`; los intentos incorrectos no cambian la práctica. Las explicaciones están en arrays de recursos españoles e ingleses.

`TutorialController` tiene su propio StateFlow y no llama al flujo de partida ni a `finishMatch`. Solo persiste la última lección y la finalización mediante DataStore. Continuar y repetir reconstruyen el fixture inicial de la lección. La práctica no modifica récords, desbloqueos, saldo ni una partida en curso.

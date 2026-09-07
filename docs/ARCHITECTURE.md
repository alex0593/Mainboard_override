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

Campaña, inventario y economía deben introducir repositorios propios y Room sin añadir dependencias Android a `game-domain`.

## Contratos y mantenimiento

Las clases públicas centrales incluyen KDoc. Al cambiar una regla se debe actualizar primero el modelo o `GameAction`, cubrir el caso en `GameEngineTest` y reflejar cualquier diferencia de diseño en `GDD.md`. Los rechazos esperables usan `RejectReason`; no deben convertirse en excepciones de interfaz.

El proyecto fija AGP, Kotlin, Compose BOM y Gradle para que una semilla y una revisión sean reproducibles. `local.properties` es configuración local ignorada por control de versiones.


## Tutorial y presentación de scripts

`TutorialStep` y `Tutorial` viven en `game-domain`: definen escenarios deterministas, objetivos permitidos, transiciones tras acciones aceptadas y puntos de reinicio. `GameState` y `GameAction` conservan sus contratos. La colocación y ejecución de scripts siguen pasando por `GameEngine.reduce`; la guía no aplica efectos directamente al tablero.

`GameUiState` incorpora paso, aviso de acción ajena a la guía, puerto/valor de SPOOF y orientación de BRIDGE. El ViewModel valida selecciones y rotación, filtra acciones según el paso y llama a `Tutorial.after` con la transición del motor. Las explicaciones avanzan mediante `continueTutorial`; cada ejercicio nuevo carga su escenario, mientras que los resultados conservan el tablero para inspección. La práctica final utiliza el generador habitual sin filtrar sus acciones.

`MainViewModel` conserva el constructor Android con `Application` y añade uno con `PlayerPreferencesRepository` inyectable para pruebas. Captura el tipo de sesión antes de iniciar la escritura asíncrona de resultados y solo persiste una transición inicial a victoria. Las sesiones tutoriales no escriben récords ni última semilla normal; la finalización se guarda únicamente tras la práctica final.

Compose presenta `TutorialPanel`, `SpoofDialog`, `BridgeControl` y `PingPreview`. Las asignaciones exhaustivas de enums a recursos obligan a considerar los textos al añadir pasos, errores o resultados. Las descripciones accesibles se construyen desde información visible del tablero. Ver [TUTORIAL.md](TUTORIAL.md) para recorrido, restricciones y mantenimiento.

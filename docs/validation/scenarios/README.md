# Validación de fondos de escenarios

Fecha: 2026-09-14. Dispositivo: CLK-LX3, Android 14. JDK 17.

- `:app:assembleDebug`: correcto.
- `:app:testDebugUnitTest`: 3 pruebas correctas.
- `:app:lintDebug`: 0 errores, 57 advertencias.
- `:app:connectedDebugAndroidTest`: 20 de 21 pruebas correctas. La prueba
  `tutorialIsPlayableAndCanAdvanceAndRepeat` falla porque no encuentra el nodo
  `tutorial-repeat`; no se modificó el tutorial.
- `ScenarioArtworkUiTest`: correcta. Verifica siete recursos distintos de
  1280×640, respaldo para identificadores desconocidos, tarjetas bloqueadas y
  renderizado de cada escenario con skins `pcb` y `graphite`.
  La repetición aislada en horizontal también pasó (43.161 s); se revisaron
  capturas de selección y partidas, con fondo visible y controles legibles.
  También verifica que el recurso del menú siga `lastScenario` y vuelva a
  `classic` para valores desconocidos.
- Las pruebas existentes de reinicio libre y desafío, desbloqueo de escenarios
  y navegación a los detalles pasaron.
- `:game-domain:test`: 19 de 27 pruebas correctas. Fallan cinco casos de
  `GameEngineTest`, dos de `ScriptRulesTest` y uno de `TutorialTest`. No hay cambios
  en código ni pruebas de dominio en esta implementación.

Las capturas en `scenario-validation/`, `selection.png` y `selection-locked.png`, muestran las tarjetas;
`<id>-pcb.png` y `<id>-graphite.png` muestran las catorce combinaciones de partida.
Se generan en horizontal con un DataStore de prueba independiente del perfil real.

La prueba aislada se puede ejecutar instalando ambas APKs y usando:

```sh
adb shell am instrument -w -e class com.aela.mainboardoverride.ui.ScenarioArtworkUiTest com.aela.mainboardoverride.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.aela.mainboardoverride/files/scenario-validation docs/validation/scenarios
```

Recuperar las capturas antes de desinstalar la app; la limpieza de una ejecución
instrumentada de Gradle puede eliminar el directorio externo de la aplicación.

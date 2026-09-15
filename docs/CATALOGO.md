# Catálogo de trabajo — Mainboard Override

Índice operativo del proyecto: resume el estado comprobable, enlaza las fuentes
de detalle y conserva las decisiones que afectan al trabajo siguiente.

**Revisión:** 2026-09-15
**Estado:** vertical slice jugable con progresión, escenarios libres, tutorial,
localización ES/EN y arte temático empaquetado.

## Fuentes de verdad

- [GDD](GDD.md) — diseño de juego, reglas y experiencia prevista.
- [Arquitectura](ARCHITECTURE.md) — flujo de estado, contratos y mantenimiento.
- [Roadmap](ROADMAP.md) — prioridades y dependencias de desarrollo.
- [Ideas de mejora](IMPROVEMENTS.md) — banco de ideas sin compromiso de entrega.
- [Plan de auditoría](AGENT_PLAN.md) — contexto de revisión y riesgos conocidos.
- [Guía visual de cartas](<../CARTS spesificationst.md>) — especificación de cartas e iconos.
- [Fondos de escenarios](../assets/scenarios/README.md) — prompts, originales y exportaciones.
- [Validación visual de escenarios](validation/scenarios/README.md) — capturas y resultados instrumentados.

Los contratos técnicos viven en el código y los cambios de reglas deben reflejarse
en el GDD y en los tests del dominio. Este archivo no duplica sus detalles.

## Estado comprobado

| Área | Estado actual | Fuente principal |
| --- | --- | --- |
| Motor | Estado inmutable, acciones explícitas y `GameEngine.reduce`. | [Arquitectura](ARCHITECTURE.md) |
| Generación | Semillas reproducibles; niveles libres, escenarios y desafíos catalogados. | [Arquitectura](ARCHITECTURE.md) |
| Scripts | `PING`, `SPOOF`, `KILL` (`KILL_PROCESS` internamente) y `BRIDGE`. | [GDD](GDD.md) |
| Progresión | Desafíos desbloquean escenarios por victorias distintas; récords y créditos persisten. | `PlayerPreferencesRepository` |
| Economía | Compra de skins y recompensas implementadas con transacciones DataStore e idempotencia. | `PlayerPreferencesRepository` |
| Escenarios | Siete fondos generados, empaquetados y usados en tarjetas, partidas y menú. | [README de escenarios](../assets/scenarios/README.md) |
| Tutorial | Lecciones deterministas, flujo separado y progreso persistido. | [Arquitectura](ARCHITECTURE.md) |
| Localización | Recursos españoles e ingleses para la UI y el tutorial. | `app/src/main/res/values*` |
| Accesibilidad | Descripciones semánticas y objetivos táctiles mínimos en controles principales. | [Arquitectura](ARCHITECTURE.md) |

## Deuda activa y riesgos

### Confirmado en el código

- `ScenarioScreen` desbloquea por `challengeBest.size` y marca completado por
  `completedScenarios`; una victoria libre se persiste junto con el pago
  idempotente de `finishMatch`.
- Las partidas activas no se reanudan después de la muerte del proceso; solo se
  conservan preferencias, récords, créditos, skins, tutorial y última semilla.
- La UI sigue concentrada en archivos grandes, especialmente
  `MainboardApp.kt` y `ProgressionScreens.kt`, lo que aumenta el coste de cambios.

### Validación pendiente

- La ejecución de dominio del 2026-09-15 terminó con 27 pruebas y 8 fallos en
  `GameEngineTest`, `ScriptRulesTest` y `TutorialTest`; tareas A01–A08 del roadmap.
  Su cierre requiere resolver la causa y verificar la suite completa.
- La suite instrumental general tuvo un fallo aislado del tutorial al no
  encontrar `tutorial-repeat`; la prueba específica de escenarios pasó.
- La caché de previsualizaciones tiene invalidación por clave y revisión de
  renderizado, pero cualquier cambio futuro de geometría debe incrementar
  explícitamente su revisión.

### Decisiones todavía abiertas

- ¿Se guardará un récord por escenario libre para distinguir desbloqueado de
  completado?
- ¿Se implementará la reanudación de partidas o se mantendrá el reinicio como
  comportamiento intencional?
- El orden propuesto prioriza resolver los fallos de dominio antes de ampliar contenido.

## Decisiones recientes

- Los fondos de `classic`, `lab`, `data`, `industry`, `archive`, `core` y `ghost`
  son recursos independientes de las skins de tablero y fichas.
- El menú usa el fondo del último escenario libre jugado mediante
  `preferences.lastScenario`; perfiles nuevos y valores desconocidos vuelven a
  `classic`. Los desafíos no cambian ese valor.
- Las previsualizaciones de selección usan la semilla fija `42` y se etiquetan
  como ejemplos; iniciar una partida genera o conserva la semilla según el modo.
- Los textos de UI permanecen nativos y localizados; el arte no contiene nombres
  de escenarios ni botones incrustados.

## Prioridades activas

El [plan operativo](ROADMAP.md#4-plan-operativo-y-tareas) contiene tareas con
identificadores, dependencias y criterios de cierre:

1. **A01–A10 — P0:** corregir fallos de dominio y tutorial; cerrar validación.
2. **B03–B06 — P0/P1:** verificar flujos y decidir reanudación; B01–B02 y B04 ya están aplicadas.
3. **C01–C06 — P1:** modularidad, accesibilidad, feedback, caché, arte y localización.
4. **D01–D04 — P2:** semillas, equilibrio, tutorial y práctica.
5. **E01–E06 — P2:** ampliaciones propuestas; seleccionar alcance antes de implementar.

Las ideas no incluidas aquí permanecen en [IMPROVEMENTS.md](IMPROVEMENTS.md) y
no implican trabajo comprometido.

## Cómo verificar el estado

Desde la raíz del repositorio, con JDK 17 y el SDK configurado:

```sh
./gradlew :game-domain:test
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Para el arte de escenarios, revisar [la validación instrumentada](validation/scenarios/README.md),
que contiene capturas en horizontal y la prueba `ScenarioArtworkUiTest`.

## Criterios de mantenimiento

- Ningún cambio de regla entra sin test de dominio y actualización del GDD.
- Todo stub debe comportarse como se documenta o aparecer como deuda explícita.
- Toda cadena visible nueva se añade en `values/` y `values-es/`.
- Toda semilla usada para validar un nivel debe conservar reproducibilidad.
- Una nueva prioridad se añade al roadmap y aquí solo se resume con su estado.

# Plan — Rediseño de layout: sin huecos ni scroll forzado + tienda uniforme

Rama: `main`. Validación: tests + lint + suite instrumentada en CLK-LX3 (USB).

## Alcance final (aprobado en chat)

- **Enfoque B**: sistema de ventana común (`AppWindow` + `ArtHeader`) + ajustes por pantalla.
- **Menú**: 2 columnas (identidad + estado útil a la izquierda, acciones a la derecha), sin
  scroll, centrado. El panel central de estado que se probó fue descartado por el usuario
  ("innecesario"); el estado (saldo/meta/récord) vuelve integrado en la columna izquierda.
- **Ajustes**: dos paneles dentro de `AppWindow` (AJUSTES DE JUEGO con idioma + rejilla 2×2 de
  switches; VOLUMEN/SISTEMA con sliders) + franja inferior ACERCA DE (versión, saldo, línea
  decorativa). Sin scroll, sin recortes.
- **Tienda**: cards uniformes **240 dp** (20% menores) con **margin 5 dp** alrededor del
  contenido, anatomía fija (preview 100 dp, etiqueta, línea de estado, línea reservada,
  botón) — fichas y placas iguales. Scroll solo en los grids (tienda/escenarios/desafíos).
- Anuncios: punto abierto — `AppWindow` expone una banda inferior opcional (hoy vacía) para
  el banner futuro.

## Pendiente (fuera de alcance de esta tanda)

- [ ] Tutorial-inicio (sin decidir aún).
- [ ] Tablero de partida como rectángulo sin textura estirada (sin decidir aún).

## Tareas ejecutadas

- [x] 1. `AppWindow.kt` nuevo (shell + `ArtHeader`); `ProgressionHeader` delega en `ArtHeader`.
- [x] 2. Ajustes: doble panel + franja inferior, sin scroll (`SettingsScreen.kt`); strings
      nuevos EN/ES (`settings_game`, `settings_volume`, `about_decor`, `record_empty`).
- [x] 3. Menú: 2 columnas sin scroll (`MenuScreen.kt`), botones centrados verticalmente.
- [x] 4. Tienda: cards 240 dp uniformes con margin 5 dp (`ProgressionScreens.kt`).
- [x] 5. Fix preexistente: pin de overlays de escenarios actualizado a 1280×800 (assets
      regenerados a 2x en `4d961a0` sin actualizar los tests) — `PuzzleTutorialUiTest`,
      `ScenarioArtworkUiTest`, nota en `AGENTS.md`.
- [x] 6. Validación: unit tests verdes, lint 0 issues, suite instrumentada **31/31 en verde**
      sobre USB (los flakes `No compose hierarchies` se evitan haciendo `am force-stop` antes
      de la pasada; eran provocados por las animaciones infinitas de la tienda abierta).
- [x] 7. Instalado en CLK-LX3 y verificado por dumps uiautomator + perfil de píxeles.
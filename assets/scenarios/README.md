# Escenarios visuales

Imágenes temáticas por escenario, alineadas al lenguaje visual de la app:
mainboard oscuro, acentos en cian/ámbar, superficie clara para UI, sin texto dentro de la imagen porque los nombres ya vienen de strings.

## Escenarios

- `classic` — Red original
- `lab` — Laboratorio
- `data` — Centro de datos
- `industry` — Red industrial
- `archive` — Archivo profundo
- `core` — Núcleo blindado
- `ghost` — Red fantasma

## Archivos

- `assets/scenarios/originals/` — originales generados.
- `assets/scenarios/<id>-prompt.md` — prompt por escenario.
- `app/src/main/res/drawable-nodpi/scenario_<id>.png` — export reducido empaquetado.

## Generación e integración

Los siete fondos se generaron el 2026-09-14 con la herramienta integrada
`imagegen`, una llamada por escenario, usando exactamente el contenido de cada
`<id>-prompt.md`. No se utilizó CLI ni una API key.

Originales: `originals/classic.png`, `originals/lab.png`, `originals/data.png`,
`originals/industry.png`, `originals/archive.png`, `originals/core.png` y
`originals/ghost.png` (1774×887). Se conservan intactos, incluida su metadata.
Los siete recursos `scenario_<id>.png` son exportaciones RGB opacas de 1280×640,
reducidas con interpolación bicúbica mediante Java ImageIO.

`scenarioBackgroundResource` selecciona el recurso por identificador y usa
`classic` como respaldo. Las tarjetas lo muestran detrás de su contenido,
conservando la miniatura del tablero y el bloqueo. Las partidas libres lo usan
como fondo de pantalla con recorte centrado y opacidad de 0.72. Las skins del
tablero y las fichas siguen siendo independientes; desafíos y tutorial conservan
sus fondos anteriores. Las imágenes son decorativas y no contienen texto de UI.

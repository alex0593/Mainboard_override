# Prompt: sprite de lock

Original generado por el usuario y conservado en `originals/board_lock.png`;
export reducido en `app/src/main/res/drawable-nodpi/board_lock.png`.
Integrado en `Board.kt` (`BoardCell` y previsualizaciones) con el valor
exigido como insignia: suite 27/27.

## Qué es el número que ves hoy

En el tablero, una celda lock se dibuja como un número amarillo con borde
amarillo (por ejemplo un **3**). Ese número es el **valor exigido**: cualquier
ficha tuya que toque esa celda debe hacerlo con ese valor. Si la contactas
con otro valor, la partida termina al instante (`LOCK_TRIPPED`); si la
contactas con el valor exacto, el lock se consume y la celda queda libre.
KILL también puede eliminarlo. Los locks son siempre visibles (no se revelan
como los honeypots).

Hoy ese número va "pelado", sin icono propio: el firewall tiene su chip verde
y el honeypot sus circuitos con flecha, pero el lock no tiene sprite. Este
prompt es para generarlo con la misma herramienta y estilo que el resto.

## Prompt de generación

```
Transparent square game board sprite for Mainboard Override, dark PCB cyberpunk
game with terminal green, cyan and warning yellow. Primary request: a small
padlock fused with a circuit chip, top-down, dark metal lock body with warning
yellow shackle and keyhole glow, cyan circuit traces entering from two edges,
restrained neon, strong silhouette readable at 40 pixels, fills 85% of square.
Genuine transparent background. No text, letters, numbers, watermark, UI card
or surrounding scene.
```

Sin números ni letras dentro del sprite: el valor exigido lo dibuja la app
encima (Compose), igual que los valores de las cartas.

## Exportación e integración

- Original: `assets/board/originals/board_lock.png` (se conserva intacto).
- Export reducido: `app/src/main/res/drawable-nodpi/board_lock.png` (mismo
  flujo que `board_firewall.png` y `board_honeypot.png`).
- Integración pendiente en `Board.kt` (`BoardCell`): dibujar el sprite de fondo
  con el valor exigido encima, manteniendo el borde amarillo de 2 dp, la
  descripción de TalkBack (`node_lock`) y el resaltado de objetivo de KILL.
- El recorte de márgenes transparentes y la caché en memoria ya los cubre
  `threatBitmap`; reutilizarlos para el nuevo recurso.

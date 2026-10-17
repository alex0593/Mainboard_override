# Sprite STUN

Generado con Pollinations.ai (flux, text-to-image gratuito vía skill `pollinations-image`; el tier anónimo solo honra el prompt embebido en la **ruta** `/prompt/<texto>`, no en query string). Original: `originals/script_card_stun.png` (1254 × 1254, PNG con canal alfa).

Paraliza al daemon un turno: un nodo-chip recibe una **descarga** que lo congela —contorno eléctrico y banda de rayo atravesando el nodo en diagonal—. Basado en el brief de `docs/CARTAS.md` y en los sprites del proyecto `script_card_kill.png` y `script_card_bridge.png` como referencias de estilo (ámbar = chispa/alerta en la familia). Sin texto ni valores incrustados.

Entrega de arte solamente; no se modifica la UI ni la lógica del script (el cableado en `ScriptCards.kt` se hizo aparte).

## Prompt exacto

one single fused object: a square microchip icon whose outline is drawn as a jagged electric lightning bolt, chip pins along its edges, glowing vivid amber yellow neon wireframe with danger red inner circuitry traces, one small spark burst at its top corner, abstract flat vector game icon, pure black background, cyberpunk terminal aesthetic, high contrast, crisp glowing lines, centered with wide empty margins, no photograph, no animal, no creature, no people, no real world scene, no text, no letters, no numbers, no logo, no watermark, no frame, no border

## Proceso (6 iteraciones)

1. **v1**: prompt en query string —el tier anónimo lo ignora y devuelve imagen determinista por seed—; el usuario lo describió como «una bocina en una calle». Descartada.
2. **v3** (formato ruta, seed 888): marco gris con mancha; no lee como nodo + descarga. Descartada.
3. **v4** (seed 1204): casi vacía, solo una chispa roja (fg 2,6 %). Descartada.
4. **v5** (seed 512, sujeto al principio del prompt): blob triangular rojo/ámbar con energía correcta pero sin lectura de nodo+rayo. Descartada.
5. **v6** (seed 2026, objeto fusionado chip+rayo): composición aceptada —chip romboidal con pines y banda diagonal de descarga, rojo saturado conservado sobre negro—.
6. **Postproceso final** (Pillow, determinista): keying por distancia al color de borde, filtro de componentes conexos con descarte de blobs en esquinas (elimina el borrón inferior derecho), **tinte ámbar fósforo** (`#FFD43B` alto, `#3A0E12` bajo) sobre píxeles poco saturados (los rojos saturados se conservan), recorte al bbox y encaje centrado en lienzo 1254 × 1254 con márgenes 10 % laterales / 18 % superior e inferior.

## Crítica final (honestidad)

La pieza es un icono tech abstracto legible a 128 px (banda diagonal de alta energía, rojo/ámbar en paleta, fondo transparente), pero **más esquemática** que los sprites de la familia y sin la lectura literal de «nodo paralizado» (el chip se intuye romboidal, no frontal). Se acepta dentro del límite de 6 iteraciones; una regeneración con API key y referencias de estilo reales (tipo imagegen con inputs) daría mejor resultado.

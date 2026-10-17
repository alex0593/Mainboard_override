# Sprite CLOAK

Generado con Pollinations.ai (flux, text-to-image gratuito vía skill `pollinations-image`; el tier anónimo solo honra el prompt embebido en la **ruta** `/prompt/<texto>`, no en query string). Original: `originals/script_card_cloak.png` (1254 × 1254, PNG con canal alfa).

Congela la base del reloj de traza durante 2 turnos (el ruido pendiente sigue contando): un nodo-chip queda oculto bajo un **velo de señal** de líneas de barrido lima; los haces de escaneo se deslizan fuera del velo. Basado en el brief de `docs/CARTAS.md` y en los sprites del proyecto `script_card_stealth.png` y `script_card_ping.png` como referencias de estilo. Sin texto ni valores incrustados.

Entrega de arte solamente; no se modifica la UI ni la lógica del script (el cableado en `ScriptCards.kt` se hizo aparte).

## Prompt exacto

monochrome green phosphor neon wireframe game icon on pure black background, abstract flat vector, cyberpunk terminal aesthetic, printed circuit board style, vivid terminal green glow only, no white light: a single square computer microchip with short pins on all four sides, drawn in vivid green neon outlines, partially hidden behind a square grid camouflage shroud of thin translucent green lines, the chip clearly readable behind the grid, one tiny cyan status dot, high contrast, symmetric, centered with wide empty margins, no photograph, no animal, no creature, no people, no real world scene, no text, no letters, no numbers, no logo, no watermark, no card frame

## Proceso (6 iteraciones)

1. **v1–v2**: el tier anónimo de Pollinations ignora el prompt en query string (`/prompt/?prompt=…`): la imagen es función solo del seed → resultados figurativos ajenos a la familia (el usuario reportó «un gato»). Se descartaron.
2. **v3–v4** (formato ruta, seed 301): siluetas demasiado tenues / raya vertical; ilegibles a 128 px. Descartadas.
3. **v5–v6** (semilla 777): por fin composición estable — marco-rejilla rectangular con el chip encendido en el centro, fondo casi negro. v6 añadió «monochrome green … no white light», que no logró verde en el render (el neón salió blanco), pero sí el fondo negro más limpio.
4. **Postproceso final** (Pillow, determinista): keying por distancia al color de borde (fondo → alfa 0), filtro de componentes conexos (mín. 1,2 % de área, elimina specks), gamma de alfa 0,65 para realzar el marco-rejilla, **tinte fósforo lima** (`#B6FF2E` alto, `#10231D` bajo) sobre píxeles poco saturados, recorte al bbox y encaje centrado en lienzo 1254 × 1254 con márgenes 10 % laterales / 18 % superior e inferior.

## Crítica final (honestidad)

La pieza es un icono tech abstracto, en paleta (lima sobre void), legible a 128 px y con margen de seguridad correcto, pero es **más plana y esquemática** que los sprites de la familia (que son hardware oscuro con más elementos secundativos). Se acepta dentro del límite de 6 iteraciones; si se reparte presupuesto de arte, regenerar con el mismo prompt en el modo con API key/prompt realce.

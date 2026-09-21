# Logo de Mainboard Override

Archivo final: `mainboard-override-logo.png`, PNG RGB de 1024 × 1024.

Exportación del diseño de `app/src/main/res/drawable/ic_launcher_foreground.xml` sobre el fondo `#07110F` definido en `app/src/main/res/values/colors.xml`. Se conservan las coordenadas del viewport 108 × 108, las curvas cuadráticas, los grosores y los colores originales. Renderizado con Java2D y suavizado de bordes.

Se probó la herramienta integrada imagegen con una referencia rasterizada del vector y el siguiente prompt. La variante generada se descartó porque introdujo un halo y alteró el fondo. El PNG final es la exportación fiel del recurso original.

## Prompt utilizado

> Use case: logo-brand. Asset type: existing Android app logo exported as a square 1024x1024 PNG. Input image: exact reference and edit target. Reproduce this icon faithfully, preserving its layout, proportions and flat solid colors. Background #07110F; domino fill #10231D; outline and horizontal divider #39E7E0; exactly six round pips #B6FF2E arranged as two columns of three in the upper half; lower half empty. Preserve the reference's centered position, generous outer margins, rounded corners, outline thickness and divider position. No text, no shadow, no gradients, no glow, no 3D effects, no additional objects. This is a faithful reproduction, not a redesign.

Validación: inspección visual de los seis puntos, mitad inferior vacía, encuadre y colores; comprobación del formato y dimensiones del archivo. No se modifica el icono instalado de Android.

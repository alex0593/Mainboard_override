# Crear imágenes de escenario — Mainboard Override

## 1. Qué hay ahora

Los escenarios existen en código y en strings:

- `game-domain/src/main/kotlin/.../domain/Progression.kt`
  - `classic`
  - `lab`
  - `data`
  - `industry`
  - `archive`
  - `core`
  - `ghost`

- `app/src/main/res/values/strings.xml`
  - `scenario_classic`
  - `scenario_lab`
  - `scenario_data`
  - `scenario_industry`
  - `scenario_archive`
  - `scenario_core`
  - `scenario_ghost`

- `app/src/main/res/values-es/strings.xml`
  - versiones en español de los mismos nombres.

El proyecto ya tiene una carpeta `assets/` con prompts y originales:
- `assets/board/`
- `assets/skins/`
- `assets/menu/`

El estilo visual actual es “mainboard oscuro, estilo neobrutal/terminal, acentos en cian/ámbar”.

## 2. Qué imágenes necesito

Para cada escenario quiero una imagen que:
- Represente el nombre/tema del escenario.
- Se parezca al lenguaje visual del juego.
- Sirva como apoyo visual en pantalla de selección o galería.
- No contradiga la línea actual de assets existentes.

Escenarios:
1. `classic` — Red original
2. `lab` — Laboratorio
3. `data` — Centro de datos
4. `industry` — Red industrial
5. `archive` — Archivo profundo
6. `core` — Núcleo blindado
7. `ghost` — Red fantasma

## 3. Cómo organizarlo

Mi recomendación es tener:
- Una carpeta dedicada a los originales de escenario, por ejemplo `assets/scenarios/originals/`.
- Un prompt por escenario, similar al estilo que ya usan los archivos `.md` de `assets/`.
- Versiones reducidas empaquetadas en `app/src/main/res/drawable-nodpi/` cuando estén listas.

Estructura sugerida:
- `assets/scenarios/originals/`
- `assets/scenarios/<id>-prompt.md`
- `app/src/main/res/drawable-nodpi/scenario_<id>.png` (o similar)

## 4. Qué pide cada escenario

Lo más coherente es interpretar cada nombre con una variante del mismo lenguaje de mainboard, manteniendo:
- Fondo claro para overlays/UI.
- Centro relativamente vacío o de tono neutro.
- Detalles de borde y acentos temáticos.
- Sin texto dentro de la imagen, porque los nombres ya están en strings.

Interpretación sugerida:

### classic — Red original
Mainboard base, tono oscuro, circuitos finos, sin temática extra fuerte. Es la referencia neutra.

### lab — Laboratorio
Tinte más limpio/frío, luces refrigeradas, sensores, paneles de prueba, trazos más ordenados, acento cian/verde frío.

### data — Centro de datos
Líneas más densas, racks, racks de servidor estilizados, luz ámbar/cian, mayor orden y repetición estructural.

### industry — Red industrial
Superficie más pesada, metal desgastado, detalles más gruesos, acentos ámbar/rojo suaves, sensación de infraestructura más dura.

### archive — Archivo profundo
Tonos más tranquilos, estantes/sellos de almacenamiento, luz tenue, recuerdos de datos guardados, acento frío y más “profundo”.

### core — Núcleo blindado
Superficie blindada, placas más duras, resalte de seguridad, acento cian más marcado pero contenido, sensación de núcleo protegido.

### ghost — Red fantasma
Efecto más etéreo/luminoso, trazos más tenues, acentos fantasmas o residuo de señal, atmósfera menos sólida, sin volumen fuerte.

## 5. Cómo proceder

Opción A — Yo los genero como imagen real:
Esto excede lo que puedo hacer directamente en este entorno. Si quieres, puedo preparar los prompts, los nombres de archivo y la organización de carpetas para que otros los generen o los peguen aquí.

Opción B — Yo armo los prompts y la estructura:
Creo los archivos `assets/scenarios/<id>-prompt.md` y la estructura de carpetas, con prompts listos para generar imágenes acordes a cada nombre y al estilo del proyecto.

Opción C — Empezamos con la galería existente:
Reutilizamos composition existente y añadimos solo los prompts y la planificación para después generar los archivos finales.

## 6. Mi recomendación

Crear primero la estructura y los prompts, porque así:
- Los escenarios tienen nombre, temática y prompt alineados.
- Se puede generar después con el mismo flujo que el resto de assets.
- No hay que adivinar estilo ni tamaño después.

Si quieres, te dejo que prepare esos prompts en `assets/scenarios/` y el catálogo de nombres/temáticas en un md pequeño.

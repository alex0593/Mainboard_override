# Google Play — ficha de tienda (release 0.1.0)

Fuente de verdad para publicar **Mainboard Override** en Google Play. Los textos
están en ES y EN listos para pegar; los pasos externos (Play Console) se marcan
como pendientes en `docs/ROADMAP.md` (tarea G05). Actualizar esta ficha en cada
release que cambie versión, textos o datos de contacto.

## 1. Datos técnicos

| Campo | Valor |
| --- | --- |
| Paquete / applicationId | `com.aela.mainboardoverride` |
| Título (máx. 30) | `Mainboard Override` (19) |
| Versión | `versionName 0.1.0` · `versionCode 12` |
| minSdk / targetSdk | 26 (Android 8.0) / **36** (cumple el requisito de API 36 desde el 31-ago-2026) |
| Orientación | Solo horizontal (`sensorLandscape`) |
| Permisos | Únicamente `VIBRATE` — **sin `INTERNET`**, sin cuentas, sin anuncios, sin analítica |
| Categoría | Juegos → Puzzle |
| Idiomas de la app | Español e inglés (`values/`, `values-es/`) |
| Play App Signing | Sí; certificado de subida SHA-256 `3af521aaa8202a90da5cbc4169e1ef3ca8fec0dc4d21aecd421976325a5aeeaf` |

## 2. Artefactos de release

| Artefacto | Ruta | Tamaño | SHA-256 |
| --- | --- | --- | --- |
| AAB (para Play) | `app/build/outputs/bundle/release/app-release.aab` | 56.698.614 B (54 MiB) | `6d0e9379a74a42c0678db3a0552537cbf15ab823e29c3bc2f273f68aa9da1387` |
| APK (descarga directa/itch) | `app/build/outputs/apk/release/app-release.apk` | 54.457.058 B (52 MB) | `504e82b41cea68b2101cd5c5101ceb30a4f427838091706565beb77e7a1c796d` |

Regenerar con `./gradlew :app:assembleRelease :app:bundleRelease` (JDK 17) y
volver a calcular los hashes. **Play recibe el AAB**; la APK firmada va al
sitio de descarga directa y a itch.io (`tools/deploy-site.sh --apk` y
`tools/publish-itch.sh`).

## 3. Textos de la ficha

### Título

```
Mainboard Override
```

### Descripción corta (máx. 80 caracteres)

```
Puzle táctico de dominós en la placa: conecta, corre scripts y escapa.
```

```
Domino puzzle on a circuit board: connect, run scripts and escape.
```

### Descripción larga (máx. 4000 caracteres) — ES

```
Intrusión en tiempo real. Dominós en la placa.

Mainboard Override es un puzle táctico para Android: conecta fichas de dominó
sobre una placa de circuitos para enrutar tu paquete hasta el nodo de
extracción antes de que el rastreo llegue al 100 %. Cada turno que pasa suma
ruido, así que planifica, rota y descarta con cuidado.

CÓMO SE JUEGA
• Coloca y rota dominós para trazar el camino del paquete.
• Cada movimiento alimenta el rastreo: el tablero se pone tenso.
• Cinco scripts con tope de 3 de RAM: PING (sondea), SPOOF (engaña al rastreo),
  KILL (rompe enlaces), BRIDGE (une tramos) y STEALTH (disipa el ruido).
• Cartas, guías en pista y un sistema de ruido legible: el tablero nunca miente.

CONTENIDO
• 30 desafíos con semillas reproducibles: mismas reglas, mismos huecos.
• Modo libre con 7 escenarios y récords locales.
• Tutorial de 10 lecciones que enseña las reglas sin manuales.
• 18 skins de ficha y 17 de placa para personalizar el tablero.
• Audio synthwave 100 % procedural y vibración táctil.

HECHO PARA MÓVIL
• Partidas cortas, 100 % offline: sin cuentas, sin anuncios, sin compras.
• Solo horizontal, diseñado para una mano en la pantalla.
• Español e inglés, intercambiables en cualquier momento.
• Progresión y récords guardados en tu dispositivo.

Requiere Android 8.0 o superior.
Conecta. Ejecuta. Escapa.
```

### Description (long, EN)

```
Real-time intrusion. Dominoes on the board.

Mainboard Override is a tactical puzzle for Android: connect domino tiles on a
circuit board to route your packet to the extraction node before the trace
reaches 100 %. Every passing turn adds noise, so plan carefully, rotate and
discard with intent.

HOW IT PLAYS
• Place and rotate dominoes to trace the packet's path.
• Each move feeds the trace: the board keeps the pressure up.
• Five scripts on a 3-RAM cap: PING (scout), SPOOF (spoof the trace),
  KILL (break links), BRIDGE (join segments) and STEALTH (clear the noise).
• Cards, hint guides and a readable noise system: the board never lies.

WHAT'S INSIDE
• 30 reproducible seeded challenges: same rules, same gaps.
• Free play with 7 scenarios and local records.
• A 10-turn tutorial that teaches the rules without a manual.
• 18 domino skins and 17 board skins to personalize the table.
• Fully procedural synthwave soundtrack and haptic feedback.

BUILT FOR MOBILE
• Short sessions, 100 % offline: no accounts, no ads, no purchases.
• Landscape only, designed for one hand on the screen.
• Spanish and English, switchable at any time.
• Progress and records stored on your device.

Requires Android 8.0 or higher.
Connect. Run. Escape.
```

## 4. Checklist de assets

| Asset | Especificación | Estado |
| --- | --- | --- |
| Icono de aplicación | 512×512 PNG **sin alfa ni redondeado** | ☐ (origen: `assets/logo/mainboard-override-logo.png`, exportar con `tools/PrepareSkinAsset.java`) |
| Gráfico de funciones | 1024×500 PNG/JPG | ☐ (composición con logo + placa) |
| Capturas teléfono | 2–8, **1920×1080 horizontal** (la app es solo horizontal), PNG | ☐ (tomar del dispositivo: `adb exec-out screencap -p > pantalla.png`) |
| Capturas tablet | 1080×1920 o 1920×1080 | ☐ (opcional en primera release) |
| Vídeo de presentación | 30–60 s, YouTube URL | ☐ (opcional) |

Capturas sugeridas (una por pantalla principal): 1) desafío en curso con el
rastreo visible, 2) tutorial con guías, 3) galería de skins, 4) modo libre con
escenario, 5) pantalla de victoria con récord.

## 5. Hoja «Seguridad de datos» (Data safety)

La app **no recoge ni comparte ningún dato**. Respuestas del formulario:

- ¿Recopila esta app algún dato? → **No**.
- ¿Comparte esta app algún dato? → **No**.
- Cifrado en tránsito → N/A (no hay red; la app **no tiene permiso
  `INTERNET`**).
- Puede eliminarse → N/A (no se guarda nada en servidores).
- Diseñada para cumplir con las normas de Play Family → **No** (no se
  dirige a menores de forma específica; evita requisitos adicionales).

Justificación interna (no se sube, queda aquí):

- Único permiso: `VIBRATE` (retroalimentación táctil).
- Persistencia local con DataStore Preferences: progresión, récords, ajustes
  de audio/idioma. `allowBackup="true"` → copia local del dispositivo, no es
  recogida de datos.
- Sin SDKs de terceros: ni analítica, ni anuncios, ni crash reporters, ni
  red social. Tampoco hay compras in-app (los créditos son de juego).

## 6. Política de privacidad (plantilla)

Si Play pide URL (no es obligatoria sin recogida de datos, pero tranquiliza),
hostear en `https://mainboard-override.duckdns.org/privacidad.html` con este
contenido:

```text
Política de privacidad — Mainboard Override
Última actualización: 2026-10-02

Mainboard Override no recoge, transmite ni comparte datos personales.

1. Datos que procesamos
   Ninguno. La app funciona sin conexión y no incluye el permiso de red
   INTERNET, por lo que no puede enviar información a terceros.

2. Almacenamiento local
   La progresión, los récords y los ajustes se guardan únicamente en tu
   dispositivo (DataStore Preferences de Android). Puedes borrarlos
   desinstalando la app. La copia de seguridad local la gestiona Android
   según la configuración del dispositivo.

3. Servicios de terceros
   No hay analítica, publicidad, redes sociales ni proveedores de
   telemetría.

4. Compras
   No hay compras in-app ni anuncios. Los créditos del juego se ganan
   jugando y no tienen valor real.

5. Menores de edad
   La app no está dirigida de forma específica a menores y no solicita
   datos de ningún usuario.

6. Contacto
   Correo del desarrollador: [CORREO DE CONTACTO]

7. Cambios
   Cualquier cambio de esta política se publicará en esta misma dirección
   con su fecha de actualización.
```

## 7. Pasos en Play Console (G05)

1. Cuenta personal verificada (25 USD, ya pendiente de pago).
2. Crear app → tipo **Juego** → gratis (sin IAP ni anuncios).
3. Subir el **AAB** a la pista **prueba cerrada**.
4. Activar **Play App Signing** (Play guarda la clave; la de subida es la
   nuestra: SHA-256 `3af521aaa8202a90da5cbc4169e1ef3ca8fec0dc4d21aecd421976325a5aeeaf`).
5. Ficha: textos del §3, assets del §4 (los 3 obligatorios: icono, gráfico de
   funciones, capturas), categoría Juegos → Puzzle.
6. Cuestionario de clasificación por edades (sin violencia, sexo, lenguaje,
   ni interacción online → esperable PEGI 3 / ESRB Everyone).
7. Hoja de seguridad de datos del §5.
8. Declaraciones: sin anuncios, sin IAP, sin cuentas.
9. **Prueba cerrada**: cuenta creada después del 13-nov-2023 exige
   **12 probadores continuos durante 14 días** antes de pedir producción.
10. Tras los 14 días: solicitar paso a producción (revisión ~7 días).

## 8. Checklist de publicación

- [ ] `./gradlew :game-domain:test :app:testDebugUnitTest :app:lintRelease` en verde.
- [ ] AAB y APK regenerados y hashes actualizados en §2.
- [ ] Textos revisados en §3 (ES y EN).
- [ ] Assets del §4 exportados y validados (sin alfa en el icono).
- [ ] AAB subido a prueba cerrada; los 12 probadores aceptados el día 1.
- [ ] Fecha del día 1 anotada en `docs/ROADMAP.md` (G05).
- [ ] Tras 14 días: solicitud de producción + fecha.
- [ ] Publicación: `v0.1.0` etiquetado en git y release notes mínimos.

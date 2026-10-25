# Changelog

Historial de versiones de **Mainboard Override**. Formato basado en los prefijos
de commits del repo (`feat:`, `fix:`, `perf:`, `chore:`, `docs:`).

## 0.1.2 (versionCode 14) — sin publicar

Versión en desarrollo; sustituye al 0.1.1 (versionCode 13), que nunca llegó a
publicarse. Rango: `v0.1.0` (31cc800) .. `5716cf9` (72 commits).

### Añadido

- `feat:` scripts **CLOAK** (congela el rastreo de la base) y **STUN** (frena al
  daemon), con sprites de carta, arte de fusión y documentación de prompts.
- `feat:` progresión de scripts por banda de progreso de campaña; el conjunto
  desbloqueado se expone a la UI.
- `feat:` **tienda de skins**: renombrada desde galería con badge gratis,
  propiedad de skins con espacios de nombres, compras con créditos de dominó,
  catálogo por categoría con reglas en el dominio, chips de filtro en la
  cabecera, tarjetas compactas y ventana unificada para menú y ajustes.
- `feat:` arte procedural del trío techno, skin nebula y **FX premium animados**
  (configuración + tests).
- `feat:` campaña ampliada a **100 desafíos** con tableros más anchos; nodos de
  datos dibujados en el tablero y contados en el HUD; la victoria exige barrerlos.
- `feat:` franja de progreso de tutorial persistente con salto de lección.
- `feat:` animaciones de tablero, contadores del HUD y feedback de rechazo.
- `feat:` página de política de privacidad publicada en la landing.
- `feat:` sonido electrónico más rico y tick de UI más discreto.
- `feat:` flag debug de desbloqueo total para desafíos y tienda.

### Cambiado

- `perf:` **peso del APK de 56,5 MB a 30,7 MB** en debug (release: 18,7 MB).
  Arte estático convertido de PNG a **WebP** (pipeline completo sobre WebP),
  skins lossy a resolución nativa, tiles de dominó cacheados y FX más ligeros.
- `perf:` pasada de rendimiento P0–P3: sin motas ambientales en modo libre,
  el ping ya no recompona la partida entera, los relojes idle se detienen con
  movimiento reducido, el pulso del circuito se pausa fuera de primer plano,
  los FX premium respetan movimiento reducido y la decodificación WebP de la
  tienda se aplaza del primer frame.
- `feat:` tablero más ancho a costa del panel de controles.
- `feat:` diálogo SPOOF rediseñado con chevrons de borde; sweep premium de
  skins atenuado; previews de tienda más ligeras.
- `chore:` nginx endurecido con cabeceras de seguridad (paridad con Serpy).

### Arreglado

- `fix:` STEALTH fuera de la lista de ayuda general; tick final de CLOAK/STUN
  fijado.
- `fix:` mezclador SFX y banda sonora autorecuperables; escena MENÚ realzada.
- `fix:` pips sobre las previews de dominó de la tienda; los FX respetan la
  silueta de la ficha.
- `fix:` puerta de skins de dominó y reparto de precios de la tienda.
- `fix:` pantalla de ajustes desplazable con toggles apilados.
- `fix:` tests de overlay de escenario anclados a los assets regenerados 1280×800.

### Documentación

- `docs:` specs y planes: tienda de skins, scripts progresivos (SDD), pasada
  de rendimiento; contratos actualizados en GDD y arquitectura; contadores de
  tests sincronizados (dominio: 72); gotchas de tests instrumentados en
  AGENTS.md; distinción entre release publicada y versión en desarrollo.

## 0.1.0 (versionCode 12) — publicada 2026-10-03

Primera release firmada (etiqueta `v0.1.0`). Detalle en el histórico de
commits hasta `31cc800` y en `docs/PLAY.md`.

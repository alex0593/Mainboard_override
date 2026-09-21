# Futuro — Mainboard Override

Ideas y alcances diferidos durante los incrementos A–E, guardados para no
perderse. Nada de aquí está aprobado para implementación: cada punto debe
pasar por spec acordado antes de codificarse, como los incrementos E.
El banco original de ideas vive en [IMPROVEMENTS.md](IMPROVEMENTS.md).

**Revisión:** 2026-09-21.

## Scripts y amenazas (tras E01-lock)

- Scripts nuevos: stealth/VPN, ocultar rastreo temporalmente, dañar o mover el
  daemon, duplicar puerto temporalmente, eliminar o alterar puentes.
- Descarte o reorganización del mazo de scripts en algunas partidas.
- Mejoras de script desbloqueables: menor coste, menor ruido, carta extra,
  efecto secundario controlado.
- Nuevas amenazas u objetivos: routers, células inestables, zonas de rastreo
  acelerado, más variantes de lock.
- Daemon con comportamiento más diverso: huida, avance doble condicional,
  detección temprana.

## Retos y generación (tras E02)

- Geometrías nuevas: tableros con huecos, columnas asimétricas, juntas
  irregulares (exige subir `REVISION` de previsualizaciones).
- Catálogo de retos con más restricciones: caps de RAM, manos fijas,
  combinaciones de reglas por desafío.
- Reajuste de generación solo si un futuro informe de equilibrio lo pide;
  el vigente está en `BalanceTest` (D02).

## Progresión y metas (tras E05/E06)

- Metas semanales (la diaria ya existe: +10 a la primera victoria del día).
- Inventario o backpack para partidas largas, con objetos o mejoras ligeras.
- Más logros, insignias o premios parciales por estilo de juego.
- Fases de campaña más profundas si el contenido lo pide.

## Sonido, VFX y atmósfera (tras E04)

- Glitch visual sostenido al superar 80 % de rastreo y estática ambiental.
- Partículas adicionales si los efectos actuales se quedan cortos.
- Cues de sonido nuevas solo para eventos nuevos.

## Accesibilidad pendiente

- Revisión manual con TalkBack y lector en mano (la evidencia actual es
  automatizada: roles, targets ≥48dp, fuente 1.3x y contraste AA).
- Pantallas pequeñas y horizontal invertido sin verificación física.

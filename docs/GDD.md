# Mainboard Override — GDD y seguimiento

## Progresión local

La campaña contiene 30 desafíos secuenciales en tres fases temáticas: Red local (1–10), Enrutamiento (11–20) y Sobreescritura de hardware (21–30). Completar 5, 10, 15, 20, 25 y 30 desafíos distintos desbloquea respectivamente Laboratorio, Centro de datos, Red industrial, Archivo profundo, Núcleo blindado y Red fantasma en modo libre. La red original está disponible desde el inicio. Los escenarios modifican dimensiones, longitud de ruta y densidad de obstáculos; cada generación comprueba una solución con el motor.

Una victoria otorga 20 créditos y la primera victoria de un desafío añade 40. Derrotas y abandonos no otorgan créditos. La primera victoria de cada día natural suma 10 créditos extra, una sola vez. Hay cuatro logros sin recompensa económica: primera victoria, cinco desafíos distintos, completar un escenario libre y ganar con rastreo 40 o menos; se conceden una sola vez y se muestran en el resumen y en la pantalla de desafíos. La puntuación de una victoria es 1000 − turnos×40 − rastreo (mínimo 0); a menos turnos y rastreo, más puntos, y el desempate de récords sigue ese mismo orden. Se muestra en el resumen y en el detalle de cada desafío. Los desafíos 7, 17 y 27 vetan SPOOF: ese script nunca se reparte en ellos (se avisa en el detalle previo). Cobre, Aurora, Titanio, Jade y Rubí son PCB cosméticas de compra única, por 200 créditos cada una; grafito y señal cuestan 80. Las fichas de dominó no tienen coste. Los escenarios solo se desbloquean jugando desafíos. No hay créditos retroactivos; los récords existentes sí cuentan para los desbloqueos.

Al finalizar se muestra el desglose y se puede revisar el tablero sin controles de juego, regresar al resumen, reintentar o continuar al siguiente desafío. La galería separa fichas y PCB con vistas previas y estados de equipamiento/compra. El menú anima pulsos de circuitos y respeta movimiento reducido.

Durante la partida los scripts se muestran como cartas compactas en una banda superior, junto a RAM, turno y rastreo. El botón EJECUTAR TURNO conserva una posición estable en el panel de controles y se remarca cuando se intenta colocar una segunda ficha antes de resolver el turno. SPOOF presenta un selector vertical desplazable de valores 0–6 para el puerto elegido.

Los escenarios libres incluyen dos recogidas de tablero: un disipador que resta 8 al rastreo y una reserva que suma 1 RAM. Cada casilla se consume una sola vez y no puede superar los límites de los recursos. Cada mapa libre genera además un lock visible junto a la ruta de referencia: exige ese valor exacto al contactar; acertar lo consume, cubrir su celda con el valor exacto también lo consume, cualquier otro contacto o cobertura derrota al instante (`LOCK_TRIPPED`) y KILL puede eliminarlo. Los desafíos no generan locks. Las cartas y los iconos tienen la guía visual en [CARTAS.md](CARTAS.md).

## Concepto

Juego móvil de lógica táctica. El jugador es un analista que se infiltra en un sistema cerrado: construye un circuito con fichas de dominó y usa cartas que representan comandos para alterar el tablero. El objetivo es conectar el nodo de inicio con la extracción antes de ser localizado.

## Vertical slice actual

### Bucle y tablero

1. Se repone la mano hasta tres dominós, se roba un script si la mano no llega a los cupos del turno y la RAM vuelve a 3.
2. El jugador puede ejecutar tantos scripts como pueda pagar y debe colocar exactamente un dominó.
3. El sistema suma 8 de rastreo más el ruido, activa trampas y mueve el daemon.
4. Se comprueban primero las derrotas y después la conexión de victoria.

El modo libre tiene 6 filas: Red original 8 columnas, Laboratorio y Centro de datos 9, Red industrial y Archivo profundo 10, Núcleo blindado y Red fantasma 11. Los desafíos conservan su geometría anterior. Los puertos S0 (inicio) y X6 (extracción) se dibujan fuera de los bordes izquierdo y derecho; no ocupan celdas de la cuadrícula. Cada dominó ocupa dos celdas ortogonales; sus mitades están conectadas internamente, pero todo contacto externo debe compartir valor. Las redes pueden ramificarse y cada semilla contiene una ruta solucionable. Las semillas antiguas del modo libre generan mapas distintos tras este cambio.

### Scripts incluidos

| Script | RAM | Ruido | Efecto actual |
|---|---:|---:|---|
| `PING` | 1 | 0 | Revela un honeypot oculto sin activar al azar y amplía la vista previa en una ficha por uso, sin robarla. |
| `SPOOF` | 2 | 5 | Reescribe permanentemente un puerto de una ficha en mano. |
| `KILL_PROCESS` | 3 | 15 | Elimina un firewall, un honeypot visible, un lock o una ficha colocada completa al seleccionar cualquiera de sus dos celdas. Puede eliminarlo en el mismo turno de PING si alcanza la RAM. |
| `BRIDGE` | 2 | 10 | Copia un puerto de una ficha adyacente a través del firewall; la salida queda libre para colocar una ficha compatible. |
| `STEALTH` | 1 | 0 | Descarta todo el ruido pendiente del turno antes de que se sume al rastreo; sin ruido pendiente se rechaza sin gastar carta ni RAM. |

Las derrotas posibles son rastreo al 100 %, daemon en el inicio, lock contactado con valor erróneo, kernel panic sin acciones habilitables o bolsa de hardware agotada. BRIDGE y KILL_PROCESS pueden evitar kernel panic si abren una colocación legal. La victoria requiere una ruta continua hasta extracción después de resolver la fase del sistema.

La mano de scripts arranca con un único cupo y abre uno más cada dos turnos: los turnos 1-2 sostienen un script, los turnos 3-4 dos, los turnos 5-6 tres, y así sucesivamente. Al ejecutar el turno se roba la primera carta del mazo solo si la mano está por debajo del cupo del turno nuevo y queda existencias, de modo que el tope crece lento pero nunca te quedas sin script por haber gastado el tuyo.

PING conserva descubrimientos anteriores y selecciona entre trampas ocultas sin activar con azar reproducible por semilla y carta. La vista de la siguiente ficha se limpia al terminar el turno. Cada uso amplía la vista previa en una ficha y revela otra trampa disponible. Si no queda información nueva, rechaza el uso sin gastar carta ni RAM. No hay límite de scripts por turno aparte de las cartas disponibles, sus objetivos válidos y la RAM.

BRIDGE requiere al menos una ficha junto al firewall en el eje seleccionado. Si ambos puertos existen, deben coincidir. Rechaza otra pared en la salida, extremos fuera del mapa y puentes duplicados sin gastar recursos. Conserva el firewall, guarda y muestra el número copiado y solo conecta extremos ocupados compatibles. KILL_PROCESS elimina también el puente del firewall destruido y cualquier lock visible. Puede destruir un honeypot visible incluso si PING lo acaba de revelar, siempre que alcance la RAM. Los honeypots ocultos no son objetivos válidos. También puede eliminar una ficha colocada completa apuntando a cualquiera de sus mitades; la ficha deja de formar parte de la red y los recursos ya consumidos no se reembolsan. Destruir una trampa elimina su presencia, revelado y registro de activación, sin reembolsar ruido ni cartas. Mantiene el coste de 3 RAM y 15 de ruido.

STEALTH se juega sin objetivo, como PING: descarta el ruido pendiente completo del turno, de modo que al ejecutar el turno el rastreo solo suma su base de 8. Cuesta 1 RAM y no añade ruido propio. Su jugada natural es combinarse en el mismo turno con SPOOF (+5), BRIDGE (+10) o una trampa de honeypot (+20) dentro del tope de 3 RAM; KILL ya gasta las 3 RAM, así que su ruido de 15 no admite combinación. Si no hay ruido pendiente se rechaza con mensaje propio sin consumir carta ni RAM. No abre colocaciones, así que no puede evitar kernel panic.

## Qué se lleva implementado

- [x] Proyecto Android modular con Kotlin y Jetpack Compose.
- [x] Motor puro con estado inmutable y acciones/rechazos explícitos.
- [x] Colocación, cuatro rotaciones, ramificación y búsqueda de conectividad.
- [x] Semillas reproducibles con ruta garantizada.
- [x] Cinco scripts (PING, SPOOF, KILL, BRIDGE, STEALTH), RAM renovable, ruido, rastreo y amenazas.
- [x] Menú, ayuda, ajustes, tablero, mano y resultado en horizontal.
- [x] Textos en español e inglés.
- [x] Persistencia de ajustes, última semilla y mejor resultado.
- [x] Pruebas unitarias de reglas esenciales y una partida completa.
- [x] Wrapper Gradle reproducible y APK de depuración compilado con Android API 37.
- [x] Icono adaptativo con foreground exportado del logo (`logo_foreground.png`), fondo `#07110F`, capa monocroma y splash propio.

## Qué se tiene que cambiar o completar

### Para cerrar el vertical slice

- [x] Conectar audio original, vibración y VFX a los ajustes ya persistidos. (Hecho: 16 cues, volúmenes, silencio y movimiento reducido; vibración cableada en `HapticsHost`; partículas de recogida y flash al 80 % de rastreo en E04. Glitch sostenido y estática quedan como atmósfera futura.)
- [ ] Añadir arrastre real; la interacción accesible por selección y pulsación ya está implementada.
- [x] Elegir visualmente orientación de `BRIDGE` y ambas mitades de `SPOOF`, con previsualización y cancelación.
- [x] Ayuda contextual con «?» durante la partida.
- [x] Fichas claras de Kenney con puntos en mano, tablero y vistas previas.
- [ ] Completar validación manual de TalkBack, texto ampliado y pantallas pequeñas.
- [x] Añadir pruebas instrumentadas Compose de los controles de scripts.
- [ ] Añadir pruebas masivas de semillas y perfiles en dispositivos reales.
- [ ] Sustituir símbolos procedurales por arte y sonido originales de fidelidad final ligera.

### Después del vertical slice

- [ ] Campaña con contenido propio: las fases Red local, Enrutamiento y Sobreescritura ya existen como etiquetas (E05); falta contenido diferenciado por fase.
- [ ] Fragmentos de datos, inventario, deckbuilding y mejoras de scripts.
- [x] Script STEALTH (F02): descarta el ruido pendiente del turno por 1 RAM. Quedan VPN y mayor variedad de amenazas y objetivos.
- [ ] Room para campaña e inventario y guardado de partida activa.
- [ ] Temas Kali-Strike, Ubun-Core, Deb-Server, Arch-Elite y Retro-DOS.

## Cambios respecto a la idea inicial

- La “Energía/RAM” queda definida como RAM renovable de 3 por turno.
- `SPOOF` persiste para evitar que una conexión válida se rompa al terminar el turno.
- `KILL_PROCESS` elimina firewalls, honeypots visibles y fichas completas seleccionando cualquiera de sus celdas.
- El mazo de scripts vacío no derrota por sí mismo; memoria agotada usa la bolsa de hardware.
- DataStore sustituye temporalmente a Room porque el slice solo guarda valores simples.
- Se prioriza una sola orientación horizontal y no se reanuda una partida tras cerrar el proceso.

## Dirección audiovisual

Neobrutalismo de sistema operativo: PCB oscuro, verde terminal, cian, amarillo de advertencia, rojo de alerta, paneles duros y tipografía monoespaciada. La versión final ligera añade flujo luminoso, glitch al superar 80 % de rastreo, teclado mecánico, estática y ambiente synthwave. La vibración ya responde a colocaciones, scripts, trampas y resultados según el ajuste persistido; las cues informativas (PING, turno, arranque, compra) no vibran. Las recogidas emiten un destello cian y cruzar 80 de rastreo dispara un flash rojo único (ambos finitos y compatibles con movimiento reducido).

## Verificación técnica

- `:game-domain:test`: correcto (51 pruebas, 0 fallos); pruebas del motor, los 30 desafíos, barrido de 1000 semillas libres y 700 combinaciones de escenario/semilla.
- `:app:assembleDebug`: correcto; APK generado en `app/build/outputs/apk/debug/`.
- `:app:connectedDebugAndroidTest`: correcto en CLK-LX3 con Android 14 (28 pruebas en 8 clases), incluidas progresión, compras (incluidas las skins premium nuevas), revisión del tablero, ayuda, tutorial, accesibilidad, recogidas y movimiento reducido.
- `:app:lintDebug`: correcto; 0 errores, 61 advertencias y 2 notas (versiones y dependencias, orientación, candidatos a plurales, recursos sin uso, tipografía y APIs disuadidas). Sin baselines ni supresiones nuevas.
- Evidencia y límites de la validación: los informes de Gradle en `app/build/reports/`.


## Mejoras de controles

- Ayuda contextual con objetivos, resultado visible de cada script y reinicio de red.
- PING muestra su vista de fichas; BRIDGE se dibuja en el tablero; SPOOF permite cancelar sin coste.
- Orientación y orden de puertos visibles, destinos legales ocultos tras colocar y soporte de fichas dobles en las cuatro rotaciones.
- Errores y resultados traducidos, ayuda dentro de partida, paneles desplazables y semántica accesible sin revelar trampas ocultas.
- Posición explícita de cada celda para evitar superposición en el origen del tablero.

# Mainboard Override — GDD y seguimiento

## Progresión local

La campaña contiene 30 desafíos secuenciales. Completar 5, 10, 15, 20, 25 y 30 desafíos distintos desbloquea respectivamente Laboratorio, Centro de datos, Red industrial, Archivo profundo, Núcleo blindado y Red fantasma en modo libre. La red original está disponible desde el inicio. Los escenarios modifican dimensiones, longitud de ruta y densidad de obstáculos; cada generación comprueba una solución con el motor.

Cada victoria concede 20 créditos y la primera victoria de un desafío añade 40. Derrotas y abandonos no otorgan créditos. Cobre y Aurora son PCB cosméticas de compra única, por 200 créditos cada una. Los escenarios solo se desbloquean jugando desafíos. No hay créditos retroactivos; los récords existentes sí cuentan para los desbloqueos.

Al finalizar se muestra el desglose y se puede revisar el tablero sin controles de juego, regresar al resumen, reintentar o continuar al siguiente desafío. La galería separa fichas y PCB con vistas previas y estados de equipamiento/compra. El menú anima pulsos de circuitos y respeta movimiento reducido.

Durante la partida los scripts se muestran como cartas compactas en una banda superior, junto a RAM, turno y rastreo. El botón EJECUTAR TURNO conserva una posición estable en el panel de controles y se remarca cuando se intenta colocar una segunda ficha antes de resolver el turno. SPOOF presenta un selector vertical desplazable de valores 0–6 para el puerto elegido.

Los escenarios libres incluyen dos recogidas de tablero: un disipador que resta 8 al rastreo y una reserva que suma 1 RAM. Cada casilla se consume una sola vez y no puede superar los límites de los recursos. Las cartas y los iconos tienen la guía visual en [CARTS spesificationst.md](../CARTS%20spesificationst.md).

## Concepto

Juego móvil de lógica táctica. El jugador es un analista que se infiltra en un sistema cerrado: construye un circuito con fichas de dominó y usa cartas que representan comandos para alterar el tablero. El objetivo es conectar el nodo de inicio con la extracción antes de ser localizado.

## Vertical slice actual

### Bucle y tablero

1. Se repone la mano hasta tres dominós, se roba un script y la RAM vuelve a 3.
2. El jugador puede ejecutar tantos scripts como pueda pagar y debe colocar exactamente un dominó.
3. El sistema suma 8 de rastreo más el ruido, activa trampas y mueve el daemon.
4. Se comprueban primero las derrotas y después la conexión de victoria.

El tablero varía entre 8 y 10 columnas y entre 6 y 7 filas, con inicio 0 y extracción 6. Cada dominó ocupa dos celdas ortogonales; sus mitades están conectadas internamente, pero todo contacto externo debe compartir valor. Las redes pueden ramificarse y cada semilla contiene una ruta solucionable.

### Scripts incluidos

| Script | RAM | Ruido | Efecto actual |
|---|---:|---:|---|
| `PING` | 1 | 0 | Revela honeypots y las tres fichas siguientes. |
| `SPOOF` | 2 | 5 | Reescribe permanentemente un puerto de una ficha en mano. |
| `KILL_PROCESS` | 3 | 15 | Elimina el firewall seleccionado. |
| `BRIDGE` | 2 | 10 | Conecta valores iguales a ambos lados de un firewall. |

Las derrotas posibles son rastreo al 100 %, daemon en el inicio, kernel panic sin acciones habilitables o bolsa de hardware agotada. La victoria requiere una ruta continua hasta extracción después de resolver la fase del sistema.

## Qué se lleva implementado

- [x] Proyecto Android modular con Kotlin y Jetpack Compose.
- [x] Motor puro con estado inmutable y acciones/rechazos explícitos.
- [x] Colocación, cuatro rotaciones, ramificación y búsqueda de conectividad.
- [x] Semillas reproducibles con ruta garantizada.
- [x] Cuatro scripts, RAM renovable, ruido, rastreo y amenazas.
- [x] Menú, ayuda, ajustes, tablero, mano y resultado en horizontal.
- [x] Textos en español e inglés.
- [x] Persistencia de ajustes, última semilla y mejor resultado.
- [x] Pruebas unitarias de reglas esenciales y una partida completa.
- [x] Wrapper Gradle reproducible y APK de depuración compilado con Android API 37.

## Qué se tiene que cambiar o completar

### Para cerrar el vertical slice

- [ ] Conectar audio original, vibración y VFX a los ajustes ya persistidos.
- [ ] Añadir arrastre real; la interacción accesible por selección y pulsación ya está implementada.
- [x] Elegir visualmente orientación de `BRIDGE` y ambas mitades de `SPOOF`, con previsualización y cancelación.
- [x] Ayuda contextual con «?» durante la partida.
- [x] Fichas claras de Kenney con puntos en mano, tablero y vistas previas.
- [ ] Completar validación manual de TalkBack, texto ampliado y pantallas pequeñas.
- [x] Añadir pruebas instrumentadas Compose de los controles de scripts.
- [ ] Añadir pruebas masivas de semillas y perfiles en dispositivos reales.
- [ ] Sustituir símbolos procedurales por arte y sonido originales de fidelidad final ligera.

### Después del vertical slice

- [ ] Campaña: red local, enrutamiento/DNS y sobreescritura de hardware.
- [ ] Fragmentos de datos, inventario, deckbuilding y mejoras de scripts.
- [ ] Scripts Stealth/VPN y mayor variedad de amenazas y objetivos.
- [ ] Room para campaña e inventario y guardado de partida activa.
- [ ] Temas Kali-Strike, Ubun-Core, Deb-Server, Arch-Elite y Retro-DOS.

## Cambios respecto a la idea inicial

- La “Energía/RAM” queda definida como RAM renovable de 3 por turno.
- `SPOOF` persiste para evitar que una conexión válida se rompa al terminar el turno.
- `KILL_PROCESS` elimina firewalls; eliminar dominós se pospone hasta definir componentes desconectados.
- El mazo de scripts vacío no derrota por sí mismo; memoria agotada usa la bolsa de hardware.
- DataStore sustituye temporalmente a Room porque el slice solo guarda valores simples.
- Se prioriza una sola orientación horizontal y no se reanuda una partida tras cerrar el proceso.

## Dirección audiovisual

Neobrutalismo de sistema operativo: PCB oscuro, verde terminal, cian, amarillo de advertencia, rojo de alerta, paneles duros y tipografía monoespaciada. La versión final ligera añadirá flujo luminoso, glitch al superar 80 % de rastreo, hápticos, teclado mecánico, estática y ambiente synthwave.

## Verificación técnica

- `:game-domain:test`: correcto; pruebas del motor, los 30 desafíos y 600 combinaciones de escenario/semilla.
- `:app:assembleDebug`: correcto; APK generado en `app/build/outputs/apk/debug/`.
- `:app:connectedDebugAndroidTest`: pruebas correctas en CLK-LX3 con Android 14, incluidas progresión, compras, revisión del tablero, ayuda y movimiento reducido.
- `:app:lintDebug`: correcto; 0 errores y 18 advertencias (versiones, orientación, candidatos a plurales y recursos sin uso). Sin baselines ni supresiones nuevas.
- Evidencia y límites de la validación: los informes de Gradle en `app/build/reports/`.


## Mejoras de controles

- Ayuda contextual con objetivos, resultado visible de cada script y reinicio de red.
- PING muestra su vista de fichas; BRIDGE se dibuja en el tablero; SPOOF permite cancelar sin coste.
- Orientación y orden de puertos visibles, destinos legales ocultos tras colocar y soporte de fichas dobles en las cuatro rotaciones.
- Errores y resultados traducidos, ayuda dentro de partida, paneles desplazables y semántica accesible sin revelar trampas ocultas.
- Posición explícita de cada celda para evitar superposición en el origen del tablero.

# Mainboard Override — GDD y seguimiento

## Visión

Juego móvil de lógica táctica. El jugador es un analista que se infiltra en un sistema cerrado: construye un circuito con fichas de dominó y usa cartas que representan comandos para alterar el tablero. El objetivo es conectar el nodo de inicio con la extracción antes de ser localizado.

## Vertical slice actual

### Bucle y tablero

1. Se repone la mano hasta tres dominós, se roba un script y la RAM vuelve a 3.
2. El jugador puede ejecutar tantos scripts como pueda pagar y debe colocar exactamente un dominó.
3. El sistema suma 8 de rastreo más el ruido, activa trampas y mueve el daemon.
4. Se comprueban primero las derrotas y después la conexión de victoria.

El tablero es una cuadrícula de 9×7 con inicio 0 y extracción 6. Cada dominó ocupa dos celdas ortogonales; sus mitades están conectadas internamente, pero todo contacto externo debe compartir valor. Las redes pueden ramificarse y cada semilla contiene una ruta solucionable.

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
- [x] Semillas reproducibles con ruta garantizada y semilla tutorial.
- [x] Cuatro scripts, RAM renovable, ruido, rastreo y amenazas.
- [x] Menú, ayuda, ajustes, tablero, mano y resultado en horizontal.
- [x] Textos en español e inglés.
- [x] Persistencia de ajustes, tutorial, última semilla y mejor resultado.
- [x] Pruebas unitarias de reglas esenciales y una partida completa.
- [x] Wrapper Gradle reproducible y APK de depuración compilado con Android API 37.

## Qué se tiene que cambiar o completar

### Para cerrar el vertical slice

- [ ] Conectar audio original, vibración y VFX a los ajustes ya persistidos.
- [ ] Añadir arrastre real; la interacción accesible por selección y pulsación ya está implementada.
- [x] Elegir visualmente orientación de `BRIDGE` y ambas mitades de `SPOOF`, con previsualización y cancelación.
- [x] Ayuda contextual con «?» durante la partida; sustituye el tutorial del menú. Sus ejercicios deterministas se conservan para pruebas; ver [guía completa](TUTORIAL.md).
- [x] Fichas claras de Kenney con puntos en mano, tablero y vistas previas.
- [ ] Completar validación manual de TalkBack, texto ampliado y pantallas pequeñas.
- [x] Añadir pruebas instrumentadas Compose del tutorial y los controles de scripts.
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

- `:game-domain:test`: correcto; 12 pruebas ejecutadas (7 del motor y 5 del tutorial).
- `:app:assembleDebug`: correcto; APK generado en `app/build/outputs/apk/debug/`.
- `:app:connectedDebugAndroidTest`: 6 pruebas correctas en CLK-LX3 con Android 14, incluidos español/inglés y texto ampliado.
- `:app:lintDebug`: correcto; 0 errores y 3 advertencias preexistentes sobre target SDK, Gradle y orientación fija. Sin baselines ni supresiones nuevas.
- Evidencia y límites de la validación: [TUTORIAL.md](TUTORIAL.md#resultado-de-esta-entrega-7-de-septiembre-de-2026).


## Mejoras del tutorial y controles

- Guía con objetivos comprobados por el motor, resultado visible de cada script y reinicio por lección.
- PING muestra su vista de fichas; BRIDGE se dibuja en el tablero; SPOOF permite cancelar sin coste.
- Orientación y orden de puertos visibles, destinos legales ocultos tras colocar y soporte de fichas dobles en las cuatro rotaciones.
- Errores y resultados traducidos, ayuda dentro de partida, paneles desplazables y semántica accesible sin revelar trampas ocultas.
- Posición explícita de cada celda para evitar superposición en el origen del tablero.
- Finalización tutorial separada de récords y última semilla normales; sin cambios de reglas ni migración de preferencias.

# Ideas de mejora — Mainboard Override

El orden de ejecución y los criterios de cierre viven en
[ROADMAP.md](ROADMAP.md#4-plan-operativo-y-tareas). Este banco conserva opciones
de producto; su inclusión no implica que estén aprobadas para implementación.

## 1. Jugabilidad y reglas

- Añadir un modo de dificultad escalable para escenarios libres, por ejemplo configurar ruta/firewalls/traps extra sin crear un nuevo escenario.
- Permitir una opción “modo puntuación” con objetivo de rastreo mínimo, turnos mínimos o ambas, para partidas repetibles.
- Hacer que el daemon tenga comportamiento más diverso: por ejemplo huida, avance doble en condiciones especiales o detección temprana.
- Añadir un nuevo tipo de amenaza o objetivo: locks, routers, células inestables, zonas de rastreo acelerado.
- Ampliar el uso de buffs: efectos de un solo uso más variados (reducción de ruido, valor libre, protección de celda, racha de trazas).
- Permitir jugar dominós en “modo práctica” con solución visible o validación paso a paso fuera del tutorial oficial.
- Introducir partidas de tiempo limitado o racha diaria con metas claras.
- Añadir combinaciones de reglas en desafíos para crear melodías de tensión distintas (por ejemplo ruta corta + daemon lento, o tracción rápida + firewall alto).

## 2. Scripts y descarte

- Añadir scripts nuevos con roles complementarios: ocultar rastreo, mover/dañar daemon, duplicar puerto temporalmente, eliminar o alterar puentes, etc.
- Permitir descarte o reorganización de mazo de scripts en algunas partidas para dar variedad.
- Ofrecer upgrades o mejoras de script desbloqueables: menor coste, menor ruido, carta extra, efecto secundario.
- Permitir prácticas de script específico, por ejemplo un modo de entrenamiento para KILL o BRIDGE.
- Añadir feedback más explícito sobre qué celda destruye KILL cuando apunta a una ficha completa.
- Hacer que PING muestre más contexto: cuántas trampas quedan ocultas, si la próxima carta puede cambiar la jugada, o una previsualización de los próximos pasos si se juega de forma óptima.

## 3. Generación y desafíos

- Mejorar la generación de semillas para que haya más variedad y menos repetición perceptible.
- Añadir validación de equilibrio: detectar niveles demasiado fáciles, demasiado ruidosos o muy dependientes de un solo script.
- Permitir crear desafíos custom o compartir them mediante semilla y reglas en texto.
- Añadir catálogo de retos con restricciones de mano, por ejemplo “sin SPOOF”, “solo BRIDGE”, “max 2 RAM”.
- Ofrecer un “generador de semillas amigables” para partidas más didácticas.
- Añadir un modo de validación de ruta más estricto o más laxo según el propósito del nivel.
- Añadir escenarios con geometrías distintas: tableros con huecos, juntas irregulares o columnas asimétricas.

## 4. UI/UX

- Reducir la concentración de composables en pantallas grandes extrayendo piezas reutilizables: header, panel de controles, indicadores de recursos.
- Mejorar accesibilidad: TalkBack, fuentes ampliadas, apoyo en pantallas pequeñas, descripciones de celdas más claras.
- Añadir arrastre real además de selección/tap.
- Mejorar feedback visual de acciones: animaciones de colocación, ruido, rastreo, daemon y efectos de script.
- Añadir modos de color o contraste para jugadores con necesidades visuales.
- Ofrecer una vista de “cambios pendientes” antes de ejecutar turno.
- Mejorar la previsualización de puzzles: que la imagen se actualice cuando cambian las skins equipadas.
- Añadir un panel de ayuda contextual más modular que pueda moverse o cerrarse sin quitar el tablero.
- Unificar temas visuales de menú y partida para coherencia.

## 5. Progresión y economía

- Validar la economía ya implementada: compras, saldo, recompensas idempotentes y confirmación correcta en UI.
- Añadir sistema de logros, insignias o récords por estilo de juego, no solo por victoria.
- Ampliar progresión de campaña con fases temáticas más claras: red local, DNS/enrutamiento, sobreescritura de hardware, etc.
- Añadir un inventario o “backpack” para partidas largas, con objetos o mejoras ligeros.
- Permitir desbloquear premios parciales o desafíos secundarios si no se consigue victoria perfecta.
- Añadir modo libre con metas diarias/semanales y recompensas de participación.

## 6. Tutorial y onboarding

- Aumentar cobertura del tutorial: más práctica con scripts avanzados, errores comunes y lectura de tablero.
- Añadir lecciones cortas extra como “puntos de referencia” desbloqueables.
- Ofrecer práctica sandbox dentro del tutorial sin penalización.
- Mejorar el flujo entre lecciones, por ejemplo saltar al tema que el jugador quiere repasar.
- Añadir experiencia interactiva de reglas antes de la primera partida.

## 7. Sonido, VFX y atmosfera

- Conectar audio, vibración y VFX a los ajustes ya persistidos.
- Añadir indicios de glitch, estática, teclado mecánico o synthwave ligero para reforzar atmósfera.
- Mejorar feedback sonoro por tipo de evento: colocación, script, trampas, daemon, victoria/derrota.
- Añadir partículas sutiles para rastreo alto o recogida de buffs.

## 8. Calidad técnica y tests

- Aumentar cobertura de tests de dominio para reglas nuevas antes de añadirlas.
- Añadir pruebas instrumentadas más amplias para controles y diálogos.
- Añadir validaciones masivas de semillas para detectar niveles problemáticos.
- Mejorar la claridad de tests existentes y nombres descriptivos.
- Centralizar lógica de validación de UI para no depender de cálculos dispersos en composables.
- Prever invalidación de caché de previsualizaciones al cambiar geometría de generador o renderizado.

## 9. Contenido y merchandising

- Añadir más skins temáticas coherentes con la estética neobrutal/terminal.
- Añadir variantes de arte para tablero sin coste de compra y separar claramente skins de fichas y de placa.
- Ampliar textos de ayuda, ejemplos de jugadas y notas de diseño en GDD.
- Considerar un pequeño glosario de sistema para novatos.

## 10. Monetización — AdMob (decidido: se usará AdMob)

**Decisión del producto (2026-10-03): «usaré AdMob».** Evaluación honesta antes
de tocar código, porque hoy el proyecto afirma lo contrario en muchos sitios.

### Evaluación

- **Encaja, pero rompe promesas actuales.** La app hoy no pide `INTERNET`, es
  100 % offline y la política de privacidad publicada lo afirma («no incluye el
  permiso de red INTERNET… no hay analítica, publicidad…»). AdMob añade red, el
  SDK `play-services-ads` y la recogida del ID de publicidad (AAID), con
  consentimiento UMP obligatorio en la UE/EEE.
- **Coste real de integración:** cuenta de AdMob con perfil de pago (mín. de
  edad), permiso `INTERNET`, meta-data del App ID en el manifiesto, flujo UMP
  al arrancar, unidades de anuncio de prueba hasta aprobar la app, y textos de
  tienda/política **reecritos**. Publicar clics propios o tráfico inautérico
  suspende la cuenta.
- **Impacto en UX:** el bucle de juego es offline; los anuncios solo deben
  vivir fuera del tablero (menú, entre partidas, opcionalmente recompensada).
  Un banner permanente en 320dp de pantalla apretaría el layout y ensucia la
  estética neobrutal — se recomienda intersticial ligero o recompensado.
- **Ingresos esperados bajos al inicio** (base pequeña → eCPM bajo); es una
  apuesta de largo plazo, no una vía de monetización inmediata.

### Onda expansiva (checklist antes del primer commit con AdMob)

1. `AndroidManifest.xml`: permiso `INTERNET` + meta-data
   `com.google.android.gms.ads.APPLICATION_ID` (usar IDs de prueba en dev).
2. `app/build.gradle.kts`: `com.google.android.gms:play-services-ads` (+ UMP).
3. Inicialización `MobileAds.initialize` y consentimiento UMP al arrancar
   (degradando sin red: sin conexión no hay anuncio, el juego sigue igual).
4. **Textos que hay que corregir** (invariante: `values/` = `values-es/` y docs):
   - `docs/PLAY.md` §1 (permisos «sin INTERNET»), §5 (Data safety «no
     recoge» → AAID + IP del dispositivo recogidos por Google), §6 (política
     de privacidad: añadir AdMob y enlazar «Cómo usa Google la información
     de las apps»), frases «sin anuncios» (ES/EN) y checklist de declaraciones.
   - `web/privacidad.html` (publicada): reescribir §1 («no incluye
     INTERNET»), §3 («no hay publicidad») y §4 («no hay anuncios»).
   - `web/index.html` («Todo offline…»), `README.md` («offline»): matizar a
     «juego sin conexión; los anuncios requieren red».
   - Ficha de Play: casillas «contiene anuncios» y revisar clasificación
     PEGI/ESRB con anuncios.
5. Verificación: `./gradlew test`, `:app:lintDebug`, suite instrumental, prueba
   manual con IDs de prueba y con el móvil **sin conexión** (el bucle no puede
   romperse si no hay red).

Mientras ese checklist no se ejecute, el estado publicado sigue siendo
«sin anuncios, sin INTERNET» y es coherente en toda la documentación.

## Prioridades sugeridas

1. Resolver los fallos actuales de tests antes de ampliar reglas.
2. Aclarar progreso y validar la economía implementada.
3. Mejorar feedback visual y accesibilidad.
4. Añadir variedad de scripts/amenazas con reglas validadas.
5. Ampliar generación y catálogo de retos.
6. Completar sonido/VFX cuando estén listos los ajustes persistidos.

## Nota

Esta lista es un banco de ideas, no un orden de trabajo. Cada idea debe validarse con el GDD actual, los tests existentes y el estilo del proyecto.

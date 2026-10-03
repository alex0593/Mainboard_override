# Validación del build de release (0.1.0)

Fecha: 2026-10-03. Dispositivo: emulador `mo-release` (Android 15, API 35,
google_apis x86_64, KVM). JDK 17. Evidencia de G03 en `docs/ROADMAP.md`.

Artefacto probado: `app/build/outputs/apk/release/app-release.apk`
(`versionName 0.1.0`, `versionCode 12`, R8/minify activo, firmado con la clave
de subida).

- `./gradlew test`: BUILD SUCCESSFUL (dominio 52/52, app 8/8; nada de código
  cambió desde la última ejecución completa, tareas `UP-TO-DATE`).
- `./gradlew :app:lintRelease`: BUILD SUCCESSFUL — 0 errores, 63 advertencias.
- `adb install`: Success. La APK debug no coexistía por firma distinta (este
  emulador estaba limpio).
- Arranque de `MainActivity`, 16 s de estabilidad: proceso vivo (pid 2226) y
  **0 `FATAL EXCEPTION`** en logcat — verifica que R8 no rompe la carga de la
  app ni la navegación por reflexión/recursos recortados.
- Interacción: descartado el diálogo del sistema «Viewing full screen», menú
  principal renderizado completo (`title-0.1.0.png`) y acceso a la lista de
  desafíos con logros, Fase 1 y estados de desbloqueo (`challenges-0.1.0.png`).
  Sigue vivo, 0 excepciones tras la interacción.

Nota: el AAB (`app-release.aab`, 56.698.614 B, `6d0e9379…1387`) se firma con
la misma clave pero no es instalable directamente en un dispositivo; Play lo
procesa en su servidor. La verificación en hardware real (CLK-LX3) queda para
la primera instalación manual, igual que en incrementos anteriores.

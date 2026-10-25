# Release 0.1.2 (versionCode 14) — checklist de publicación

Preparado el 2026-10-25 desde `5716cf9` (`main`, 0 commits pendientes contra
`origin/main`). **Nada de este checklist se ha ejecutado aún: es el plan, no el
registro de un deploy.**

## 1. Estado verificado

- [x] `versionCode = 14` y `versionName = "0.1.2"` en `app/build.gradle.kts` (líneas 16-17).
- [x] `main` limpio y sincronizado con `origin/main` (0/0).
- [x] `keystore.properties` presente y gitignored; keystore fuera del repo
      (`~/.android/mainboard-override-upload.jks`, alias `mainboard-override`).
- [x] Sin secretos trackeados (`git ls-files` limpio de keystores/tokens).
- [x] `values-es/` al día (`strings.xml`, `tutorial.xml`).
- [x] Licencias de assets en `app/src/main/assets/licenses/` (3 ficheros).
- [x] APK debug instalada y probada en el Huawei CLK-LX3 (dispositivo real del usuario).
- [x] Suite instrumental: **37/39 passed**; los 2 fallos son flakes UI conocidos
      (`No compose hierarchies found`, pasan aislados; raíz: animaciones infinitas
      de la tienda — ver AGENTS.md).
- [x] Peso medido: debug **30,7 MiB** (32.163.654 B), release **18,7 MiB**
      (19.593.750 B), AAB 20,9 MiB (21.874.334 B).

## 2. Artefactos — ⚠️ regenerar antes de subir

Los binarios actuales en `app/build/outputs/` están fechados 21-oct 12:20/12:24,
**minutos antes** del último commit (`5716cf9`, 12:25). Regenerar desde el HEAD:

```sh
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew :app:assembleRelease :app:bundleRelease :app:lintRelease
sha256sum app/build/outputs/apk/release/app-release.apk app/build/outputs/bundle/release/app-release.aab
```

- [ ] AAB y APK regenerados desde `5716cf9` y hashes nuevos anotados aquí y en
      `docs/PLAY.md` §2 (referencia actual: APK `77eb7bba…`, AAB `768d010b…`).

## 3. Capturas (2–8, 1920×1080 horizontal, PNG)

Pantallas a capturar (una por pantalla principal):

1. Desafío en curso con el rastreo visible (nodos de datos en el tablero).
2. Tutorial con guías (franja de progreso).
3. **Tienda de skins** (antes galería) con chips de filtro y badge gratis.
4. Modo libre con escenario.
5. Pantalla de victoria con récord.
6. Diálogo SPOOF rediseñado / carta CLOAK o STUN en la mano (nuevo contenido).

```sh
adb exec-out screencap -p > pantalla.png
```

- [ ] Capturas tomadas del CLK-LX3 en landscape 2400×1080 y recortadas a 1920×1080.
- [ ] Icono 512×512 sin alfa y gráfico 1024×500 exportados (siguen marcados
      como pendientes en `docs/PLAY.md` §4 desde 0.1.0 — comprobar si ya existen).

## 4. Notas de versión

ES (para Play Console e itch.io, versión corta):

```
Actualización 0.1.2
- Dos scripts nuevos: CLOAK (congela el rastreo) y STUN (frena al daemon).
- Campaña ampliada a 100 desafíos con nodos de datos que barrer.
- Tienda de skins con FX premium animados y filtros.
- La app pesa mucho menos: el APK es un 45 % más ligero.
- Mejoras de rendimiento y soporte de movimiento reducido.
```

EN (opcional, para itch.io en inglés):

```
Update 0.1.2
- Two new scripts: CLOAK (freezes the trace) and STUN (stalls the daemon).
- Campaign expanded to 100 challenges with data nodes to sweep.
- Skin store with animated premium FX and filters.
- Much lighter app: the APK is 45 % smaller.
- Performance fixes and reduced-motion support.
```

- [ ] Textos pegados en Play Console (sección «Notas de la versión») e itch.io.

## 5. Play Store

- [ ] `./gradlew :game-domain:test :app:testDebugUnitTest :app:lintRelease` en verde
      (re-ejecutar desde `5716cf9`; la última suite conocida es la del punto 1).
- [ ] AAB subido a **prueba cerrada** con el versionCode 14 (incremental sobre el 12).
- [ ] Ficha actualizada: textos de `docs/PLAY.md` §3 si cambió contenido
      (los textos ya mencionan CLOAK/STUN y 100 desafíos — verificar).
- [ ] Data safety sin cambios (sin permisos nuevos; solo `VIBRATE`).
- [ ] Si ya se está en la fase de 14 días de prueba cerrada: la actualización
      se aplica a los probadores existentes sin reiniciar el contador.

## 6. itch.io

- [ ] APK de release regenerada (punto 2).
- [ ] `BUTLER_API_KEY=<key> ./tools/publish-itch.sh --release android`.
- [ ] Notas de versión actualizadas en el panel de itch.io.
- [ ] Enlace de descarga probado tras el push (butler reemplaza el canal).

## 7. Tras publicar (sitio de descarga directa)

- [ ] `./tools/deploy-site.sh --apk --duckdns` — publica APK + regenera
      SHA256SUMS + actualiza versión/hash/tamaño en `web/index.html`.
- [ ] Verificación pública: `curl -sI https://mainboard-override.duckdns.org/`
      y descarga del `latest.apk`.
- [ ] Tag `v0.1.2` en el commit de release (**solo con confirmación del usuario**).
- [ ] `docs/PLAY.md` §2 y §8 actualizados con los hashes definitivos.

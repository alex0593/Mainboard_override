# Tienda de skins — Diseño (2026-10-03)

> Objetivo: **dar uso a la economía** transformando la galería existente (`skins` →
> *Tienda*) en un catálogo con reparto nuevo de gratis/pago, compras de fichas y
> placas con posesión namespaced. `nebula` queda **aparte**: solo se cablean sus
> resources para cerrar el test en rojo, sin catálogo/strings/precio (entrará
> después con un tope nuevo por encima de 200). Sin IAP (solo créditos del
> juego), sin arte nuevo, sin audio nuevo.

## Decisiones tomadas con el usuario

| # | Decisión | Respuesta |
|---|----------|-----------|
| 1 | Reparto gratis/pago | **3 placas + 3 fichas gratis** (las primeras del catálogo); el resto de pago (29 items) |
| 2 | Esquema de precios | **Dos niveles 80/200 por id** (como `skinPrice` actual) |
| 3 | Migración de jugadores actuales | **Otorgar solo lo equipado** si pasa a ser de pago |
| 4 | Forma de la tienda | **Pulir `SkinGallery` = Tienda** (una pantalla, sin flujos duplicados) |
| 5 | Skin `nebula` (WIP en árbol, test en rojo) | **Aparte**: solo cablear resources (test en verde, sin catálogo/strings). Cuando entre, **precio >200 (nuevo tope)** — decisión 2026-10-03 |
| 6 | Modelo de posesión | **A: namespacing** `board:<id>` / `domino:<id>` |
| 7 | PCBs animadas | **Fuera de alcance de este diseño** — se resuelve aparte |

Listas exactas (primeras 3 del catálogo actual, confirmadas):
- **Placas gratis:** `pcb`, `blueprint`, `industrial`
- **Fichas gratis:** `kenney`, `dark`, `gingerbread`

## Alcance

**Dentro:** reglas de catálogo/precios en `Rewards` (dominio), posesión namespaced
+ normalización + otorgamiento de lo equipado, `buySkin` con categoría, check de
propiedad en `setDominoSkin`, UI de tienda (renombre, badge FREE, badge precio,
bugs latentes del diálogo), cableado **resources-only** de `nebula` (para cerrar
su test rojo), tests (dominio, app, instrumentados), docs (GDD, ESTADO, ROADMAP
bloque K, `assets/skins/README`).

**Fuera:** animaciones de PCB (trabajo aparte, decisión del usuario), `nebula`
en catálogo/strings/precio (entra después, tope >200), IAP/dinero real,
pantallas o rutas nuevas, bump de `PuzzlePreview.REVISION`, arte nuevo, audio
nuevo (reutiliza `SoundCue.Coin`), cualquier cambio de generador/niveles (los
niveles 1–30, modo libre y escenarios siguen byte-idénticos).

## 1. Catálogo, precios y reglas (dominio puro)

**Totales: 35 items** — placas 17, fichas 18 (catálogo actual intacto; `nebula`
**no** entra). Gratis 6 · de pago 29 (14 placas + 15 fichas).

Contrato nuevo en `game-domain/.../Progression.kt` → `Rewards`:

```kotlin
val freeBoardSkins = setOf("pcb", "blueprint", "industrial")
val freeDominoSkins = setOf("kenney", "dark", "gingerbread")
val premiumSkins = setOf("copper", "aurora", "titanium", "jade", "ruby", "sapphire", "amber", "amethyst") // sin cambios
val boardSkinIds = setOf("pcb", "blueprint", "industrial", "rust", "ice", "graphite", "signal", "copper", "aurora", "obsidian", "ceramic", "titanium", "jade", "ruby", "sapphire", "amber", "amethyst")
val dominoSkinIds = setOf("kenney", "dark", "gingerbread", "hearts", "stars", "circuit", "blueprint", "copper", "ice", "aurora", "obsidian", "ceramic", "titanium", "jade", "ruby", "sapphire", "amber", "amethyst")
fun skinPrice(id: String) = if (id in premiumSkins) SKIN_PRICE else ENTRY_SKIN_PRICE  // premium → 200; resto de pago → 80
/** Total: id desconocido → false, así la guarda "unknown" vive en la capa de escritura (repo). */
fun isPaid(board: Boolean, id: String): Boolean =
    (if (board) id in boardSkinIds else id in dominoSkinIds) && id !in (if (board) freeBoardSkins else freeDominoSkins)
fun ownedKey(board: Boolean, id: String) = if (board) "board:$id" else "domino:$id"
```

- **La regla se invierte** respecto al actual `affordable → 80 / resto → 200`:
  ahora `premium → 200 / resto → 80`. Resultado sobre el catálogo: los 8 gem
  como placa y como ficha → **16 items a 200**; todo lo demás de pago → **13
  items a 80** (`graphite`/`signal` siguen a 80).
- **El estado de pago es (categoría, id)**: `blueprint` es gratis como placa y de
  pago como ficha. Por eso `purchasableSkins`/`affordableSkins` (sets globales de
  id) **se eliminan**; consumidores migran a `isPaid(board, id)`.
- **`nebula` resources-only:** ramas en `boardSkinResources.kt` (full + preview),
  `dominoShellResource` y `dominoPipColor` (`#FFF3E0` según el test) con los
  assets ya exportados en `drawable-nodpi/` (+ originales y `generate_nebula.py`
  versionados). **Sin** entrada en `boardSkins`/`dominoSkins`, **sin** strings,
  **sin** precio: no aparece en la tienda. Cierra en verde
  `DominoResourcesTest.nebulaPackResolvesBoardShellAndStarlightPip` (el WIP en
  rojo). Su entrada de catálogo y su tope de precio (>200) son trabajo futuro.

## 2. Posesión, compra y migración (data layer)

**Claves:** `owned_skins` guarda `board:<id>` / `domino:<id>`.

- **Normalización en lectura** — función pura e idempotente: id desnudo →
  `board:<id>` (el único historial de compras son placas: `paid` era
  `pcb && …`). Sin flag de migración; aplica en la lectura de `PlayerPreferences`
  y en toda escritura posterior. Los perfiles legados funcionan para siempre.
- **Otorgar lo equipado** — `ensureEquippedOwned()`, idempotente, llamado una vez
  desde el `init` de `MainViewModel`: si `prefs.boardSkin` (o `prefs.dominoSkin`)
  es de pago y su clave namespaced no está en posesión → se añade. Nada más
  cambia; **sin créditos retroactivos**.
- **Compra** — `buySkin(board: Boolean, id: String)` en repo + ViewModel.
  Valida: `isPaid(board, id)` (gratis → nada que comprar), no poseído, saldo ≥
  `skinPrice(id)`; **un solo `store.edit`** (atomicidad actual). La pertenencia
  al catálogo se valida en el propio repo con `isPaid` (total: id desconocido →
  false), conservando la guarda `unknown ids` en la capa de escritura donde
  estaba; un test de app fija la equivalencia listas de UI ↔ sets del dominio
  para que no se desvíen. Éxito → `SoundCue.Coin`; rechazo → tick.
- **Equipado con check en ambas categorías:**
  - `setBoardSkin` → exige `!isPaid(true, id)` o `board:<id>` en posesión.
  - `setDominoSkin` → **añade el mismo check** (hoy escribe sin validar); sin
    esto las fichas de pago serían impagables.
- La galería ya distingue categoría en Equip
  (`if (pcb) setBoardSkin else setDominoSkin`); el flujo no cambia.

## 3. UI — La Tienda (`SkinGallery`)

- **Renombre:** string `skins` ("SKINS") → `store` con valor **"STORE" (EN) /
  "TIENDA" (ES)**; se actualizan los 2 usos (`MenuScreen:94` botón de menú,
  `SkinGallery:96` cabecera). La ruta de navegación `skins` no se toca.
- **Tarjeta:**
  - `paid = Rewards.isPaid(pcb, id)` para las dos chips (fichas pasan a ser de
    pago por primera vez).
  - `owned = claveNamespaced(id) in prefs.ownedSkins` (normalizada al leer).
  - **Badge FREE:** nuevo string `skin_free` ("FREE"/"GRATIS") en la línea donde
    hoy solo se muestra el precio; `if (paid) precio else FREE`.
  - Botones (`Equipped`/`Equip`/`Buy · N credits`), `missing_credits`,
    `testTags` (`skin-action-*`, `confirm-purchase`, `skin filter`) y chips
    Fichas/PCB: sin cambios de flujo.
- **Bugs latentes corregidos en raíz:**
  1. `SkinGallery:152` — el título del diálogo hace `boardSkins.first { it.id == id }`:
     hoy no revienta porque solo se compran placas; con fichas de pago lanzaría
     `NoSuchElementException`. El estado `purchase` pasa a guardar
     `(board: Boolean, id: String)` y el título/precio resuelven en la lista
     correcta.
  2. `confirm_skin_purchase` ("You can equip this PCB after purchase") y
     `challenge_description` ("for PCB skins") → texto generalizado EN+ES.
- **Strings (EN + ES, pares sincronizados):** `store`, `skin_free`, ajuste de
  `confirm_skin_purchase` (sin `skin_nebula`: nebula no aparece en UI).

## 4. Tests

**Dominio (`SkinCatalogTest` reescrito):** los 3 tests actuales fijan la regla
vieja (*"obsidian no es comprable"*, *"las fichas nunca entran al set"*) que este
cambio derriba **a propósito** (contrato nuevo decidido, no expectativas
ajustadas en un fallo). Nuevos asserts:
- `freeBoardSkins`/`freeDominoSkins` con membresía exacta (los primeros 3).
- Caso cruzado: `isPaid(true, "blueprint") == false`,
  `isPaid(false, "blueprint") == true`; los 6 gratis → `isPaid == false` en su
  categoría.
- `skinPrice`: 8 gem → 200; `graphite`, `signal`, `obsidian`, `ceramic`,
  `hearts` → 80.

**App (unit):** `DominoResourcesTest.nebulaPackResolvesBoardShellAndStarlightPip`
→ verde con los resources de `nebula` cableados (sin catálogo). Nuevo
`SkinCatalogListsTest`: `boardSkins`/`dominoSkins` ≡ `boardSkinIds`/`dominoSkinIds`
y primeras 3 de cada lista ≡ free sets.

**Instrumentados (`ProgressionUiTest`, store aislado):**
1. Compra de ficha end-to-end: chip fichas → tarjeta → Buy → confirm → Equipar.
2. Migración: sembrar `owned_skins={copper}` (desnudo), `board_skin=obsidian`
   (pago no poseído), `domino_skin=hearts` (paga no poseída) → al abrir:
   `{board:copper, board:obsidian, domino:hearts}`; una skin de pago **no
   equipada** no se otorga.
3. Idempotencia: segunda apertura no duplica ni cambia nada.
4. Los 10 tests existentes en verde (ajustando asserts de ids desnudos si los
   hubiera).

## 5. Documentación

- `docs/GDD.md` §economía: reescribir *"…grafito y señal cuestan 80. Las fichas
  de dominó no tienen coste."* → reparto 3+3, dos niveles 80/200, posesión
  namespaced, otorgamiento de lo equipado.
- `docs/ESTADO.md`: líneas "sus fichas correspondientes son gratuitas" +
  descripción de la tienda.
- `docs/ROADMAP.md`: **Bloque K — Tienda de skins** (formato de evidencia como
  F01/F03):
  - **K01** — Catálogo y precios: `Rewards` con `freeBoardSkins`/`freeDominoSkins`,
    `isPaid`, regla invertida; `nebula` resources-only (test en verde, sin
    catálogo/strings/precio).
  - **K02** — Posesión y compra: namespaced `board:`/`domino:`, normalización en
    lectura, `ensureEquippedOwned()`, `buySkin(board, id)`, check en
    `setDominoSkin`.
  - **K03** — UI: `store`/TIENDA, badge FREE, `purchase(board, id)`, bugs del
    diálogo y textos EN/ES.
  - **K04** — Tests y docs: `SkinCatalogTest` reescrito, `ProgressionUiTest`
    (+3 flujos), GDD/ESTADO/README de skins.
- `assets/skins/README.md` (catálogo de precios) y `docs/PLAY.md` si listan
  precios/textos de skins.

## Archivos a tocar

| Área | Archivos |
|------|----------|
| Dominio | `game-domain/.../Progression.kt` (`Rewards`), `SkinCatalogTest.kt` |
| Data | `data/PlayerPreferencesRepository.kt` (normalización, checks, `buySkin`) |
| VM | `MainViewModel.kt` (`buySkin(board,id)` + guarda de catálogo, `ensureEquippedOwned`) |
| UI | `ui/ProgressionScreens.kt` (listas + `SkinGallery`), `ui/MenuScreen.kt`, `ui/BoardSkinResources.kt`, `ui/DominoImage.kt` |
| Strings | `res/values/strings.xml`, `res/values-es/strings.xml` |
| Tests | `ProgressionUiTest.kt` (+3), `DominoResourcesTest` (test ya existe; pasa con resources) |
| Docs | `GDD.md`, `ESTADO.md`, `ROADMAP.md`, `assets/skins/README.md`, `PLAY.md` |
| Nebula (solo resources) | `ui/BoardSkinResources.kt`, `ui/DominoImage.kt` + assets ya en árbol: `drawable-nodpi/{board,domino}_nebula*.png`, `assets/skins/originals/*`, `assets/skins/generate_nebula.py` |

`mcp_audit.db` (sin relación) queda fuera de estos commits.

## Criterios de aceptación

1. `./gradlew test` en verde (dominio + app; `nebulaPack…` en verde con
   resources-only).
2. `./gradlew :app:lintDebug` sin errores.
3. `./gradlew :app:connectedDebugAndroidTest` en verde en emulador (28 actuales
   + 3 nuevos de `ProgressionUiTest` = 31).
4. Reparto verificable: 6 gratis (3+3), 29 de pago = 14 placas + 15 fichas
   (13 items a 80 · 16 items a 200 · 35 en total).
5. Perfil con `owned_skins` desnudos y skins equipadas de pago hereda su
   posesión sin perder equipado y sin créditos retroactivos.
6. Compra de ficha deduce saldo de forma atómica y equipa tras comprar.
7. Docs (GDD/ESTADO/ROADMAP-K/README) reflejan el nuevo contrato, EN/ES
   sincronizados, niveles y previews intactos (`REVISION` sin bump).

# Cartas e iconos — Mainboard Override

This document defines the visual brief for the seven script cards and the two free play board buffs in Mainboard Override. It is written for bitmap generation tools. The app draws all labels, values, RAM costs and noise values in Compose, so generated art must not contain readable text, numbers, logos or UI frames.

## Shared card format

- Canvas: PNG, 768 × 512 px, landscape 3:2, transparent or deep PCB background.
- Safe area: keep the central illustration inside 10% margins; leave the top 18% clear for the card title and the bottom 18% clear for RAM/noise badges.
- Style: tactical cyber security terminal, hand-built PCB traces, crisp vector-like shapes, restrained neon glow, high contrast at 128 dp wide.
- Palette: void green `#07110F`, panel green `#10231D`, terminal lime `#B6FF2E`, cyan `#39E7E0`, warning amber `#FFD43B`, danger red `#FF4155`, muted copper `#76968C`.
- Lighting: one directional glow from the upper right; no photorealism, no gradients that hide the silhouette, no tiny details below 4 px.
- Composition: one clear focal symbol, three or fewer secondary elements, diagonal circuit traces that suggest an action, no characters or hands.

## Card prompts

### PING

Prompt: `A cyber terminal script card illustration for PING, a compact radar pulse scanning a domino circuit board, cyan concentric rings, three tiny upcoming domino silhouettes appearing as signal echoes, dark PCB traces, lime and cyan neon accents, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

### SPOOF

Prompt: `A cyber terminal script card illustration for SPOOF, one domino tile with a glowing port being rewritten by a cyan cursor and split signal, two clean value sockets connected by amber circuit traces, dark PCB background, lime and cyan neon accents, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

### KILL_PROCESS

Prompt: `A cyber terminal script card illustration for KILL PROCESS, a red daemon process node interrupted by a sharp lime shutdown slash, broken firewall gate fragments and sparks, dark PCB traces, danger red with terminal lime highlights, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

### BRIDGE

Prompt: `A cyber terminal script card illustration for BRIDGE, two matching glowing circuit nodes joined by a bright bridge over a blocked firewall gap, horizontal and vertical trace hints, cyan connection arc with amber endpoints, dark PCB traces, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

### STEALTH

Prompt: `A cyber terminal script card illustration for STEALTH, a signal mask erasing a burst of red noise particles into a clean quiet trace line, dispersing scrambled static rings fading over dark PCB traces, terminal lime and cyan neon accents with muted danger red, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

### CLOAK

Prompt: `A cyber terminal script card illustration for CLOAK, a square microchip node shrouded by a camouflage veil of thin terminal lime scan lines, the chip glowing dimmer behind the veil, deflected diagonal cyan scan beams sliding off the veil, dark PCB traces, terminal lime and cyan neon accents, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

Shipped art (2026-10-17): fusion pipeline — FLUX base render from endpoint `flux.2-klein-4b` (Apache 2.0), seed 101, plus a Pillow depth post-process; final candidate judged by Nemotron 3.5 super-vl-120b. Exported as `script_card_cloak.png`, 1254 × 1254 RGBA (full recipe in `assets/cards/cloak-prompt.md`).

### STUN

Prompt: `A cyber terminal script card illustration for STUN, a square daemon process node frozen by a jagged amber lightning bolt, a bright spark burst at the impact point, broken motion trail stubs short-circuiting into stillness, dark PCB traces, warning amber with danger red highlights, crisp tactical game UI concept art, landscape 3:2, no text, no letters, no numbers, no logo, clear center silhouette, safe empty top and bottom margins.`

Shipped art (2026-10-17): fusion pipeline — FLUX base render from endpoint `flux.2-klein-4b` (Apache 2.0), seed 1202, plus a Pillow depth post-process; final candidate judged by Nemotron 3.5 super-vl-120b. Exported as `script_card_stun.png`, 1254 × 1254 RGBA (full recipe in `assets/cards/stun-prompt.md`).

## Board buff icons

### TRACE_COOLER

Prompt: `A small cyber PCB pickup icon for a trace cooler, a compact cooling fin and downward waveform arrow, cyan ice glow with lime center, readable at 32 dp, transparent background, crisp vector-like game asset, no text, no letters, no numbers.`

### RAM_RESERVE

Prompt: `A small cyber PCB pickup icon for a RAM reserve, a glowing memory chip with one extra illuminated module, terminal lime and cyan accents, readable at 32 dp, transparent background, crisp vector-like game asset, no text, no letters, no numbers.`

## Export and review

Export one PNG per prompt using the names `card_ping.png`, `card_spoof.png`, `card_kill_process.png`, `card_bridge.png`, `card_stealth.png`, `card_cloak.png`, `card_stun.png`, `buff_trace_cooler.png`, and `buff_ram_reserve.png`. The packaged runtime names are `script_card_*.png` at 1254 × 1254 (square canvas, illustration centered, transparent background); the STEALTH original lives at `assets/cards/originals/` and matches its shipped export, while the CLOAK and STUN files kept there are the earlier Pollinations renders (archive only — the shipped exports of those two come from the fusion pipeline described above). Process notes for all three live in `assets/cards/stealth-prompt.md`, `cloak-prompt.md` and `stun-prompt.md`. Before importing, verify that the illustration remains legible at 128 × 85 px, that all safe margins are empty, and that no accidental text was generated.

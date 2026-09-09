# CARTS specifications

This document defines the visual brief for the four script cards and the two free play board buffs in Mainboard Override. It is written for bitmap generation tools. The app draws all labels, values, RAM costs and noise values in Compose, so generated art must not contain readable text, numbers, logos or UI frames.

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

## Board buff icons

### TRACE_COOLER

Prompt: `A small cyber PCB pickup icon for a trace cooler, a compact cooling fin and downward waveform arrow, cyan ice glow with lime center, readable at 32 dp, transparent background, crisp vector-like game asset, no text, no letters, no numbers.`

### RAM_RESERVE

Prompt: `A small cyber PCB pickup icon for a RAM reserve, a glowing memory chip with one extra illuminated module, terminal lime and cyan accents, readable at 32 dp, transparent background, crisp vector-like game asset, no text, no letters, no numbers.`

## Export and review

Export one PNG per prompt using the exact names `card_ping.png`, `card_spoof.png`, `card_kill_process.png`, `card_bridge.png`, `buff_trace_cooler.png`, and `buff_ram_reserve.png`. Before importing, verify that the illustration remains legible at 128 × 85 px, that all safe margins are empty, and that no accidental text was generated.

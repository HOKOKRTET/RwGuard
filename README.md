# RwGuard — Anti-Crash & Packet Limiter

RwGuard is a packet-level firewall and exploit mitigation plugin for Paper, Purpur, Folia, and Spigot (1.16.5 – 26.x+). 

Unlike plugins that handle packets on the main thread through high-level Bukkit events, RwGuard intercepts incoming traffic directly inside Netty's EventLoop before the server decodes or decompresses the payload. Malicious packets are dropped with 0% main thread TPS impact.

---

## How It Works

* **Pre-Decoder Netty Filter:** Intercepts raw ByteBuf data before Minecraft's decoder or GZIP inflater runs. Memory bombs and oversized buffers are dropped before heap allocation.
* **Folia Support:** Native compatibility with Folia's threaded regions (`folia-supported: true`). Network operations run asynchronously, and tasks use `GlobalRegionScheduler` / `AsyncScheduler`.
* **Multi-Engine:** Automatically hooks into PacketEvents (2.x) or ProtocolLib (5.x) if present. If neither is installed, runs in standalone mode via direct Netty channel pipeline injection.
* **Native Memory Safety:** All dropped ByteBuf buffers are released with `ReferenceCountUtil.safeRelease()` to prevent direct memory leaks.

---

## Exploit Mitigations

### NBT & Memory Bombs
* **Raw Decompressor Bomb Shield:** Scans compressed byte streams in ByteBuf before `NbtIo.read()`. Halts "Attack Client" / decompression CPU starvation attacks.
* **Recursion Depth Cap:** Enforces a maximum compound tag depth (default 32) to prevent `StackOverflowError` crashes.
* **Buffer Size Bounds:** Hard cap on raw byte buffers before decoder (default 512 KB uncompressed).

### Chat & Commands
* **Brigadier Selector Freeze:** Blocks `@a[nbt={...}]` and `@e[nbt={...}]` queries in chat and tab-complete before Brigadier evaluates them, preventing 10-second tick freezes.
* **Control Characters:** Drops messages and commands containing null bytes (`\0`) or ASCII control codes (< 32).
* **Length Limits:** Enforces maximum command length (default 256 characters).
* **PlaceholderAPI Stripping:** Automatically removes `%token%` placeholders from anvil renames to stop PAPI parse hangs and info leaks.

### Anarchy & Physics Lag Machines
* **Illegal Fireworks:** Restricts firework flight power (max 3) and explosive stars (max 8) across items, clicks, and crossbow shots. Prevents client GPU freezes and server tick stalls.
* **Falling Block Limiter:** Caps active falling sand, gravel, and anvil entities per chunk (default max 40). Stops collapsing chunk lag machines into portals or cobwebs.
* **Lever & Button Spam:** Rate-limits interaction with levers and buttons (default max 8 clicks/sec) to stop redstone clock tick starvation.
* **Redstone Limiter:** Tracks and throttles redstone clocks exceeding maximum updates per chunk window.

### Inventory & Items
* **Slot Index Validation:** Drops click packets with illegal slot IDs (< -1 or > 256).
* **Creative Item Limits:** Validates creative slot packets: stack size (max 64), display name length (max 128), and lore size (max 2048 chars).
* **SkullOwner Crash:** Limits skull owner strings to 64 characters to prevent auth-thread hangs.
* **Auto-Recipe Spam:** Limits `ServerboundPlaceRecipePacket` rates to stop crafting table flood crashes.
* **Potion Overflow:** Blocks potions with illegal duration (>1,000,000) or amplifier (>127).

### Movement & World
* **Coordinate Sanity:** Drops movement packets containing `NaN`, `Infinity`, or coordinates beyond world bounds (>30,000,000 blocks or Y > 4000 / Y < -2000).
* **Vehicle Desyncs:** Limits vehicle move packets and prevents End Gateway teleport crashers.
* **Spectate Bypass:** Rejects `PacketPlayInSpectate` from non-spectator players.
* **Projectile Speed:** Cancels projectile shoots with abnormal velocity vectors.

### Custom Payloads & Modded Channels
* **Channel Flood Protection:** Limits registered plugin channels per client (default max 128).
* **Payload Size:** Enforces hard cap on payload data (default 32 KB).
* **Purpur Beehive Exploit:** Restricts `purpur:beehive_c2s` custom payload spam.
* **Client Information:** Limits locale string to 16 characters and bounds requested view distance (2 to 64).

### Books & Signs
* **Surrogate Pairs:** Drops UTF-8 surrogate byte sequences that corrupt book serialization.
* **Page Bounds:** Limits book pages (max 50) and characters per page (max 512).
* **Lectern Spam:** Throttles rapid lectern opening and page flips.
* **Sign Limits:** Restricts line lengths (max 384 chars) and update packet rates.

---

## Features

* **Bedrock Adaptation:** Automatically detects Floodgate players and applies dynamic multiplier (2.0x) to prevent false kicks from mobile touch controls.
* **Printer Mod Tolerance:** `printer-mode-tolerance: true` prevents false kicks for Litematica and Schematica users.
* **Forensic Dumper:** Record raw packet byte-streams directly to `plugins/RwGuard/dumps/`:
  ```text
  /guard dump <player> [count]
  ```
* **Discord Alerts:** Asynchronous webhooks with rich embeds on blocked attacks.
* **Admin Tools:** Real-time throughput metrics via `/guard stats` and player inspection via `/guard check <player>`.

---

## Commands & Permissions

Permission: `rwguard.admin`

| Command | Description |
| :--- | :--- |
| `/guard stats` | Real-time PPS, dropped packet counters, and engine state |
| `/guard check <player>` | View player packet rates and violation history |
| `/guard dump <player> [count]` | Capture raw incoming packets to file for analysis |
| `/guard freeze <player>` | Drop all network packets from a player |
| `/guard unfreeze <player>` | Unfreeze a player |
| `/guard clear <player>` | Reset violations and counters |
| `/guard reload` | Reload configuration and messages |

---

## Compatibility

* **Platforms:** Paper, Purpur, Folia, Spigot
* **Versions:** 1.16.5 – 1.21.x+, 26.x+
* **Java:** 17+
* **Optional:** PacketEvents 2.x, ProtocolLib 5.x, Floodgate

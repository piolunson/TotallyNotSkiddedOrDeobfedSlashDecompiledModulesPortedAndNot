# Hachimi 2.0

A PvP/anarchy client for Fabric 1.21.1 — combat modules, exploits, rendering, movement and a ClickGUI.

This repository holds the **client sources** (`src/main/java`), remapped from intermediary (`class_1234`,
`field_1234`, `method_1234`) to Yarn 1.21.1 names, a built jar, and the original archive
`Hachimi-2.0.jar_Decompiler.com.zip`.

---

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.15.0 or newer |
| Fabric API | any build for 1.21.1 |
| Java | 21 |

## Installation

1. Install Fabric Loader for 1.21.1.
2. Drop `fabric-api` and `Hachimi-2.0-built.jar` from this repo into `.minecraft/mods/`.
3. Launch the game — the client loads as a mod (`environment: client`).

> The jar is built from these sources but has **not been launched in game** — treat it as a build to test.

## Controls

- **ClickGUI** — bound to the `ClickGui` module key by default (Client category, rebindable).
- **Commands** — prefix `.` (e.g. `.help`). The prefix and its key are changed with `.prefix`.
- In game, `.help` prints the current command list with descriptions.

---

## Modules

### Combat
Aura, AutoCrystal, PistonCrystal, AutoAnchor, Surround, AutoTrap, AutoCrawlTrap, AntiCrawl, SelfTrap, SelfFill,
AutoTotem, AutoArmor, AutoPotion, AutoRelease, AutoWeb, AutoXP, AutoMine, AutoRegear, AntiRegear, AntiWeakness,
Criticals, MaceAura, KeyPearl, Offhand, Mainhand, NoHitDelay, Blocker, BasePlace, PistonKick, SpearKill, SelfBow, AutoLog

### Exploits
PacketFly, Phase, Disabler, Backtrack, FakeLatency, FastLatency, FastProjectile, GodMode, ChorusControl,
ChorusInvincibility, XCarry, InventorySync, MaceSpoof, NewChunks, NoMineAnimation, PacketCanceler, RocketExtend, Crasher, AntiHunger

### Movement
Speed, Flight, ElytraFly, BoatFly, TridentFly, FakeFly, Velocity, Step, LongJump, Parkour, SafeWalk, Jesus, IceSpeed,
NoSlow, NoFall, NoJumpDelay, NoAccel, FastFall, FastSwim, Sprint, AutoWalk, EntitySpeed, EntityControl, Follower,
FireworkBoost, TickShift, FakeLag, AntiLevitation, Yaw

### Render
ESP, Nametags, Chams, Tracers, HoleESP, PhaseESP, StorageESP, BreakESP, Search, Skeleton, Trajectories, Waypoints,
Breadcrumbs, BlockHighlight, Crosshair, ViewModel, Animations, Ambience, Skybox, Shaders, Particles, KillEffects,
Fullbright, NoRender, NoWeather, NoBob, MotionCamera, Zoom, NameProtect, Tooltips, CrystalModel, TrueSight, ViewClip, PlaceEcho

### Player
Scaffold, AirPlace, AutoTool, AutoEat, FastEat, FastPlace, AutoFish, AutoMount, AutoRespawn, Speedmine,
InventoryManager, Replenish, Freecam, FreeLook, MultiTask, NoGlitch, NoRotate

### Miscellaneous
Timer, ChestStealer, ChestSwap, AutoLogin, AutoReconnect, AutoAFK, AntiAFK, AntiAim, AntiSpam, AutoAnvilRename,
AutoFrameDupe, BetterChat, ChatNotifier, ExtraTab, FakePlayer, Kit, MiddleClick, NoPacketKick, NoSoundLag, PMSound,
Server, Shulkerception, SkinBlink, Spammer, Swing, TrueDurability

### Client
HUD, CombatHud, ClickGui, Colors, Font, Socials, Rotations, Chat, Anticheat, RPC

---

## Commands

Prefix `.`:

```
bind        config      connect     coords      customfont  disableall
drawn       friend      hclip       help        hideall     history
kit         leave       load        macro       modules     nbt
notify      openfolder  peek        prefix      queue       reloadsound
reset       resetgui    save        skingrab    stats       toggle
vclip       vanish      waypoint    yaw
```

`.help` prints the full list with descriptions and arguments.

---

## Repository layout

```
Hachimi-2.0-built.jar                 built client (Fabric 1.21.1, intermediary jar)
Hachimi-2.0.jar_Decompiler.com.zip    original archive (CFR decompile, intermediary namespace)
src/main/java/net/hachimi/...         client sources after the Yarn 1.21.1 remap
src/main/java/meteordevelopment/...   Discord IPC library used by the RPC module
```

## State of the sources

- Remap: 775 files, 10 533 class references, 6 188 fields and 7 217 methods renamed to Yarn 1.21.1+build.3.
- **One** intermediary reference remains: `method_51531` in `MixinChunkSkylightProvider` — Yarn has no name for
  it, so it is used explicitly in `@Inject` with a comment.
- The tree compiled cleanly (Loom 1.9.2, MC 1.21.1, fabric-api 0.116.9+1.21.1) once the decompiler artifacts
  were repaired.
- Java 21, Gradle 8.14+.

## Building

This repository does not include a build setup — to produce a jar you need to wire these sources into Fabric
Loom for MC 1.21.1 with `net.fabricmc:yarn:1.21.1+build.3:v2` mappings and Fabric API `0.116.9+1.21.1`.
The client relies on an access widener (access to Minecraft fields and methods), so a build without one will
not compile.

---

## Credits

Original client: **haodzzzzzzz** — https://hachimiclient.pw

The sources here are the result of decompiling and remapping the original jar. The code belongs to its author;
if you are the author and want this gone, get in touch and I will remove it.

The client targets anarchy servers. Using it may break server rules — you do so at your own risk.

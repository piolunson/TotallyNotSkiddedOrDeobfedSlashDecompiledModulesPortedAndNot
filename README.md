# Hachimi 2.0

Klient PvP/anarchy na Fabric 1.21.1 — moduły combat, exploity, render, movement i GUI w stylu ClickGUI.

To repozytorium zawiera **źródła klienta** (`src/main/java`), zremapowane z intermediary (`class_1234`,
`field_1234`, `method_1234`) na nazwy Yarn 1.21.1, plus oryginalny archiwum `Hachimi-2.0.jar_Decompiler.com.zip`.

---

## Wymagania

| | |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.15.0 lub nowszy |
| Fabric API | dowolna wersja dla 1.21.1 |
| Java | 21 |

## Instalacja

1. Zainstaluj Fabric Loader dla 1.21.1.
2. Wrzuć `fabric-api` oraz jar klienta do `.minecraft/mods/`.
3. Uruchom grę — klient ładuje się jako mod (`environment: client`).

> W tym repo **nie ma** gotowego jara, tylko źródła. Klienta nie testowałem w grze po zbudowaniu.

## Sterowanie

- **ClickGUI** — domyślnie pod klawiszem `ClickGui` (moduł w kategorii Client, można zmienić).
- **Komendy** — prefix `.` (np. `.help`). Prefix i jego klawisz zmienia komenda `.prefix`.
- W grze `.help` wypisze aktualną listę komend z opisami.

---

## Moduły

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

## Komendy

Prefix `.`:

```
bind        config      connect     coords      customfont  disableall
drawn       friend      hclip       help        hideall     history
kit         leave       load        macro       modules     nbt
notify      openfolder  peek        prefix      queue       reloadsound
reset       resetgui    save        skingrab    stats       toggle
vclip       vanish      waypoint    yaw
```

Pełną listę z opisami i argumentami wypisuje `.help`.

---

## Struktura repozytorium

```
Hachimi-2.0.jar_Decompiler.com.zip   oryginalne archiwum (decompile CFR, namespace intermediary)
src/main/java/net/hachimi/...        źródła klienta po remapie na Yarn 1.21.1
src/main/java/meteordevelopment/...  Discord IPC (biblioteka używana przez moduł RPC)
hachsmth-tohmvre/, hchm/             luźne pliki, nietknięte
```

## Stan źródeł

- Remap: 775 plików, 10 533 odwołań do klas, 6 188 pól i 7 217 metod zamienione na nazwy Yarn 1.21.1+build.3.
- Zostało **jedno** odwołanie intermediary: `method_51531` w `MixinChunkSkylightProvider` — Yarn nie ma dla niego nazwy, więc jest użyte jawnie w `@Inject` z komentarzem.
- Drzewo kompilowało się czysto (Loom 1.9.2, MC 1.21.1, fabric-api 0.116.9+1.21.1) po naprawie artefaktów decompilera.
- Java 21, Gradle 8.14+.

## Budowanie

Repozytorium nie zawiera konfiguracji builda — do zbudowania jara trzeba podpiąć te źródła pod Fabric Loom
dla MC 1.21.1 z mappingami `net.fabricmc:yarn:1.21.1+build.3:v2` i Fabric API `0.116.9+1.21.1`.
Klient korzysta z access widenera (dostęp do pól i metod MC), więc build bez niego się nie skompiluje.

---

## Autorzy i licencja

Oryginalny klient: **haodzzzzzzz** — https://hachimiclient.pw

Źródła w tym repo są wynikiem dekompilacji i remapu oryginalnego jara. Kod należły do jego autora;
jeśli jesteś autorem i chcesz, żeby to zniknęło — napisz, usunę.

Klient jest przeznaczony do gry na serwerach anarchy. Używanie go może łamać regulamin serwerów —
robią to na własną odpowiedzialność.

#!/usr/bin/env python3
"""Trzecia tura napraw: konkretne miejsca wskazane przez javac."""
import re

ROOT = 'downloads/Hachimi-2.0.jar_Decompiler.com/'

EDITS = [
    # --- Listener: cast na Invokable ---
    ('net/hachimi/eventbus/Listener.java',
     'this.invoker = callSite.getTarget().invoke(subscriber);',
     'this.invoker = (Invokable<Object>)callSite.getTarget().invoke(subscriber);'),

    # --- MixinGameOptions: typ generyczny SimpleOption<Integer> ---
    ('net/hachimi/client/mixin/option/MixinGameOptions.java',
     'this.fov = new net.minecraft.client.option.SimpleOption("options.fov"',
     'this.fov = new net.minecraft.client.option.SimpleOption<Integer>("options.fov"'),

    # --- ModuleButton: kasty Config<?> ---
    ('net/hachimi/client/impl/gui/click/impl/config/ModuleButton.java',
     'new CheckboxButton(frame, this, config, x, y)',
     'new CheckboxButton(frame, this, (Config)config, x, y)'),
    ('net/hachimi/client/impl/gui/click/impl/config/ModuleButton.java',
     'new DropdownButton(frame, this, config, x, y)',
     'new DropdownButton(frame, this, (Config)config, x, y)'),
    ('net/hachimi/client/impl/gui/click/impl/config/ModuleButton.java',
     'new TextButton(frame, this, config, x, y)',
     'new TextButton(frame, this, (Config)config, x, y)'),
    ('net/hachimi/client/impl/gui/click/impl/config/ModuleButton.java',
     'new BindButton(frame, this, config, x, y)',
     'new BindButton(frame, this, (Config)config, x, y)'),
    ('net/hachimi/client/impl/gui/click/impl/config/ModuleButton.java',
     'new ColorButton(frame, this, config, x, y)',
     'new ColorButton(frame, this, (Config)config, x, y)'),

    # --- Config: byte -> T ---
    ('net/hachimi/client/api/config/Config.java',
     'return (T)element.getAsByte();',
     'return (T)(Object)element.getAsByte();'),

    # --- listy konfiguracyjne: surowa ArrayList rzutowana na T ---
    ('net/hachimi/client/api/config/setting/ItemListConfig.java',
     'super(name, desc, new ArrayList<>(Arrays.asList(values)));',
     'super(name, desc, (T)new ArrayList(Arrays.asList(values)));'),
    ('net/hachimi/client/api/config/setting/BlockListConfig.java',
     'super(name, desc, new ArrayList<>(Arrays.asList(values)));',
     'super(name, desc, (T)new ArrayList(Arrays.asList(values)));'),
    ('net/hachimi/client/api/config/setting/EntityListConfig.java',
     'super(name, desc, new ArrayList<>(Arrays.asList(values)));',
     'super(name, desc, (T)new ArrayList(Arrays.asList(values)));'),

    # --- CommandManager: surowy builder ---
    ('net/hachimi/client/impl/manager/client/CommandManager.java',
     'LiteralArgumentBuilder<Object> literalArgumentBuilder = (LiteralArgumentBuilder)',
     'LiteralArgumentBuilder literalArgumentBuilder = (LiteralArgumentBuilder)'),

    # --- ExplosionUtil: EntityType<?> -> surowy ---
    ('net/hachimi/client/util/world/ExplosionUtil.java',
     'return DefaultAttributeRegistry.get(entity.getType());',
     'return DefaultAttributeRegistry.get((EntityType)entity.getType());'),

    # --- StreamUtils: jawne typy w lambdach ---
    ('net/hachimi/client/util/StreamUtils.java',
     '}).sorted(Comparator.comparing(Intermediary::key)).map(Intermediary::value);',
     '}).sorted(Comparator.comparing((Intermediary<T, U> i) -> i.key())).map((Intermediary<T, U> i) -> i.value());'),

    # --- Backtrack: surowy Packet.apply ---
    ('net/hachimi/client/impl/module/exploit/BacktrackModule.java',
     'packet.apply(mc.getNetworkHandler());',
     '((Packet)packet).apply(mc.getNetworkHandler());'),

    # --- HUDModule: Stream<Module> ---
    ('net/hachimi/client/impl/module/client/HUDModule.java',
     'Stream var10000 = modules.stream();',
     'Stream<Module> var10000 = modules.stream();'),

    # --- AutoMine: comparatory z typem ---
    ('net/hachimi/client/impl/module/combat/AutoMineModule.java',
     'Comparator var10000;',
     'Comparator<PlayerEntity> var10000;'),
    ('net/hachimi/client/impl/module/combat/AutoMineModule.java',
     'case 1 -> var10000 = Comparator.comparingDouble((p) -> (double)(p.getHealth() + p.getAbsorptionAmount()));',
     'case 1 -> var10000 = Comparator.comparingDouble((PlayerEntity p) -> (double)(p.getHealth() + p.getAbsorptionAmount()));'),
    ('net/hachimi/client/impl/module/combat/AutoMineModule.java',
     'default -> var10000 = Comparator.comparingDouble((p) -> mc.player.squaredDistanceTo(p));',
     'default -> var10000 = Comparator.comparingDouble((PlayerEntity p) -> mc.player.squaredDistanceTo(p));'),

    # --- PistonCrystal: comparatory z typem ---
    ('net/hachimi/client/impl/module/combat/PistonCrystalModule.java',
     'Comparator var10000;',
     'Comparator<PlayerEntity> var10000;'),
    ('net/hachimi/client/impl/module/combat/PistonCrystalModule.java',
     'var10000 = Comparator.comparingDouble((e) -> mc.player.squaredDistanceTo(e));',
     'var10000 = Comparator.comparingDouble((PlayerEntity e) -> mc.player.squaredDistanceTo(e));'),
    ('net/hachimi/client/impl/module/combat/PistonCrystalModule.java',
     'var10000 = Comparator.comparingDouble((e) -> (double)(e.getHealth() + e.getAbsorptionAmount()));',
     'var10000 = Comparator.comparingDouble((PlayerEntity e) -> (double)(e.getHealth() + e.getAbsorptionAmount()));'),

    # --- AutoCrystal: DamageData<T> ---
    ('net/hachimi/client/impl/module/combat/AutoCrystalModule.java',
     '         this.damageData = damageData;',
     '         this.damageData = (T)(Object)damageData;'),

    # --- ModuleCommand: surowy Config ---
    ('net/hachimi/client/impl/command/ModuleCommand.java',
     'updateValue(Config<?> config, String value)',
     'updateValue(Config config, String value)'),
    ('net/hachimi/client/impl/command/ModuleCommand.java',
     'Color color = ((ColorConfig)config).parseColor(value);\n               config.setValue(color);',
     'Color parsedColor = ((ColorConfig)config).parseColor(value);\n               config.setValue(parsedColor);'),

    # --- SliderButton: setValue przez surowy NumberConfig ---
    ('net/hachimi/client/impl/gui/click/impl/config/setting/SliderButton.java',
     '            this.config.setValue(min);',
     '            ((NumberConfig)this.config).setValue(min);'),
    ('net/hachimi/client/impl/gui/click/impl/config/setting/SliderButton.java',
     '            this.config.setValue(max);',
     '            ((NumberConfig)this.config).setValue(max);'),

    # --- ChatNotifier: zmienna Packet nie moze nazywac sie entity ---
    ('net/hachimi/client/impl/module/misc/ChatNotifierModule.java',
     '         Packet entity = event.getPacket();\n         if (entity instanceof EntityStatusS2CPacket) {\n            EntityStatusS2CPacket packet = (EntityStatusS2CPacket)entity;',
     '         Packet rawPacket = event.getPacket();\n         if (rawPacket instanceof EntityStatusS2CPacket) {\n            EntityStatusS2CPacket packet = (EntityStatusS2CPacket)rawPacket;'),

    # --- MixinLivingEntityRenderer: getRenderLayer oczekuje T ---
    ('net/hachimi/client/mixin/render/entity/MixinLivingEntityRenderer.java',
     'this.getRenderLayer(livingEntity, false, false, false)',
     'this.getRenderLayer((T)livingEntity, false, false, false)'),

    # --- AntiCheatManager: rozdzielenie dwoch zmiennych ---
    ('net/hachimi/client/impl/manager/anticheat/AntiCheatManager.java',
     'int uid_dup = packet.getParameter();\n         this.transactions[this.index] = uid_dup;',
     'int pingParam = packet.getParameter();\n         this.transactions[this.index] = pingParam;'),
    ('net/hachimi/client/impl/manager/anticheat/AntiCheatManager.java',
     'uid_dup = event.getPacket();\n         if (uid_dup instanceof PlayerPositionLookS2CPacket packet) {',
     'uid = event.getPacket();\n         if (uid instanceof PlayerPositionLookS2CPacket packet) {'),

    # --- FastProjectile: trzy rozne "stack" ---
    ('net/hachimi/client/impl/module/exploit/FastProjectileModule.java',
     'ItemStack stack_dup = mc.player.getStackInHand(packet.getHand());',
     'ItemStack potionStack = mc.player.getStackInHand(packet.getHand());'),
    ('net/hachimi/client/impl/module/exploit/FastProjectileModule.java',
     'if ((stack_dup.getItem() == Items.SPLASH_POTION || stack_dup.getItem() == Items.LINGERING_POTION) && (Boolean)this.potionConfig.getValue() || stack_dup.getItem() == Items.ENDER_PEARL && (Boolean)this.pearlConfig.getValue() || stack_dup.getItem() == Items.SNOWBALL && (Boolean)this.snowballConfig.getValue() || stack_dup.getItem() == Items.EGG && (Boolean)this.eggConfig.getValue()) {',
     'if ((potionStack.getItem() == Items.SPLASH_POTION || potionStack.getItem() == Items.LINGERING_POTION) && (Boolean)this.potionConfig.getValue() || potionStack.getItem() == Items.ENDER_PEARL && (Boolean)this.pearlConfig.getValue() || potionStack.getItem() == Items.SNOWBALL && (Boolean)this.snowballConfig.getValue() || potionStack.getItem() == Items.EGG && (Boolean)this.eggConfig.getValue()) {'),
    ('net/hachimi/client/impl/module/exploit/FastProjectileModule.java',
     'stack_dup = event.getPacket();\n               if (stack_dup instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket) {\n                  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket packet = (net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket)stack_dup;',
     'stack = event.getPacket();\n               if (stack instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket) {\n                  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket packet = (net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket)stack;'),
    ('net/hachimi/client/impl/module/exploit/FastProjectileModule.java',
     'ItemStack stack_dup = mc.player.getMainHandStack();',
     'ItemStack mainStack = mc.player.getMainHandStack();'),
    ('net/hachimi/client/impl/module/exploit/FastProjectileModule.java',
     'if (stack_dup.getItem() == Items.BOW && (Boolean)this.arrowConfig.getValue()) {',
     'if (mainStack.getItem() == Items.BOW && (Boolean)this.arrowConfig.getValue()) {'),

    # --- SpeedModule: Packet x vs double x ---
    ('net/hachimi/client/impl/module/movement/SpeedModule.java',
     'double x_dup = (double)packet.getPlayerVelocityX();',
     'double velocityX = (double)packet.getPlayerVelocityX();'),
    ('net/hachimi/client/impl/module/movement/SpeedModule.java',
     'x_dup = event.getPacket();\n            if (x_dup instanceof EntityVelocityUpdateS2CPacket) {\n               EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket)x_dup;',
     'x = event.getPacket();\n            if (x instanceof EntityVelocityUpdateS2CPacket) {\n               EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket)x;'),
    ('net/hachimi/client/impl/module/movement/SpeedModule.java',
     'double x_dup = packet.getVelocityX();',
     'double velocityX2 = packet.getVelocityX();'),

    # --- AnimationsModule ---
    ('net/hachimi/client/impl/module/render/AnimationsModule.java',
     'packets_dup = event.getPacket();\n            if (packets_dup instanceof EntityAnimationS2CPacket) {\n               EntityAnimationS2CPacket packet = (EntityAnimationS2CPacket)packets_dup;',
     'packets = event.getPacket();\n            if (packets instanceof EntityAnimationS2CPacket) {\n               EntityAnimationS2CPacket packet = (EntityAnimationS2CPacket)packets;'),

    # --- AirPlace: zepsuty self-cast ---
    ('net/hachimi/client/impl/module/player/AirPlaceModule.java',
     'BlockHitResult result_dup = (BlockHitResult)result_dup;',
     'BlockHitResult blockHit = (BlockHitResult)result;'),
    ('net/hachimi/client/impl/module/player/AirPlaceModule.java',
     'BlockHitResult result_dup2 = (BlockHitResult)result_dup2;',
     'BlockHitResult blockHit2 = (BlockHitResult)result;'),

    # --- AutoTotem: Set vs Packet ---
    ('net/hachimi/client/impl/module/combat/AutoTotemModule.java',
     'Set<String> failureReasonsSet_dup = this.getFailureReasons();\n               if (failureReasonsSet_dup.isEmpty()) {',
     'Set<String> failureReasons = this.getFailureReasons();\n               if (failureReasons.isEmpty()) {'),
    ('net/hachimi/client/impl/module/combat/AutoTotemModule.java',
     'String.join(", ", failureReasonsSet_dup)',
     'String.join(", ", failureReasons)'),

    # --- ShadersModule: generyki renderera ---
    ('net/hachimi/client/impl/module/render/ShadersModule.java',
     'EntityRenderer<Entity> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(fakePlayerEntity);',
     'EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(fakePlayerEntity);'),
    ('net/hachimi/client/impl/module/render/ShadersModule.java',
     'EntityRenderer<Entity> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);',
     'EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(entity);'),
]

failed = []
for path, old, new in EDITS:
    full = ROOT + path
    s = open(full, encoding='utf-8').read()
    if old not in s:
        failed.append((path, old[:60]))
        continue
    s = s.replace(old, new)
    open(full, 'w', encoding='utf-8').write(s)

print('nalozonych poprawek:', len(EDITS) - len(failed), '/', len(EDITS))
for f in failed:
    print('  NIE ZNALEZIONO:', f)

RENAMES = [
    ('net/hachimi/client/impl/manager/player/InventoryManager.java', 'allowedBundle_dup', 'filteredPackets'),
    ('net/hachimi/client/impl/module/movement/VelocityModule.java', 'allowedBundle_dup', 'filteredPackets'),
    ('net/hachimi/client/impl/module/movement/BoatFlyModule.java', 'packetY_dup', 'packetYOffset'),
    ('net/hachimi/client/impl/module/player/AirPlaceModule.java', 'result_dup2', 'blockHit2'),
    ('net/hachimi/client/impl/module/player/AirPlaceModule.java', 'result_dup', 'blockHit'),
    ('net/hachimi/client/impl/module/render/AnimationsModule.java', 'packets_dup', 'bundlePackets'),
]
for path, old, new in RENAMES:
    full = ROOT + path
    s = open(full, encoding='utf-8').read()
    s = re.sub(r'\b%s\b' % re.escape(old), new, s)
    open(full, 'w', encoding='utf-8').write(s)
print('zmiany nazw _dup wykonane')

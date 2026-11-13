package net.hachimi.client.impl.module.exploit;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.projectile.FireworkVelocityEvent;
import net.hachimi.client.impl.event.item.TridentWaterEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.EncodeYawEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;

public final class DisablerModule extends ToggleModule {

    private static DisablerModule INSTANCE;

    Config<Mode> modeConfig;

    private final Timer fireworkTimer;

    public DisablerModule() {
        super("Disabler", "Disables anticheat checks", ModuleCategory.EXPLOITS);
        this.modeConfig    = this.register(new EnumConfig("Mode", "The mode for disabling anticheat checks", Mode.GRIM_TRIDENT, Mode.values()));
        this.fireworkTimer = new CacheTimer();
        INSTANCE = this;
    }

    public static DisablerModule getInstance() {
        return INSTANCE;
    }

    public String getModuleData() {
        return this.isGrim() ? "Grim" : EnumFormatter.formatEnum(this.modeConfig.getValue());
    }

    @EventListener
    public void onDisconnect(DisconnectEvent event) {
        this.disable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.getStage() != StageEvent.EventStage.PRE) return;

        if (this.modeConfig.getValue() == Mode.GRIM_TRIDENT) {
            if (mc.player.isUsingItem()) return;

            int tridentSlot = -1;
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof TridentItem
                        && EnchantmentUtil.getLevel(stack, Enchantments.RIPTIDE) > 0) {
                    tridentSlot = i;
                    break;
                }
            }
            if (tridentSlot == -1) return;

            Managers.INVENTORY.setSlot(tridentSlot);
            Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ZERO, Direction.DOWN));
            Managers.INVENTORY.syncToClient();

        } else if (this.modeConfig.getValue() == Mode.GRIM_FIREWORK) {
            int elytraSlot   = -1;
            int fireworkSlot = -1;
            for (int i = 0; i < 36; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof FireworkRocketItem && i < 9) fireworkSlot = i;
                if (stack.getItem() instanceof ElytraItem) elytraSlot = i;
            }

            if (!this.isBoostedByRocket() && !mc.player.isFallFlying() && fireworkSlot != -1) {
                if (mc.player.onGround()) {
                    mc.player.jumpFromGround();
                } else {
                    Managers.MOVEMENT.setMotionY(-0.05);
                }

                if (this.fireworkTimer.passed(1700)
                        && !mc.player.onGround()
                        && mc.player.getDeltaMovement().y < 0.0F) {

                    if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA && elytraSlot != -1) {
                        mc.gameMode.handleInventoryMouseClick(0,
                                elytraSlot < 9 ? elytraSlot + 36 : elytraSlot,
                                0, ClickType.QUICK_MOVE, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, 6, 0, ClickType.QUICK_MOVE, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0,
                                elytraSlot < 9 ? elytraSlot + 36 : elytraSlot,
                                0, ClickType.QUICK_MOVE, mc.player);
                    }

                    Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(
                            mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    Managers.INVENTORY.setSlot(fireworkSlot);
                    Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                            InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                    Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    Managers.INVENTORY.syncToClient();
                    this.fireworkTimer.reset();

                    if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && elytraSlot != -1) {
                        mc.gameMode.handleInventoryMouseClick(0, 6, 0, ClickType.QUICK_MOVE, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0,
                                elytraSlot < 9 ? elytraSlot + 36 : elytraSlot,
                                0, ClickType.QUICK_MOVE, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, 6, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                }
            }
        }
    }

    @EventListener
    public void onTridentWaterCheck(TridentWaterEvent event) {
        if (this.modeConfig.getValue().equals(Mode.GRIM_TRIDENT)) {
            event.cancel();
        }
    }

    @EventListener
    public void onFireworkVelocity(FireworkVelocityEvent event) {
        if (this.modeConfig.getValue() == Mode.GRIM_FIREWORK) {
            event.cancel();
        }
    }

    @EventListener
    public void onEncodeYaw(EncodeYawEvent event) {
        if (this.isYawOverflow()) {
            event.cancel();
        }
    }

    public boolean isBoostedByRocket() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity rocket) {
                if (((AccessorFireworkRocketEntity) rocket).hookWasShotByEntity()
                        && ((AccessorFireworkRocketEntity) rocket).hookGetShooter() == mc.player) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGrim() {
        return this.modeConfig.getValue().name().toLowerCase().contains("grim");
    }

    public boolean isGrimFirework() {
        return this.isEnabled() && this.modeConfig.getValue() == Mode.GRIM_FIREWORK;
    }

    public boolean isYawOverflow() {
        return !mc.player.isFallFlying() && this.modeConfig.getValue() == Mode.GRIM_OVERFLOW;
    }

    public boolean grimFireworkCheck() {
        return this.isGrimFirework() && !this.isBoostedByRocket();
    }

    public boolean grimFireworkCheck2() {
        return this.isGrimFirework() && this.isBoostedByRocket();
    }

    public enum Mode {
        GRIM_TRIDENT, GRIM_FIREWORK, GRIM_OVERFLOW
    }
}

package dev.leonetic.features.modules.exploit;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.EnchantmentUtil;
import dev.leonetic.util.models.Timer;

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

public final class DisablerModule extends Module {

    private final Setting<Mode> modeConfig;
    private final Timer         fireworkTimer = new Timer();
    private int                 sequence      = 0;

    public DisablerModule() {
        super("Disabler", "Disables anticheat checks.", Category.EXPLOIT);
        this.modeConfig = mode("Mode", Mode.GRIM_TRIDENT);
    }

    private int nextSequence() {
        return ++sequence;
    }

    @Override
    public String getDisplayInfo() {
        return this.isGrim() ? "Grim" : modeConfig.getValue().name();
    }

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (nullCheck() || mc.player.isDeadOrDying()) return;

        if (modeConfig.getValue() == Mode.GRIM_TRIDENT) {
            if (mc.player.isUsingItem()) return;

            int tridentSlot = -1;
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (!stack.isEmpty()
                        && stack.getItem() instanceof TridentItem
                        && EnchantmentUtil.getLevel(Enchantments.RIPTIDE, stack) > 0) {
                    tridentSlot = i;
                    break;
                }
            }
            if (tridentSlot == -1) return;

            mc.player.getInventory().selected = tridentSlot;

            mc.player.connection.send(new ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND, nextSequence(),
                    mc.player.getYRot(), mc.player.getXRot()));

            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ZERO, Direction.DOWN, nextSequence()));

            mc.player.containerMenu.broadcastChanges();

        } else if (modeConfig.getValue() == Mode.GRIM_FIREWORK) {
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
                    var motion = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(motion.x, -0.05, motion.z);
                }

                if (fireworkTimer.passedMs(1700)
                        && !mc.player.onGround()
                        && mc.player.getDeltaMovement().y < 0.0) {

                    if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA && elytraSlot != -1) {
                        mc.gameMode.handleInventoryMouseClick(0,
                                elytraSlot < 9 ? elytraSlot + 36 : elytraSlot,
                                0, ClickType.QUICK_MOVE, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, 6, 0, ClickType.QUICK_MOVE, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0,
                                elytraSlot < 9 ? elytraSlot + 36 : elytraSlot,
                                0, ClickType.QUICK_MOVE, mc.player);
                    }

                    mc.player.connection.send(new ServerboundPlayerCommandPacket(
                            mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

                    mc.player.getInventory().selected = fireworkSlot;

                    mc.player.connection.send(new ServerboundUseItemPacket(
                            InteractionHand.MAIN_HAND, nextSequence(),
                            mc.player.getYRot(), mc.player.getXRot()));

                    mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

                    mc.player.containerMenu.broadcastChanges();
                    fireworkTimer.reset();

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

    // ─────────────────────────────────────────────────────────────
    //  Eventy Hachimi, których Leonetic nie ma w standardzie.
    //  Jeśli chcesz ich używać — musisz je najpierw dodać do Leonetic
    //  (TridentWaterEvent, FireworkVelocityEvent, EncodeYawEvent).
    //  DisconnectEvent można zastąpić override'em onDisable().
    // ─────────────────────────────────────────────────────────────

    /*
    @Subscribe
    private void onTridentWaterCheck(TridentWaterEvent event) {
        if (modeConfig.getValue() == Mode.GRIM_TRIDENT) event.cancel();
    }

    @Subscribe
    private void onFireworkVelocity(FireworkVelocityEvent event) {
        if (modeConfig.getValue() == Mode.GRIM_FIREWORK) event.cancel();
    }

    @Subscribe
    private void onEncodeYaw(EncodeYawEvent event) {
        if (isYawOverflow()) event.cancel();
    }
    */

    public boolean isBoostedByRocket() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity rocket) {
                if (rocket.getOwner() == mc.player) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGrim() {
        return modeConfig.getValue().name().toLowerCase().contains("grim");
    }

    public boolean isGrimFirework() {
        return isEnabled() && modeConfig.getValue() == Mode.GRIM_FIREWORK;
    }

    public boolean isYawOverflow() {
        return !mc.player.isFallFlying() && modeConfig.getValue() == Mode.GRIM_OVERFLOW;
    }

    public boolean grimFireworkCheck() {
        return isGrimFirework() && !isBoostedByRocket();
    }

    public boolean grimFireworkCheck2() {
        return isGrimFirework() && isBoostedByRocket();
    }

    public enum Mode {
        GRIM_TRIDENT, GRIM_FIREWORK, GRIM_OVERFLOW
    }
}

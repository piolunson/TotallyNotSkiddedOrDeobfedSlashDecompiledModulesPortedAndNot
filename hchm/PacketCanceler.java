package net.hachimi.client.impl.module.exploit;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.eventbus.annotation.EventListener;

import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

public class PacketCancelerModule extends ToggleModule {

    Config<Boolean> playerMoveConfig               = this.register(new BooleanConfig("PlayerMove", "Cancels the PlayerMoveC2SPacket packet", false));
    Config<Boolean> playerInputConfig              = this.register(new BooleanConfig("PlayerInput", "Cancels the PlayerInputC2SPacket packet", false));
    Config<Boolean> playerInteractBlockConfig      = this.register(new BooleanConfig("PlayerInteractBlock", "Cancels the PlayerInteractBlockC2SPacket packet", false));
    Config<Boolean> playerInteractEntityConfig     = this.register(new BooleanConfig("PlayerInteractEntity", "Cancels the PlayerInteractEntityC2SPacket packet", false));
    Config<Boolean> playerInteractItemConfig       = this.register(new BooleanConfig("PlayerInteractItem", "Cancels the PlayerInteractItemC2SPacket packet", false));
    Config<Boolean> craftRequestConfig             = this.register(new BooleanConfig("CraftRequest", "Cancels the CraftRequestSlotC2SPacket packet", false));
    Config<Boolean> updateSelectedSlotConfig       = this.register(new BooleanConfig("UpdateSelectedSlot", "Cancels the UpdateSelectedSlotC2SPacket packet", false));
    Config<Boolean> clickSlotConfig                = this.register(new BooleanConfig("ClickSlot", "Cancels the ClickSlotC2SPacket packet", false));
    Config<Boolean> pickFromInventoryConfig        = this.register(new BooleanConfig("PickFromInventory", "Cancels the PickFromInventoryC2SPacket packet", false));
    Config<Boolean> creativeInventoryActionConfig  = this.register(new BooleanConfig("CreativeInventoryAction", "Cancels the CreativeInventoryActionC2SPacket packet", false));
    Config<Boolean> handSwingConfig                = this.register(new BooleanConfig("HandSwing", "Cancels the HandSwingC2SPacket packet", false));
    Config<Boolean> teleportConfirmConfig          = this.register(new BooleanConfig("TeleportConfirm", "Cancels the TeleportConfirmC2SPacket packet", false));
    Config<Boolean> keepAliveConfig                = this.register(new BooleanConfig("KeepAlive", "Cancels the KeepAliveC2SPacket packet", false));
    Config<Boolean> commonPongConfig               = this.register(new BooleanConfig("CommonPong", "Cancels the CommonPongC2SPacket packet", false));

    public PacketCancelerModule() {
        super("PacketCanceler", "Cancels packets when sending to server", ModuleCategory.EXPLOITS);
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket && playerMoveConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundPlayerInputPacket && playerInputConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundUseItemOnPacket && playerInteractBlockConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundInteractPacket && playerInteractEntityConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundUseItemPacket && playerInteractItemConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundPlaceRecipePacket && craftRequestConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundSetCarriedItemPacket && updateSelectedSlotConfig.getValue()) {
            event.cancel();
        } else if (event.getPacket() instanceof ServerboundContainerClickPacket && clickSlotConfig.getValue()) {
            event.cancel();
        } else if (!(event.getPacket() instanceof ServerboundPickItemPacket)
                || !(pickFromInventoryConfig.getValue() || isAnticheatPick())) {
            // FIX #1: null-check na AnticheatModule
            if (event.getPacket() instanceof ServerboundSetCreativeModeSlotPacket && creativeInventoryActionConfig.getValue()) {
                event.cancel();
            } else if (event.getPacket() instanceof ServerboundSwingPacket && handSwingConfig.getValue()) {
                event.cancel();
            } else if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket && teleportConfirmConfig.getValue()) {
                event.cancel();
            } else if (event.getPacket() instanceof ServerboundKeepAlivePacket && keepAliveConfig.getValue()) {
                event.cancel();
            } else if (event.getPacket() instanceof ServerboundPongPacket && commonPongConfig.getValue()) {
                event.cancel();
            }
        } else {
            event.cancel();
        }
    }

    private boolean isAnticheatPick() {
        AnticheatModule ac = AnticheatModule.getInstance();
        return ac != null && ac.isPick();
    }
}

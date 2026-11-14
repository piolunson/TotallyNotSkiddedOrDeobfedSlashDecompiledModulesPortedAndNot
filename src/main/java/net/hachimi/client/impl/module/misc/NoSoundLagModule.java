package net.hachimi.client.impl.module.misc;

import java.util.Set;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class NoSoundLagModule extends ToggleModule {
   private static final Set<SoundEvent> LAG_SOUNDS;

   public NoSoundLagModule() {
      super("NoSoundLag", "Prevents sound effects from lagging the game", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      label20: {
         Packet var4 = event.getPacket();
         if (var4 instanceof PlaySoundFromEntityS2CPacket packet) {
            if (LAG_SOUNDS.contains(packet.getSound().value())) {
               break label20;
            }
         }

         var4 = event.getPacket();
         if (!(var4 instanceof PlaySoundS2CPacket)) {
            return;
         }

         PlaySoundS2CPacket packet2 = (PlaySoundS2CPacket)var4;
         if (!LAG_SOUNDS.contains(packet2.getSound().value())) {
            return;
         }
      }

      event.cancel();
   }

   static {
      LAG_SOUNDS = Set.of((SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(), (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value());
   }
}

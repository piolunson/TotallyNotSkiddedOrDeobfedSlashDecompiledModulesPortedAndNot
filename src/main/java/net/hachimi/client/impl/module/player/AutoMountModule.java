package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.hachimi.client.util.network.InteractType;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.network.packet.Packet;

public class AutoMountModule extends ToggleModule {
   Config<Float> rangeConfig = this.register(new NumberConfig("Range", "The range to mount entities", 0.1F, 4.0F, 6.0F));
   Config<Boolean> forceConfig = this.register(new BooleanConfig("ForceMount", "Forces the mounting without packets", false));
   Config<Boolean> horseConfig = this.register(new BooleanConfig("Horse", "Mounts horses", true));
   Config<Boolean> donkeyConfig = this.register(new BooleanConfig("Donkey", "Mounts donkeys", true));
   Config<Boolean> muleConfig = this.register(new BooleanConfig("Mule", "Mounts mules", true));
   Config<Boolean> llamaConfig = this.register(new BooleanConfig("Llama", "Mounts llamas", false));

   public AutoMountModule() {
      super("AutoMount", "Mounts nearby entities", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!mc.player.isRiding()) {
         for(Entity entity : mc.world.getEntities()) {
            double dist = mc.player.getEyePos().distanceTo(entity.getPos());
            if (!(dist > (double)(Float)this.rangeConfig.getValue()) && this.checkMount(entity)) {
               mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
               return;
            }
         }

      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof IPlayerInteractEntityC2SPacket packet) {
         if ((Boolean)this.forceConfig.getValue() && packet.getType() == InteractType.INTERACT_AT && this.checkMount(packet.getEntity())) {
            event.cancel();
         }
      }

   }

   private boolean checkMount(Entity entity) {
      boolean var10000;
      label38: {
         if ((Boolean)this.horseConfig.getValue()) {
            if (entity instanceof HorseEntity) {
               HorseEntity horseEntity = (HorseEntity)entity;
               if (!horseEntity.isBaby()) {
                  break label38;
               }
            }

            if (entity instanceof SkeletonHorseEntity) {
               break label38;
            }
         }

         if ((!(Boolean)this.donkeyConfig.getValue() || !(entity instanceof DonkeyEntity)) && (!(Boolean)this.muleConfig.getValue() || !(entity instanceof MuleEntity))) {
            label45: {
               if ((Boolean)this.llamaConfig.getValue() && entity instanceof LlamaEntity) {
                  LlamaEntity llamaEntity = (LlamaEntity)entity;
                  if (!llamaEntity.isBaby()) {
                     break label45;
                  }
               }

               var10000 = false;
               return var10000;
            }
         }
      }

      var10000 = true;
      return var10000;
   }
}

package net.hachimi.client.impl.module.movement;

import java.text.DecimalFormat;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class EntitySpeedModule extends ToggleModule {
   Config<Float> speedConfig = this.register(new NumberConfig("Speed", "The speed of the entity while moving", 0.1F, 0.5F, 4.0F));
   Config<Boolean> antiStuckConfig = this.register(new BooleanConfig("AntiStuck", "Prevents entities from getting stuck when moving up", false));
   Config<Boolean> strictConfig = this.register(new BooleanConfig("Strict", "The NCP-Updated bypass for speeding up entity movement", false));
   private final Timer entityJumpTimer = new CacheTimer();

   public EntitySpeedModule() {
      super("EntitySpeed", "Increases riding entity speeds", ModuleCategory.MOVEMENT);
   }

   public String getModuleData() {
      DecimalFormat decimal = new DecimalFormat("0.0");
      return decimal.format(this.speedConfig.getValue());
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (mc.player.isRiding() && mc.player.getControllingVehicle() != null) {
            double d = Math.cos(Math.toRadians((double)(mc.player.getYaw() + 90.0F)));
            double d2 = Math.sin(Math.toRadians((double)(mc.player.getYaw() + 90.0F)));
            BlockPos pos1 = BlockPos.ofFloored(mc.player.getX() + (double)2.0F * d, mc.player.getY() - (double)1.0F, mc.player.getZ() + (double)2.0F * d2);
            BlockPos pos2 = BlockPos.ofFloored(mc.player.getX() + (double)2.0F * d, mc.player.getY() - (double)2.0F, mc.player.getZ() + (double)2.0F * d2);
            if ((Boolean)this.antiStuckConfig.getValue() && !mc.player.getControllingVehicle().isOnGround() && !mc.world.getBlockState(pos1).blocksMovement() && !mc.world.getBlockState(pos2).blocksMovement()) {
               this.entityJumpTimer.reset();
               return;
            }

            BlockPos pos3 = BlockPos.ofFloored(mc.player.getX() + (double)2.0F * d, mc.player.getY(), mc.player.getZ() + (double)2.0F * d2);
            if ((Boolean)this.antiStuckConfig.getValue() && mc.world.getBlockState(pos3).blocksMovement()) {
               this.entityJumpTimer.reset();
               return;
            }

            BlockPos pos4 = BlockPos.ofFloored(mc.player.getX() + d, mc.player.getY() + (double)1.0F, mc.player.getZ() + d2);
            if ((Boolean)this.antiStuckConfig.getValue() && mc.world.getBlockState(pos4).blocksMovement()) {
               this.entityJumpTimer.reset();
               return;
            }

            if (mc.player.input.jumping) {
               this.entityJumpTimer.reset();
            }

            if (this.entityJumpTimer.passed(10000) || !(Boolean)this.antiStuckConfig.getValue()) {
               if (!mc.player.getControllingVehicle().isTouchingWater() || mc.player.input.jumping || !this.entityJumpTimer.passed(1000)) {
                  if (mc.player.getControllingVehicle().isOnGround()) {
                     mc.player.getControllingVehicle().setVelocity(mc.player.getVelocity().x, 0.4, mc.player.getVelocity().z);
                  }

                  mc.player.getControllingVehicle().setVelocity(mc.player.getVelocity().x, -0.4, mc.player.getVelocity().z);
               }

               if ((Boolean)this.strictConfig.getValue()) {
                  Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.interact(mc.player.getControllingVehicle(), false, Hand.MAIN_HAND));
               }

               this.handleEntityMotion((Float)this.speedConfig.getValue(), d, d2);
               this.entityJumpTimer.reset();
            }
         }

      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.player.isRiding() && !mc.options.sneakKey.isPressed() && mc.player.getControllingVehicle() != null) {
         if ((Boolean)this.strictConfig.getValue()) {
            if (event.getPacket() instanceof EntityPassengersSetS2CPacket) {
               event.cancel();
            } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
               event.cancel();
            }
         }

      }
   }

   private void handleEntityMotion(float entitySpeed, double d, double d2) {
      Vec3d motion = mc.player.getControllingVehicle().getVelocity();
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      if (forward == 0.0F && strafe == 0.0F) {
         mc.player.getControllingVehicle().setVelocity((double)0.0F, motion.y, (double)0.0F);
      } else {
         mc.player.getControllingVehicle().setVelocity((double)(forward * entitySpeed) * d + (double)(strafe * entitySpeed) * d2, motion.y, (double)(forward * entitySpeed) * d2 - (double)(strafe * entitySpeed) * d);
      }
   }
}

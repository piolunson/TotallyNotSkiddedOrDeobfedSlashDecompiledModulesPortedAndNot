package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.api.render.chams.ChamsModelRenderer;
import net.hachimi.client.api.render.model.StaticBipedEntityModel;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.render.entity.RenderCrystalEvent;
import net.hachimi.client.impl.event.render.entity.RenderEntityEvent;
import net.hachimi.client.impl.event.render.entity.RenderThroughWallsEvent;
import net.hachimi.client.impl.event.render.item.RenderArmEvent;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;

public class ChamsModule extends ToggleModule {
   private static ChamsModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig("Range", "The chams render range", 10.0F, 50.0F, 200.0F));
   Config<ChamsMode> modeConfig;
   Config<Float> widthConfig;
   Config<Boolean> wallsConfig;
   Config<Boolean> textureConfig;
   Config<Boolean> playersConfig;
   Config<Boolean> selfConfig;
   Config<Boolean> handsConfig;
   Config<Boolean> monstersConfig;
   Config<Boolean> animalsConfig;
   Config<Boolean> crystalsConfig;
   Config<Boolean> popsConfig;
   Config<Integer> fadeTimeConfig;
   Config<Color> colorConfig;
   private final Map<PopChamEntity, Animation> fadeList;

   public ChamsModule() {
      super("Chams", "Renders entity models through walls", ModuleCategory.RENDER);
      this.modeConfig = this.register(new EnumConfig("Mode", "The rendering mode for the chams", ChamsModule.ChamsMode.FILL, ChamsModule.ChamsMode.values()));
      this.widthConfig = this.register(new NumberConfig("Width", "The line width of the render", 1.0F, 1.5F, 5.0F, () -> this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL));
      this.wallsConfig = this.register(new BooleanConfig("ThroughWalls", "Renders chams through walls", true, () -> this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL));
      this.textureConfig = this.register(new BooleanConfig("Texture", "Renders the entity model texture", false, () -> (Boolean)this.wallsConfig.getValue() && this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL));
      this.playersConfig = this.register(new BooleanConfig("Players", "Render chams on other players", true));
      this.selfConfig = this.register(new BooleanConfig("Self", "Render chams on the player", true));
      this.handsConfig = this.register(new BooleanConfig("Hands", "Render chams on first-person hands", true));
      this.monstersConfig = this.register(new BooleanConfig("Monsters", "Render chams on monsters", true));
      this.animalsConfig = this.register(new BooleanConfig("Animals", "Render chams on animals", true));
      this.crystalsConfig = this.register(new BooleanConfig("Crystals", "Render chams on crystals", true));
      this.popsConfig = this.register(new BooleanConfig("Pops", "Render chams on totem pops", false));
      this.fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Timer for the fade", 0, 1000, 3000, () -> false));
      this.colorConfig = this.register(new ColorConfig("Color", "The color of the chams", new Color(255, 0, 0, 60)));
      this.fadeList = new ConcurrentHashMap<>();
      INSTANCE = this;
   }

   public static ChamsModule getInstance() {
      return INSTANCE;
   }

   public void onDisable() {
      this.fadeList.clear();
   }

   @EventListener
   public void onRenderThroughWalls(RenderThroughWallsEvent event) {
      if (this.modeConfig.getValue() == ChamsModule.ChamsMode.NORMAL && this.checkChams(event.getEntity())) {
         event.cancel();
      }

   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onRenderWorld(RenderWorldEvent event) {
      RenderBuffers.preRender();
      RenderSystem.disableDepthTest();

      for(Map.Entry<PopChamEntity, Animation> set : new HashSet<>(this.fadeList.entrySet())) {
         ((Animation)set.getValue()).setState(false);
         Color color = this.colorConfig.getValue();
         int boxAlpha = (int)((double)color.getAlpha() * ((Animation)set.getValue()).getFactor());
         int lineAlpha = (int)((double)145.0F * ((Animation)set.getValue()).getFactor());
         int boxColor = ColorUtil.withAlpha(color.getRGB(), boxAlpha);
         int lineColor = ColorUtil.withAlpha(color.getRGB(), lineAlpha);
         ChamsModelRenderer.renderStaticPlayerModel(event.getMatrices(), (AbstractClientPlayerEntity)set.getKey(), ((PopChamEntity)set.getKey()).getModel(), event.getTickDelta(), boxColor, lineColor, (Float)this.widthConfig.getValue(), this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL, true, false);
      }

      this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
      RenderBuffers.postRender();
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (!ShadersModule.getInstance().isEnabled() || (Boolean)ShadersModule.getInstance().textureConfig.getValue()) {
            RenderBuffers.preRender();
            if (!(Boolean)this.wallsConfig.getValue()) {
               RenderSystem.enableDepthTest();
            }

            for(Entity entity : mc.world.getEntities()) {
               if (!NoRenderModule.getInstance().isEnabled() || !NoRenderModule.getInstance().shouldSkipEntity(entity)) {
                  double x = Math.abs(mc.gameRenderer.getCamera().getPos().x - entity.getX());
                  double z = Math.abs(mc.gameRenderer.getCamera().getPos().z - entity.getZ());
                  double d = (double)(((Integer)mc.options.getViewDistance().getValue() + 1) * 16);
                  Vec3d start = FreecamModule.getInstance().isEnabled() ? FreecamModule.getInstance().getCameraPosition() : mc.player.getPos();
                  if (!(start.squaredDistanceTo(entity.getPos()) > ((NumberConfig)this.rangeConfig).getValueSq()) && !(x > d) && !(z > d) && RenderManager.isFrustumVisible(entity.getBoundingBox())) {
                     label90: {
                        if (entity instanceof LivingEntity) {
                           LivingEntity livingEntity = (LivingEntity)entity;
                           if (this.checkChams(livingEntity)) {
                              break label90;
                           }
                        }

                        if (!(entity instanceof EndCrystalEntity) || !(Boolean)this.crystalsConfig.getValue()) {
                           continue;
                        }
                     }

                     if (!(Boolean)this.wallsConfig.getValue()) {
                        RenderSystem.depthMask(false);
                     }

                     this.renderEntityChams(event.getMatrices(), entity, event.getTickDelta());
                  }
               }
            }

            RenderBuffers.postRender();
            if (!(Boolean)this.wallsConfig.getValue()) {
               RenderSystem.depthMask(true);
            }

         }
      }
   }

   @EventListener
   public void onRenderGame(RenderWorldEvent.Hand event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (!ShadersModule.getInstance().isEnabled() || (Boolean)ShadersModule.getInstance().textureConfig.getValue()) {
            RenderBuffers.preRender();
            if ((Boolean)this.handsConfig.getValue()) {
               int color1 = ((Color)this.colorConfig.getValue()).getRGB();
               int lineColor1 = ColorUtil.withAlpha(color1, 145);
               ChamsModelRenderer.renderHand(event.getMatrices(), event.getTickDelta(), lineColor1, color1, (Float)this.widthConfig.getValue(), this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL, this.modeConfig.getValue() != ChamsModule.ChamsMode.WIREFRAME, false);
            }

            RenderBuffers.postRender();
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.world != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket packet = (EntityStatusS2CPacket)var3;
            if (packet.getStatus() == 35 && (Boolean)this.popsConfig.getValue()) {
               Entity entity = packet.getEntity(mc.world);
               if (entity == mc.player || !(entity instanceof PlayerEntity)) {
                  return;
               }

               PlayerEntity player = (PlayerEntity)entity;
               Animation animation = new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue());
               this.fadeList.put(new PopChamEntity(player, mc.getRenderTickCounter().getTickDelta(true)), animation);
            }
         }

      }
   }

   @EventListener
   public void onRenderCrystal(RenderCrystalEvent event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (mc.player != null && (!(Boolean)this.textureConfig.getValue() || !(Boolean)this.wallsConfig.getValue()) && (Boolean)this.crystalsConfig.getValue() && mc.player.squaredDistanceTo(event.endCrystalEntity) <= ((NumberConfig)this.rangeConfig).getValueSq()) {
            event.cancel();
         }

      }
   }

   @EventListener
   public void onRenderEntity(RenderEntityEvent event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if (mc.player != null && (!(Boolean)this.textureConfig.getValue() || !(Boolean)this.wallsConfig.getValue()) && this.checkChams(event.entity) && !(mc.player.squaredDistanceTo(event.entity) > ((NumberConfig)this.rangeConfig).getValueSq())) {
            event.cancel();
            event.matrixStack.push();
            event.model.handSwingProgress = event.entity.getHandSwingProgress(event.g);
            event.model.riding = event.entity.hasVehicle();
            event.model.child = event.entity.isBaby();
            float h = MathHelper.lerpAngleDegrees(event.g, event.entity.prevBodyYaw, event.entity.bodyYaw);
            float j = MathHelper.lerpAngleDegrees(event.g, event.entity.prevHeadYaw, event.entity.headYaw);
            float k = j - h;
            if (event.entity.hasVehicle()) {
               Entity var8 = event.entity.getVehicle();
               if (var8 instanceof LivingEntity) {
                  LivingEntity livingEntity2 = (LivingEntity)var8;
                  h = MathHelper.lerpAngleDegrees(event.g, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
                  k = j - h;
                  float l = MathHelper.wrapDegrees(k);
                  if (l < -85.0F) {
                     l = -85.0F;
                  }

                  if (l >= 85.0F) {
                     l = 85.0F;
                  }

                  h = j - l;
                  if (l * l > 2500.0F) {
                     h += l * 0.2F;
                  }

                  k = j - h;
               }
            }

            float m = MathHelper.lerp(event.g, event.entity.prevPitch, event.entity.getPitch());
            if (LivingEntityRenderer.shouldFlipUpsideDown(event.entity)) {
               m *= -1.0F;
               k *= -1.0F;
            }

            Direction direction;
            if (event.entity.isInPose(EntityPose.SLEEPING) && (direction = event.entity.getSleepingDirection()) != null) {
               float n = event.entity.getEyeHeight(EntityPose.STANDING) - 0.1F;
               event.matrixStack.translate((float)(-direction.getOffsetX()) * n, 0.0F, (float)(-direction.getOffsetZ()) * n);
            }

            float l = this.getAnimationProgress(event.entity, event.g);
            if (event.entity instanceof PlayerEntity) {
               ChamsModelRenderer.setupPlayerTransforms((AbstractClientPlayerEntity)event.entity, event.matrixStack, l, h, event.g);
            } else {
               ChamsModelRenderer.setupTransforms(event.entity, event.matrixStack, l, h, event.g);
            }

            event.matrixStack.scale(-1.0F, -1.0F, 1.0F);
            event.matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
            event.matrixStack.translate(0.0F, -1.501F, 0.0F);
            float n = 0.0F;
            float o = 0.0F;
            if (!event.entity.hasVehicle() && event.entity.isAlive()) {
               n = event.entity.limbAnimator.getSpeed(event.g);
               o = event.entity.limbAnimator.getPos(event.g);
               if (event.entity.isBaby()) {
                  o *= 3.0F;
               }

               if (n > 1.0F) {
                  n = 1.0F;
               }
            }

            event.model.animateModel(event.entity, o, n, event.g);
            event.model.setAngles(event.entity, o, n, l, k, m);
            if (!event.entity.isSpectator()) {
               for(Object featureRenderer : event.features) {
                  ((FeatureRenderer)featureRenderer).render(event.matrixStack, event.vertexConsumerProvider, event.i, event.entity, o, n, event.g, l, k, m);
               }
            }

            event.matrixStack.pop();
         }
      }
   }

   @EventListener
   public void onRenderArm(RenderArmEvent event) {
      if (this.modeConfig.getValue() != ChamsModule.ChamsMode.NORMAL) {
         if ((Boolean)this.handsConfig.getValue() && !(Boolean)this.textureConfig.getValue()) {
            event.cancel();
         }

      }
   }

   public void renderEntityChams(MatrixStack matrixStack, Entity entity, float tickDelta) {
      int color1 = ((Color)this.colorConfig.getValue()).getRGB();
      int lineColor1 = ColorUtil.withAlpha(color1, 145);
      this.renderEntityChams(matrixStack, entity, tickDelta, color1, lineColor1);
   }

   public void renderEntityChams(MatrixStack matrixStack, Entity entity, float tickDelta, int color, int lineColor) {
      ChamsModelRenderer.render(matrixStack, entity, tickDelta, color, lineColor, (Float)this.widthConfig.getValue(), this.modeConfig.getValue() != ChamsModule.ChamsMode.FILL, this.modeConfig.getValue() != ChamsModule.ChamsMode.WIREFRAME, false);
   }

   private float getAnimationProgress(LivingEntity entity, float f) {
      if (entity instanceof SquidEntity) {
         return MathHelper.lerp(f, ((SquidEntity)entity).prevTentacleAngle, ((SquidEntity)entity).tentacleAngle);
      } else {
         float var10000;
         if (entity instanceof WolfEntity) {
            WolfEntity wolf = (WolfEntity)entity;
            var10000 = wolf.getTailAngle();
         } else {
            var10000 = (float)entity.age + f;
         }

         return var10000;
      }
   }

   public boolean checkChams(LivingEntity entity) {
      if (entity instanceof PlayerEntity) {
         if (entity != mc.player) {
            return (Boolean)this.playersConfig.getValue();
         } else {
            return (Boolean)this.selfConfig.getValue() && (!mc.options.getPerspective().isFirstPerson() || FreecamModule.getInstance().isEnabled());
         }
      } else {
         return EntityUtil.isMonster(entity) && (Boolean)this.monstersConfig.getValue() || (EntityUtil.isNeutral(entity) || EntityUtil.isPassive(entity)) && (Boolean)this.animalsConfig.getValue();
      }
   }

   public static enum ChamsMode {
      NORMAL,
      FILL,
      WIREFRAME,
      WIRE_FILL;

      // $FF: synthetic method
      private static ChamsMode[] $values() {
         return new ChamsMode[]{NORMAL, FILL, WIREFRAME, WIRE_FILL};
      }
   }

   public static class PopChamEntity extends FakePlayerEntity {
      private final StaticBipedEntityModel<AbstractClientPlayerEntity> model;

      public PopChamEntity(PlayerEntity player, float tickDelta) {
         super(player);
         this.model = new StaticBipedEntityModel<AbstractClientPlayerEntity>((AbstractClientPlayerEntity)player, false, tickDelta);
         this.leaningPitch = player.leaningPitch;
         this.lastLeaningPitch = player.leaningPitch;
         this.setPose(player.getPose());
      }

      public StaticBipedEntityModel<AbstractClientPlayerEntity> getModel() {
         return this.model;
      }
   }
}

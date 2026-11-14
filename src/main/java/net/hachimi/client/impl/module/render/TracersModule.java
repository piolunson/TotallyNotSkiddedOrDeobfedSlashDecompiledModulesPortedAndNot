package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorCamera;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.VertexSorter;
import org.joml.Matrix4f;

public class TracersModule extends ToggleModule {
   Config<RenderMode> modeConfig;
   Config<Target> targetConfig;
   Config<Float> widthConfig;
   Config<Boolean> playersConfig;
   Config<Color> playersColorConfig;
   Config<Boolean> friendsConfig;
   Config<Boolean> invisiblesConfig;
   Config<Color> invisiblesColorConfig;
   Config<Boolean> monstersConfig;
   Config<Color> monstersColorConfig;
   Config<Boolean> animalsConfig;
   Config<Color> animalsColorConfig;
   Config<Boolean> vehiclesConfig;
   Config<Color> vehiclesColorConfig;
   Config<Boolean> itemsConfig;
   Config<Color> itemsColorConfig;

   public TracersModule() {
      super("Tracers", "Draws a tracer to all entities in render distance", ModuleCategory.RENDER);
      this.modeConfig = this.register(new EnumConfig("Mode", "Render tracers to entities not visible on the screen", TracersModule.RenderMode.NORMAL, TracersModule.RenderMode.values()));
      this.targetConfig = this.register(new EnumConfig("Target", "The body part of the entity to target", TracersModule.Target.FEET, TracersModule.Target.values()));
      this.widthConfig = this.register(new NumberConfig("Width", "The line width of the tracer", 1.0F, 1.0F, 5.0F));
      this.playersConfig = this.register(new BooleanConfig("Players", "Render tracers to player", true));
      this.playersColorConfig = this.register(new ColorConfig("PlayersColor", "The render color for players", new Color(200, 60, 60), false, () -> this.playersConfig.getValue()));
      this.friendsConfig = this.register(new BooleanConfig("Friends", "Render tracers to players that are friended", false, () -> this.playersConfig.getValue()));
      this.invisiblesConfig = this.register(new BooleanConfig("Invisibles", "Render tracers to invisible entities", false));
      this.invisiblesColorConfig = this.register(new ColorConfig("InvisiblesColor", "The render color for invisibles", new Color(200, 100, 0), false, () -> this.invisiblesConfig.getValue()));
      this.monstersConfig = this.register(new BooleanConfig("Monsters", "Render tracers to monsters", false));
      this.monstersColorConfig = this.register(new ColorConfig("MonstersColor", "The render color for monsters", new Color(200, 60, 60), false, () -> this.monstersConfig.getValue()));
      this.animalsConfig = this.register(new BooleanConfig("Animals", "Render tracers to animals", false));
      this.animalsColorConfig = this.register(new ColorConfig("AnimalsColor", "The render color for animals", new Color(0, 200, 0), false, () -> this.animalsConfig.getValue()));
      this.vehiclesConfig = this.register(new BooleanConfig("Vehicles", "Render tracers to vehicles", false));
      this.vehiclesColorConfig = this.register(new ColorConfig("VehiclesColor", "The render color for vehicles", new Color(200, 100, 0), false, () -> this.vehiclesConfig.getValue()));
      this.itemsConfig = this.register(new BooleanConfig("Items", "Render tracers to items", false));
      this.itemsColorConfig = this.register(new ColorConfig("ItemsColor", "The render color for items", new Color(255, 255, 255), false, () -> this.itemsConfig.getValue()));
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.getCameraEntity() != null) {
         Entity var3 = mc.getCameraEntity();
         if (var3 instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)var3;
            if (!mc.options.hudHidden) {
               MatrixStack matrixStack = new MatrixStack();
               double d = (double)(Integer)mc.options.getFov().getValue();
               matrixStack.multiplyPositionMatrix(mc.gameRenderer.getBasicProjectionMatrix(d));
               Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
               RenderSystem.setProjectionMatrix(matrixStack.peek().getPositionMatrix(), VertexSorter.BY_DISTANCE);
               RenderBuffers.preRender();
               Vec3d playerPos = Interpolation.getRenderPosition(playerEntity, event.getTickDelta());
               Camera camera = mc.gameRenderer.getCamera();
               double eyeHeight = (double)MathHelper.lerp(event.getTickDelta(), ((AccessorCamera)(Object)camera).getLastCameraY(), ((AccessorCamera)(Object)camera).getCameraY());
               double x1 = playerEntity.getX() - playerPos.getX();
               double y1 = playerEntity.getY() - playerPos.getY() + eyeHeight;
               double z1 = playerEntity.getZ() - playerPos.getZ();
               float pitch = playerEntity.getPitch();
               float yaw = playerEntity.getYaw();
               if (FreecamModule.getInstance().isEnabled()) {
                  Vec3d pos1 = FreecamModule.getInstance().getCameraPosition();
                  Vec3d pos2 = Interpolation.getRenderPosition(pos1, FreecamModule.getInstance().getLastCameraPosition(), event.getTickDelta());
                  float[] rotations = FreecamModule.getInstance().getCameraRotations();
                  x1 = pos1.x - pos2.x;
                  y1 = pos1.y - pos2.y;
                  z1 = pos1.z - pos2.z;
                  yaw = rotations[0];
                  pitch = rotations[1];
               }

               Vec3d pos = (new Vec3d((double)0.0F, (double)0.0F, (double)1.0F)).rotateX(-((float)Math.toRadians((double)pitch))).rotateY(-((float)Math.toRadians((double)yaw))).add(new Vec3d(x1, y1, z1));

               for(Entity entity : mc.world.getEntities()) {
                  boolean var10000;
                  switch (((RenderMode)this.modeConfig.getValue()).ordinal()) {
                     case 0 -> var10000 = RenderManager.isFrustumVisible(entity.getBoundingBox());
                     case 1 -> var10000 = !RenderManager.isFrustumVisible(entity.getBoundingBox());
                     case 2 -> var10000 = true;
                     default -> throw new MatchException((String)null, (Throwable)null);
                  }

                  boolean shouldDraw = var10000;
                  if (entity != null && entity.isAlive() && entity != mc.player && shouldDraw && (!(entity instanceof PlayerEntity) || !Managers.SOCIAL.isFriend(entity.getDisplayName()) || (Boolean)this.friendsConfig.getValue())) {
                     Color color = this.getTracerColor(entity);
                     if (color != null) {
                        Vec3d entityPos = Interpolation.getRenderPosition(entity, event.getTickDelta());
                        double x2 = entity.getX() - entityPos.getX();
                        double y2 = entity.getY() - entityPos.getY();
                        double z2 = entity.getZ() - entityPos.getZ();
                        RenderManager.renderLine(event.getMatrices(), pos, (new Vec3d(x2, y2, z2)).add((double)0.0F, this.getTargetY(entity), (double)0.0F), (Float)this.widthConfig.getValue(), color.getRGB());
                     }
                  }
               }

               RenderBuffers.postRender();
               RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorter.BY_DISTANCE);
               return;
            }
         }
      }

   }

   private Color getTracerColor(Entity entity) {
      if (entity.isInvisible() && (Boolean)this.invisiblesConfig.getValue()) {
         return this.invisiblesColorConfig.getValue();
      } else {
         if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if ((Boolean)this.playersConfig.getValue()) {
               if (Managers.SOCIAL.isFriend(player.getName())) {
                  return SocialsModule.getInstance().getFriendColor();
               }

               return this.playersColorConfig.getValue();
            }
         }

         if (EntityUtil.isMonster(entity) && (Boolean)this.monstersConfig.getValue()) {
            return this.monstersColorConfig.getValue();
         } else if ((EntityUtil.isPassive(entity) || EntityUtil.isNeutral(entity)) && (Boolean)this.animalsConfig.getValue()) {
            return this.animalsColorConfig.getValue();
         } else if (EntityUtil.isVehicle(entity) && (Boolean)this.vehiclesConfig.getValue()) {
            return this.vehiclesColorConfig.getValue();
         } else {
            return entity instanceof ItemEntity && (Boolean)this.itemsConfig.getValue() ? (Color)this.itemsColorConfig.getValue() : null;
         }
      }
   }

   private double getTargetY(Entity entity) {
      double var10000;
      switch (((Target)this.targetConfig.getValue()).ordinal()) {
         case 0 -> var10000 = (double)0.0F;
         case 1 -> var10000 = (double)entity.getHeight() / (double)2.0F;
         case 2 -> var10000 = (double)entity.getEyeHeight(entity.getPose());
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public static enum RenderMode {
      ON_SCREEN,
      OFF_SCREEN,
      NORMAL;

      // $FF: synthetic method
      private static RenderMode[] $values() {
         return new RenderMode[]{ON_SCREEN, OFF_SCREEN, NORMAL};
      }
   }

   public static enum Target {
      FEET,
      TORSO,
      HEAD;

      // $FF: synthetic method
      private static Target[] $values() {
         return new Target[]{FEET, TORSO, HEAD};
      }
   }
}

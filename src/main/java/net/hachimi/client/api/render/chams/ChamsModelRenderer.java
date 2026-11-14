package net.hachimi.client.api.render.chams;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.model.StaticBipedEntityModel;
import net.hachimi.client.impl.module.misc.SwingModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.impl.module.render.CrystalModelModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorAnimalModel;
import net.hachimi.client.util.Globals;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.util.Arm;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.LlamaEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class ChamsModelRenderer implements Globals {
   private static final float SINE_45_DEGREES = (float)Math.sin((Math.PI / 4D));
   private static final MatrixStack matrices = new MatrixStack();
   private static final Vector4f pos1 = new Vector4f();
   private static final Vector4f pos2 = new Vector4f();
   private static final Vector4f pos3 = new Vector4f();
   private static final Vector4f pos4 = new Vector4f();

   public static void renderStaticPlayerModel(MatrixStack matrixStack, AbstractClientPlayerEntity entity, StaticBipedEntityModel playerModel, float tickDelta, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine) {
      double offsetX = playerModel.getX();
      double offsetY = playerModel.getY();
      double offsetZ = playerModel.getZ();
      matrices.push();
      EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);
      if (entityRenderer instanceof PlayerEntityRenderer renderer) {
         float animationProgress = renderer.getAnimationProgress(entity, tickDelta);
         setupPlayerTransforms(entity, matrices, animationProgress, entity.getBodyYaw(), tickDelta);
         matrices.scale(-1.0F, -1.0F, 1.0F);
         renderer.scale(entity, matrices, tickDelta);
         matrices.translate((double)0.0F, (double)-1.501F, (double)0.0F);
         render(matrixStack, playerModel.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         render(matrixStack, playerModel.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         render(matrixStack, playerModel.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         render(matrixStack, playerModel.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         render(matrixStack, playerModel.leftLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         render(matrixStack, playerModel.rightLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
      }

      matrices.pop();
   }

   public static void setupPlayerTransforms(AbstractClientPlayerEntity abstractClientPlayerEntity, MatrixStack matrixStack, float f, float g, float h) {
      float i = abstractClientPlayerEntity.getLeaningPitch(h);
      float j = abstractClientPlayerEntity.getPitch(h);
      if (abstractClientPlayerEntity.isFallFlying()) {
         setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
         float k = (float)abstractClientPlayerEntity.getFallFlyingTicks() + h;
         float l = MathHelper.clamp(k * k / 100.0F, 0.0F, 1.0F);
         if (!abstractClientPlayerEntity.isUsingRiptide()) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(l * (-90.0F - j)));
         }

         Vec3d vec3d = abstractClientPlayerEntity.getRotationVec(h);
         Vec3d vec3d2 = abstractClientPlayerEntity.lerpVelocity(h);
         double d = vec3d2.horizontalLengthSquared();
         double e = vec3d.horizontalLengthSquared();
         if (d > (double)0.0F && e > (double)0.0F) {
            double m = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
            double n = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation((float)(Math.signum(n) * Math.acos(m))));
         }
      } else if (i > 0.0F) {
         setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
         float k = abstractClientPlayerEntity.isTouchingWater() ? -90.0F - j : -90.0F;
         float l = MathHelper.lerp(i, 0.0F, k);
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(l));
         if (abstractClientPlayerEntity.isInSwimmingPose()) {
            matrixStack.translate(0.0F, -1.0F, 0.3F);
         }
      } else {
         setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
      }

   }

   public static void setupTransforms(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
      if (entity.isFrozen()) {
         bodyYaw += (float)(Math.cos((double)entity.age * (double)3.25F) * Math.PI * (double)0.4F);
      }

      if (!entity.isInPose(EntityPose.SLEEPING)) {
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
      }

      if (entity.deathTime > 0) {
         float f = ((float)entity.deathTime + tickDelta - 1.0F) / 20.0F * 1.6F;
         if ((f = MathHelper.sqrt(f)) > 1.0F) {
            f = 1.0F;
         }

         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 90.0F));
      } else if (entity.isUsingRiptide()) {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - entity.getPitch()));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)entity.age + tickDelta) * -75.0F));
      } else if (entity.isInPose(EntityPose.SLEEPING)) {
         Direction direction = entity.getSleepingDirection();
         float g = direction != null ? getYaw(direction) : bodyYaw;
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F));
      } else if (LivingEntityRenderer.shouldFlipUpsideDown(entity)) {
         matrices.translate(0.0F, entity.getHeight() + 0.1F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
      }

   }

   private static float getYaw(Direction direction) {
      float var10000;
      switch (direction) {
         case SOUTH -> var10000 = 90.0F;
         case WEST -> var10000 = 0.0F;
         case NORTH -> var10000 = 270.0F;
         case EAST -> var10000 = 180.0F;
         default -> var10000 = 0.0F;
      }

      return var10000;
   }

   public static void render(MatrixStack matrixStack, Entity entity, float tickDelta, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine) {
      double offsetX = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
      double offsetY = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
      double offsetZ = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
      matrices.push();
      EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);
      if (entityRenderer instanceof LivingEntityRenderer renderer) {
         LivingEntity livingEntity = (LivingEntity)entity;
         EntityModel<LivingEntity> model = renderer.getModel();
         if (entityRenderer instanceof PlayerEntityRenderer r) {
            PlayerEntityModel<AbstractClientPlayerEntity> playerModel = (PlayerEntityModel)r.getModel();
            playerModel.sneaking = entity.isInSneakingPose();
            net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose armPose = PlayerEntityRenderer.getArmPose((AbstractClientPlayerEntity)entity, Hand.MAIN_HAND);
            net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose armPose2 = PlayerEntityRenderer.getArmPose((AbstractClientPlayerEntity)entity, Hand.OFF_HAND);
            if (armPose.isTwoHanded()) {
               armPose2 = livingEntity.getOffHandStack().isEmpty() ? net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose.EMPTY : net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose.ITEM;
            }

            if (livingEntity.getMainArm() == Arm.RIGHT) {
               playerModel.rightArmPose = armPose;
               playerModel.leftArmPose = armPose2;
            } else {
               playerModel.rightArmPose = armPose2;
               playerModel.leftArmPose = armPose;
            }
         }

         model.handSwingProgress = livingEntity.getHandSwingProgress(tickDelta);
         model.riding = livingEntity.hasVehicle();
         model.child = livingEntity.isBaby();
         boolean rotating = entity == mc.player && Managers.ROTATION.isRotating();
         float bodyYaw = rotating ? Managers.ROTATION.getRotationYaw() : MathHelper.lerpAngleDegrees(tickDelta, livingEntity.prevBodyYaw, livingEntity.bodyYaw);
         float headYaw = rotating ? Managers.ROTATION.getRotationYaw() : MathHelper.lerpAngleDegrees(tickDelta, livingEntity.prevHeadYaw, livingEntity.headYaw);
         float yaw = headYaw - bodyYaw;
         if (livingEntity.hasVehicle()) {
            Entity var25 = livingEntity.getVehicle();
            if (var25 instanceof LivingEntity) {
               LivingEntity livingEntity2 = (LivingEntity)var25;
               bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
               yaw = headYaw - bodyYaw;
               float animationProgress = MathHelper.wrapDegrees(yaw);
               if (animationProgress < -85.0F) {
                  animationProgress = -85.0F;
               }

               if (animationProgress >= 85.0F) {
                  animationProgress = 85.0F;
               }

               bodyYaw = headYaw - animationProgress;
               if (animationProgress * animationProgress > 2500.0F) {
                  bodyYaw = (float)((double)bodyYaw + (double)animationProgress * 0.2);
               }

               yaw = headYaw - bodyYaw;
            }
         }

         float pitch = rotating ? Managers.ROTATION.getRotationPitch() : MathHelper.lerp(tickDelta, livingEntity.prevPitch, livingEntity.getPitch());
         float animationProgress = renderer.getAnimationProgress(livingEntity, tickDelta);
         float limbDistance = 0.0F;
         float limbAngle = 0.0F;
         if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
            limbDistance = livingEntity.limbAnimator.getSpeed(tickDelta);
            limbAngle = livingEntity.limbAnimator.getPos(tickDelta);
            if (livingEntity.isBaby()) {
               limbAngle *= 3.0F;
            }

            if (limbDistance > 1.0F) {
               limbDistance = 1.0F;
            }
         }

         model.animateModel(livingEntity, limbAngle, limbDistance, tickDelta);
         model.setAngles(livingEntity, limbAngle, limbDistance, animationProgress, yaw, pitch);
         renderer.setupTransforms(livingEntity, matrices, animationProgress, bodyYaw, tickDelta, 1.0F);
         matrices.scale(-1.0F, -1.0F, 1.0F);
         renderer.scale(livingEntity, matrices, tickDelta);
         matrices.translate((double)0.0F, (double)-1.501F, (double)0.0F);
         if (model instanceof AnimalModel m) {
            if (m.child) {
               matrices.push();
               if (((AccessorAnimalModel)(Object)m).hookGetHeadScaled()) {
                  float g = 1.5F / ((AccessorAnimalModel)(Object)m).hookGetInvertedChildHeadScale();
                  matrices.scale(g, g, g);
               }

               matrices.translate((double)0.0F, (double)(((AccessorAnimalModel)(Object)m).hookGetChildHeadYOffset() / 16.0F), (double)(((AccessorAnimalModel)(Object)m).hookGetChildHeadZOffset() / 16.0F));
               if (model instanceof net.minecraft.client.render.entity.model.BipedEntityModel) {
                  net.minecraft.client.render.entity.model.BipedEntityModel mo = (net.minecraft.client.render.entity.model.BipedEntityModel)model;
                  render(matrixStack, mo.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               } else {
                  ((AccessorAnimalModel)(Object)m).hookGetHeadParts().forEach((modelPart) -> render(matrixStack, modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
               }

               matrices.pop();
               matrices.push();
               float g = 1.0F / ((AccessorAnimalModel)(Object)m).hookGetInvertedChildBodyScale();
               matrices.scale(g, g, g);
               matrices.translate((double)0.0F, (double)(((AccessorAnimalModel)(Object)m).hookGetChildBodyYOffset() / 16.0F), (double)0.0F);
               if (model instanceof net.minecraft.client.render.entity.model.BipedEntityModel) {
                  net.minecraft.client.render.entity.model.BipedEntityModel mo = (net.minecraft.client.render.entity.model.BipedEntityModel)model;
                  render(matrixStack, mo.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                  render(matrixStack, mo.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                  render(matrixStack, mo.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                  render(matrixStack, mo.leftLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                  render(matrixStack, mo.rightLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               } else {
                  ((AccessorAnimalModel)(Object)m).hookGetBodyParts().forEach((modelPart) -> render(matrixStack, modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
               }

               matrices.pop();
            } else if (model instanceof net.minecraft.client.render.entity.model.BipedEntityModel) {
               net.minecraft.client.render.entity.model.BipedEntityModel mo = (net.minecraft.client.render.entity.model.BipedEntityModel)model;
               render(matrixStack, mo.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, mo.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, mo.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, mo.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, mo.leftLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, mo.rightLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            } else {
               ((AccessorAnimalModel)(Object)m).hookGetHeadParts().forEach((modelPart) -> render(matrixStack, modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
               ((AccessorAnimalModel)(Object)m).hookGetBodyParts().forEach((modelPart) -> render(matrixStack, modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
            }
         } else if (model instanceof SinglePartEntityModel m) {
            render(matrixStack, m.getPart(), offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         } else if (model instanceof CompositeEntityModel m) {
            m.getParts().forEach((modelPart) -> render(matrixStack, (net.minecraft.client.model.ModelPart)modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
         } else if (model instanceof LlamaEntityModel m) {
            if (m.child) {
               matrices.push();
               matrices.scale(0.71428573F, 0.64935064F, 0.7936508F);
               matrices.translate((double)0.0F, (double)1.3125F, (double)0.22F);
               render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               matrices.pop();
               matrices.push();
               matrices.scale(0.625F, 0.45454544F, 0.45454544F);
               matrices.translate((double)0.0F, (double)2.0625F, (double)0.0F);
               render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               matrices.pop();
               matrices.push();
               matrices.scale(0.45454544F, 0.41322312F, 0.45454544F);
               matrices.translate((double)0.0F, (double)2.0625F, (double)0.0F);
               render(matrixStack, m.rightHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               matrices.pop();
            } else {
               render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            }
         } else if (model instanceof RabbitEntityModel m) {
            if (m.child) {
               matrices.push();
               matrices.scale(0.56666666F, 0.56666666F, 0.56666666F);
               matrices.translate((double)0.0F, (double)1.375F, (double)0.125F);
               render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.nose, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               matrices.pop();
               matrices.push();
               matrices.scale(0.4F, 0.4F, 0.4F);
               matrices.translate((double)0.0F, (double)2.25F, (double)0.0F);
               render(matrixStack, m.leftHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.tail, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               matrices.pop();
            } else {
               matrices.push();
               matrices.scale(0.6F, 0.6F, 0.6F);
               matrices.translate((double)0.0F, (double)1.0F, (double)0.0F);
               render(matrixStack, m.leftHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.rightEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.leftEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.tail, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               render(matrixStack, m.nose, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
               matrices.pop();
            }
         }
      }

      if (entityRenderer instanceof EndCrystalEntityRenderer renderer) {
         EndCrystalEntity crystalEntity = (EndCrystalEntity)entity;
         matrices.push();
         float h = CrystalModelModule.getInstance().isEnabled() && !CrystalModelModule.getInstance().getBounce() ? -1.0F : EndCrystalEntityRenderer.getYOffset(crystalEntity, tickDelta);
         float j = ((float)crystalEntity.endCrystalAge + tickDelta) * (CrystalModelModule.getInstance().isEnabled() ? CrystalModelModule.getInstance().getSpin() : 1.0F) * 3.0F;
         matrices.push();
         if (CrystalModelModule.getInstance().isEnabled()) {
            float crystalScale = CrystalModelModule.getInstance().getScale();
            matrices.scale(crystalScale, crystalScale, crystalScale);
         }

         matrices.scale(2.0F, 2.0F, 2.0F);
         matrices.translate(0.0F, -0.5F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         matrices.translate((double)0.0F, (double)(1.5F + h / 2.0F), (double)0.0F);
         matrices.multiply((new Quaternionf()).setAngleAxis(((float)Math.PI / 3F), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         render(matrixStack, renderer.frame, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         matrices.scale(0.875F, 0.875F, 0.875F);
         matrices.multiply((new Quaternionf()).setAngleAxis(((float)Math.PI / 3F), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         render(matrixStack, renderer.frame, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         matrices.scale(0.875F, 0.875F, 0.875F);
         matrices.multiply((new Quaternionf()).setAngleAxis(((float)Math.PI / 3F), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         matrices.pop();
         matrices.pop();
      }

      matrices.pop();
   }

   public static void renderHand(MatrixStack matrixStack, float tickDelta, int lineColor, int color, float lineWidth, boolean lines, boolean fill, boolean shine) {
      if (mc.options.getPerspective().isFirstPerson() && !FreecamModule.getInstance().isEnabled()) {
         double d = (double)(Integer)mc.options.getFov().getValue();
         matrixStack.multiplyPositionMatrix(mc.gameRenderer.getBasicProjectionMatrix(d));
         matrixStack.loadIdentity();
         if ((Boolean)mc.options.getBobView().getValue()) {
            PlayerEntity playerEntity = (PlayerEntity)mc.getCameraEntity();
            float f = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
            float g = -(playerEntity.horizontalSpeed + f * tickDelta);
            float h = MathHelper.lerp(tickDelta, playerEntity.prevStrideDistance, playerEntity.strideDistance);
            matrixStack.translate(MathHelper.sin(g * (float)Math.PI) * h * 0.5F, -Math.abs(MathHelper.cos(g * (float)Math.PI) * h), 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(g * (float)Math.PI) * h * 3.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(g * (float)Math.PI - 0.2F) * h) * 5.0F));
         }

         float h1 = MathHelper.lerp(tickDelta, mc.player.lastRenderPitch, mc.player.renderPitch);
         float i1 = MathHelper.lerp(tickDelta, mc.player.lastRenderYaw, mc.player.renderYaw);
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((mc.player.getPitch(tickDelta) - h1) * 0.1F));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((mc.player.getYaw(tickDelta) - i1) * 0.1F));
         float f1 = mc.player.getHandSwingProgress(tickDelta);
         Hand hand = SwingModule.getInstance().isEnabled() ? SwingModule.getInstance().getPrevPreferredHand() : (Hand)MoreObjects.firstNonNull(mc.player.preferredHand, Hand.MAIN_HAND);
         ItemStack itemStack = mc.player.getMainHandStack();
         ItemStack itemStack2 = mc.player.getOffHandStack();
         boolean bl = itemStack.isOf(Items.BOW) || itemStack2.isOf(Items.BOW);
         boolean bl2 = itemStack.isOf(Items.CROSSBOW) || itemStack2.isOf(Items.CROSSBOW);
         HandRenderType handRenderType = ChamsModelRenderer.HandRenderType.RENDER_BOTH_HANDS;
         if (!bl && !bl2) {
            handRenderType = ChamsModelRenderer.HandRenderType.RENDER_BOTH_HANDS;
         } else if (mc.player.isUsingItem()) {
            ItemStack itemStack1 = mc.player.getActiveItem();
            Hand hand1 = mc.player.getActiveHand();
            if (!itemStack1.isOf(Items.BOW) && !itemStack1.isOf(Items.CROSSBOW)) {
               handRenderType = hand == Hand.MAIN_HAND && mc.player.getOffHandStack().isOf(Items.CROSSBOW) && CrossbowItem.isCharged(mc.player.getOffHandStack()) ? ChamsModelRenderer.HandRenderType.RENDER_MAIN_HAND_ONLY : ChamsModelRenderer.HandRenderType.RENDER_BOTH_HANDS;
            } else {
               handRenderType = ChamsModelRenderer.HandRenderType.shouldOnlyRender(hand1);
            }
         } else if (itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
            handRenderType = ChamsModelRenderer.HandRenderType.RENDER_MAIN_HAND_ONLY;
         }

         PlayerEntityRenderer playerEntityRenderer = (PlayerEntityRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player);
         if (handRenderType.renderMainHand) {
            boolean bl1 = hand == Hand.MAIN_HAND;
            boolean bl3 = mc.player.preferredHand == Hand.MAIN_HAND;
            float j = bl3 ? f1 : 0.0F;
            float k = 1.0F - MathHelper.lerp(tickDelta, mc.gameRenderer.firstPersonRenderer.prevEquipProgressMainHand, mc.gameRenderer.firstPersonRenderer.equipProgressMainHand);
            Arm arm = bl1 ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
            if (itemStack.isEmpty() && bl1 && !mc.player.isInvisible()) {
               renderFirstPersonItem(matrixStack, tickDelta, playerEntityRenderer, arm, j, k, lineColor, color, lineWidth, lines, fill, shine);
            }
         }

         if (handRenderType.renderOffHand) {
            boolean bl1 = hand == Hand.OFF_HAND;
            boolean bl3 = mc.player.preferredHand == Hand.MAIN_HAND;
            float j = bl3 ? f1 : 0.0F;
            float k = 1.0F - MathHelper.lerp(tickDelta, mc.gameRenderer.firstPersonRenderer.prevEquipProgressOffHand, mc.gameRenderer.firstPersonRenderer.equipProgressOffHand);
            Arm arm = bl1 ? mc.player.getMainArm().getOpposite() : mc.player.getMainArm();
            if (itemStack.isEmpty() && bl1 && !mc.player.isInvisible()) {
               renderFirstPersonItem(matrixStack, tickDelta, playerEntityRenderer, arm, j, k, lineColor, color, lineWidth, lines, fill, shine);
            }
         }

      }
   }

   public static void renderFirstPersonItem(MatrixStack matrixStack, float tickDelta, PlayerEntityRenderer playerEntityRenderer, Arm arm, float swingProgress, float equipProgress, int lineColor, int color, float lineWidth, boolean lines, boolean fill, boolean shine) {
      RenderSystem.disableDepthTest();
      double offsetX = mc.gameRenderer.getCamera().getPos().x;
      double offsetY = mc.gameRenderer.getCamera().getPos().y;
      double offsetZ = mc.gameRenderer.getCamera().getPos().z;
      matrices.push();
      boolean bl = arm != Arm.LEFT;
      float f = bl ? 1.0F : -1.0F;
      float g = MathHelper.sqrt(swingProgress);
      float h = -0.3F * MathHelper.sin(g * (float)Math.PI);
      float i = 0.4F * MathHelper.sin(g * ((float)Math.PI * 2F));
      float j = -0.4F * MathHelper.sin(swingProgress * (float)Math.PI);
      matrices.translate(f * (h + 0.64000005F), i + -0.6F + equipProgress * -0.6F, j + -0.71999997F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0F));
      float k = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
      float l = MathHelper.sin(g * (float)Math.PI);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * l * 70.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * k * -20.0F));
      matrices.translate(f * -1.0F, 3.6F, 3.5F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0F));
      matrices.translate(f * 5.6F, 0.0F, 0.0F);
      playerEntityRenderer.setModelPose(mc.player);
      PlayerEntityModel model = (PlayerEntityModel)playerEntityRenderer.getModel();
      model.handSwingProgress = 0.0F;
      model.sneaking = false;
      model.leaningPitch = 0.0F;
      model.setAngles(mc.player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      if (arm == Arm.RIGHT) {
         model.rightArm.pitch = 0.0F;
         render(matrixStack, model.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         model.rightSleeve.pitch = 0.0F;
      } else {
         model.leftArm.pitch = 0.0F;
         render(matrixStack, model.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         model.leftSleeve.pitch = 0.0F;
      }

      matrices.pop();
   }

   public static void render(MatrixStack matrixStack, net.minecraft.client.model.ModelPart part, double offsetX, double offsetY, double offsetZ, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine) {
      if (part.visible && (!part.cuboids.isEmpty() || !part.children.isEmpty())) {
         matrices.push();
         part.rotate(matrices);

         for(net.minecraft.client.model.ModelPart.Cuboid cuboid : part.cuboids) {
            renderModelPart(matrixStack, cuboid, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         }

         for(net.minecraft.client.model.ModelPart child : part.children.values()) {
            render(matrixStack, child, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
         }

         matrices.pop();
      }
   }

   private static void renderModelPart(MatrixStack matrixStack, net.minecraft.client.model.ModelPart.Cuboid cuboid, double offsetX, double offsetY, double offsetZ, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();

      for(net.minecraft.client.model.ModelPart.Quad quad : cuboid.sides) {
         pos1.set(quad.vertices[0].pos.x / 16.0F, quad.vertices[0].pos.y / 16.0F, quad.vertices[0].pos.z / 16.0F, 1.0F);
         pos1.mul(matrix);
         pos2.set(quad.vertices[1].pos.x / 16.0F, quad.vertices[1].pos.y / 16.0F, quad.vertices[1].pos.z / 16.0F, 1.0F);
         pos2.mul(matrix);
         pos3.set(quad.vertices[2].pos.x / 16.0F, quad.vertices[2].pos.y / 16.0F, quad.vertices[2].pos.z / 16.0F, 1.0F);
         pos3.mul(matrix);
         pos4.set(quad.vertices[3].pos.x / 16.0F, quad.vertices[3].pos.y / 16.0F, quad.vertices[3].pos.z / 16.0F, 1.0F);
         pos4.mul(matrix);
         if (fill) {
            if (shine) {
               RenderBuffers.TEXTURE_QUADS.begin(matrixStack);
               RenderSystem.setShaderTexture(0, ItemRenderer.ITEM_ENCHANTMENT_GLINT);
               RenderBuffers.TEXTURE_QUADS.color(color);
               RenderBuffers.TEXTURE_QUADS.vertexTex(offsetX + (double)pos1.x, offsetY + (double)pos1.y, offsetZ + (double)pos1.z, 0.0F, 0.0F).vertexTex(offsetX + (double)pos2.x, offsetY + (double)pos2.y, offsetZ + (double)pos2.z, 0.0F, 1.0F).vertexTex(offsetX + (double)pos3.x, offsetY + (double)pos3.y, offsetZ + (double)pos3.z, 1.0F, 1.0F).vertexTex(offsetX + (double)pos4.x, offsetY + (double)pos4.y, offsetZ + (double)pos4.z, 1.0F, 0.0F);
               RenderBuffers.TEXTURE_QUADS.end();
            } else {
               RenderBuffers.QUADS.begin(matrixStack);
               RenderBuffers.QUADS.color(color);
               RenderBuffers.QUADS.vertex(offsetX + (double)pos1.x, offsetY + (double)pos1.y, offsetZ + (double)pos1.z).vertex(offsetX + (double)pos2.x, offsetY + (double)pos2.y, offsetZ + (double)pos2.z).vertex(offsetX + (double)pos3.x, offsetY + (double)pos3.y, offsetZ + (double)pos3.z).vertex(offsetX + (double)pos4.x, offsetY + (double)pos4.y, offsetZ + (double)pos4.z);
               RenderBuffers.QUADS.end();
            }
         }

         if (lines) {
            RenderBuffers.LINES.begin(matrixStack);
            RenderSystem.lineWidth(lineWidth);
            RenderBuffers.LINES.color(lineColor);
            RenderBuffers.LINES.vertexLine(offsetX + (double)pos1.x, offsetY + (double)pos1.y, offsetZ + (double)pos1.z, offsetX + (double)pos2.x, offsetY + (double)pos2.y, offsetZ + (double)pos2.z);
            RenderBuffers.LINES.vertexLine(offsetX + (double)pos2.x, offsetY + (double)pos2.y, offsetZ + (double)pos2.z, offsetX + (double)pos3.x, offsetY + (double)pos3.y, offsetZ + (double)pos3.z);
            RenderBuffers.LINES.vertexLine(offsetX + (double)pos3.x, offsetY + (double)pos3.y, offsetZ + (double)pos3.z, offsetX + (double)pos4.x, offsetY + (double)pos4.y, offsetZ + (double)pos4.z);
            RenderBuffers.LINES.vertexLine(offsetX + (double)pos1.x, offsetY + (double)pos1.y, offsetZ + (double)pos1.z, offsetX + (double)pos1.x, offsetY + (double)pos1.y, offsetZ + (double)pos1.z);
            RenderBuffers.LINES.end();
         }
      }

   }

   private static enum HandRenderType {
      RENDER_BOTH_HANDS(true, true),
      RENDER_MAIN_HAND_ONLY(true, false),
      RENDER_OFF_HAND_ONLY(false, true);

      final boolean renderMainHand;
      final boolean renderOffHand;

      private HandRenderType(boolean renderMainHand, boolean renderOffHand) {
         this.renderMainHand = renderMainHand;
         this.renderOffHand = renderOffHand;
      }

      public static HandRenderType shouldOnlyRender(Hand hand) {
         return hand == Hand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
      }

      // $FF: synthetic method
      private static HandRenderType[] $values() {
         return new HandRenderType[]{RENDER_BOTH_HANDS, RENDER_MAIN_HAND_ONLY, RENDER_OFF_HAND_ONLY};
      }
   }
}

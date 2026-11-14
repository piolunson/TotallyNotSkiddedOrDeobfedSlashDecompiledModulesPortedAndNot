package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

public class TrajectoriesModule extends ToggleModule {
   public TrajectoriesModule() {
      super("Trajectories", "Renders the trajectory path of projectiles", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      ItemStack mainhand = mc.player.getMainHandStack();
      Item throwItem = mainhand.getItem();
      MatrixStack matrixStack = new MatrixStack();
      double d = (double)(Integer)mc.options.getFov().getValue();
      matrixStack.multiplyPositionMatrix(mc.gameRenderer.getBasicProjectionMatrix(d));
      Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
      RenderSystem.setProjectionMatrix(matrixStack.peek().getPositionMatrix(), VertexSorter.BY_DISTANCE);
      RenderBuffers.preRender();
      if ((throwItem instanceof BowItem || throwItem instanceof CrossbowItem || throwItem instanceof TridentItem) && mc.player.getItemUseTime() > 0 || this.isThrowableItem(throwItem)) {
         float yaw = mc.player.getYaw();
         double x = Interpolation.interpolateDouble(mc.player.prevX, mc.player.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
         double y = Interpolation.interpolateDouble(mc.player.prevY, mc.player.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
         double z = Interpolation.interpolateDouble(mc.player.prevZ, mc.player.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
         y = y + (double)mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
         if (throwItem == mc.player.getMainHandStack().getItem()) {
            x -= (double)(MathHelper.cos(yaw / 180.0F * (float)Math.PI) * 0.16F);
            z -= (double)(MathHelper.sin(yaw / 180.0F * (float)Math.PI) * 0.16F);
         } else {
            x += (double)(MathHelper.cos(yaw / 180.0F * (float)Math.PI) * 0.16F);
            z += (double)(MathHelper.sin(yaw / 180.0F * (float)Math.PI) * 0.16F);
         }

         float n = throwItem instanceof BowItem ? 1.0F : 0.4F;
         double motionX = (double)(-MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float)Math.PI) * n);
         double motionY = (double)(-MathHelper.sin((mc.player.getPitch() + (float)(!(throwItem instanceof SplashPotionItem) && !(throwItem instanceof LingeringPotionItem) && !(throwItem instanceof ExperienceBottleItem) ? 0 : -20)) / 180.0F * (float)Math.PI) * n);
         double motionZ = (double)(MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float)Math.PI) * n);
         float power = (float)mc.player.getItemUseTime() / 20.0F;
         power = (power * power + power * 2.0F) / 3.0F;
         if (power > 1.0F) {
            power = 1.0F;
         }

         float dist = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
         motionX /= (double)dist;
         motionY /= (double)dist;
         motionZ /= (double)dist;
         float pow = (throwItem instanceof BowItem ? power * 2.0F : (throwItem instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getProjectilePower(throwItem);
         motionX *= (double)pow;
         motionY *= (double)pow;
         motionZ *= (double)pow;
         if (!mc.player.isOnGround()) {
            motionY += mc.player.getVelocity().getY();
         }

         float size = !(throwItem instanceof BowItem) && !(throwItem instanceof CrossbowItem) ? 0.25F : 0.3F;
         boolean hasLanded = false;

         while(!hasLanded) {
            boolean landing = false;
            Vec3d lastPos = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            BlockPos blockPos = BlockPos.ofFloored(x, y, z);
            if (mc.world.getBlockState(blockPos).getBlock() == Blocks.WATER) {
               motionX *= 0.8;
               motionY *= 0.8;
               motionZ *= 0.8;
            } else {
               motionX *= 0.99;
               motionY *= 0.99;
               motionZ *= 0.99;
            }

            if (throwItem instanceof BowItem) {
               motionY -= (double)0.05F;
            } else if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
               motionY -= (double)0.05F;
            } else {
               motionY -= (double)0.03F;
            }

            Box projectileBox = new Box(x - (double)size, y - (double)size, z - (double)size, x + (double)size, y + (double)size, z + (double)size);

            for(Entity entity : mc.world.getOtherEntities((Entity)null, projectileBox)) {
               if (entity != mc.player && !this.isThrowableEntity(entity)) {
                  RenderManager.renderBox(event.getMatrices(), Interpolation.getInterpolatedEntityBox(entity), ColorsModule.getInstance().getRGB(40));
                  RenderManager.renderBoundingBox(event.getMatrices(), Interpolation.getInterpolatedEntityBox(entity), 1.5F, ColorsModule.getInstance().getRGB(145));
                  landing = true;
               }
            }

            Vec3d pos = new Vec3d(x, y, z);
            BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(lastPos, pos, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
            if (result != null && result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
               landing = true;
               Direction side = result.getSide();
               if (side != Direction.NORTH && side != Direction.SOUTH) {
                  if (side != Direction.WEST && side != Direction.EAST) {
                     RenderManager.renderSide(event.getMatrices(), x - (double)0.6F, y, z - (double)0.6F, x + (double)0.6F, y, z + (double)0.6F, side, ColorsModule.getInstance().getRGB(80));
                  } else {
                     RenderManager.renderSide(event.getMatrices(), x, y - (double)0.6F, z - (double)0.6F, x, y + (double)0.6F, z + (double)0.6F, side, ColorsModule.getInstance().getRGB(80));
                  }
               } else {
                  RenderManager.renderSide(event.getMatrices(), x - (double)0.6F, y - (double)0.6F, z, x + (double)0.6F, y + (double)0.6F, z, side, ColorsModule.getInstance().getRGB(80));
               }
            }

            if (y < (double)mc.world.getBottomY()) {
               break;
            }

            if (motionX != (double)0.0F || motionY != (double)0.0F || motionZ != (double)0.0F) {
               RenderManager.renderLine(event.getMatrices(), lastPos, pos, 1.5F, ColorsModule.getInstance().getRGB());
               if (landing) {
                  hasLanded = true;
               }
            }
         }
      }

      RenderBuffers.postRender();
      RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorter.BY_DISTANCE);
   }

   private float getProjectilePower(Item item) {
      if (!(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem)) {
         if (item instanceof ExperienceBottleItem) {
            return 0.5F;
         } else {
            return item instanceof TridentItem ? 2.0F : 1.5F;
         }
      } else {
         return 0.4F;
      }
   }

   private boolean isThrowableItem(Item item) {
      return item instanceof EnderPearlItem || item instanceof ExperienceBottleItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
   }

   private boolean isThrowableEntity(Entity entity) {
      return entity instanceof ExperienceBottleEntity || entity instanceof EnderPearlEntity || entity instanceof ExperienceOrbEntity || entity instanceof PotionEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity || entity instanceof SnowballEntity || entity instanceof EggEntity;
   }
}

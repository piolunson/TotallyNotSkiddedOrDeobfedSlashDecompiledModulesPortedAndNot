package net.hachimi.client.impl.module.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.api.render.satin.ManagedShaderEffect;
import net.hachimi.client.impl.event.EntityOutlineEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.GameJoinEvent;
import net.hachimi.client.impl.event.render.RenderShaderEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.render.entity.RenderCrystalEvent;
import net.hachimi.client.impl.event.render.entity.RenderEntityEvent;
import net.hachimi.client.impl.event.render.entity.RenderItemEvent;
import net.hachimi.client.impl.event.render.entity.RenderLabelEvent;
import net.hachimi.client.impl.event.render.item.RenderFirstPersonEvent;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorGameRenderer;
import net.hachimi.client.mixin.accessor.AccessorWorldRenderer;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.world.BlockUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class ShadersModule extends ToggleModule {
   private static ShadersModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig("RenderDistance", "The shader render range", 10.0F, 50.0F, 200.0F));
   Config<Boolean> textureConfig = this.register(new BooleanConfig("Texture", "Renders the entity model texture", true));
   Config<Boolean> outlineConfig = this.register(new BooleanConfig("Outline", "Adds an outline around the shader", true));
   Config<Float> lineWidthConfig = this.register(new NumberConfig("Width", "The outline width", 0.1F, 1.5F, 10.0F, () -> this.outlineConfig.getValue()));
   Config<Boolean> fastOutlineConfig = this.register(new BooleanConfig("FastOutline", "Faster outline calculations", false, () -> this.outlineConfig.getValue()));
   Config<Integer> qualityConfig = this.register(new NumberConfig("Quality", "The outline pixel quality", 2, 10, 32, () -> (Boolean)this.fastOutlineConfig.getValue() && (Boolean)this.outlineConfig.getValue()));
   Config<Integer> stepsConfig = this.register(new NumberConfig("Steps", "The number of steps for finding edges", 2, 4, 32, () -> (Boolean)this.fastOutlineConfig.getValue() && (Boolean)this.outlineConfig.getValue()));
   Config<Boolean> glowConfig = this.register(new BooleanConfig("Glow", "Glow outline", false, () -> this.outlineConfig.getValue()));
   Config<Float> glowRadiusConfig = this.register(new NumberConfig("GlowFactor", "The glow radius", 0.1F, 1.0F, 3.0F, () -> (Boolean)this.glowConfig.getValue() && (Boolean)this.outlineConfig.getValue()));
   Config<ShaderMode> modeConfig;
   Config<Color> gradientConfig;
   Config<Float> factorConfig;
   Config<Float> speedConfig;
   Config<Float> marbleFactorConfig;
   Config<Boolean> dotsConfig;
   Config<Integer> dotRadiusConfig;
   Config<Boolean> mixConfig;
   Config<Float> mixFactorConfig;
   Config<Float> rainbowFactorConfig;
   Config<Float> transparencyConfig;
   Config<Boolean> handsConfig;
   Config<Color> handsColorConfig;
   Config<Boolean> selfConfig;
   Config<Color> selfColorConfig;
   Config<Boolean> playersConfig;
   Config<Boolean> friendsConfig;
   Config<Color> playersColorConfig;
   Config<Boolean> monstersConfig;
   Config<Color> monstersColorConfig;
   Config<Boolean> animalsConfig;
   Config<Color> animalsColorConfig;
   Config<Boolean> itemsConfig;
   Config<Color> itemsColorConfig;
   Config<Boolean> otherConfig;
   Config<Color> crystalsColorConfig;
   Config<Boolean> projectilesConfig;
   Config<Color> projectilesColorConfig;
   Config<Boolean> chestsConfig;
   Config<Color> chestsColorConfig;
   Config<Boolean> echestsConfig;
   Config<Color> echestsColorConfig;
   Config<Boolean> shulkersConfig;
   Config<Color> shulkersColorConfig;
   private float shaderTime;
   private int textureId;
   private boolean ignoreEntityRender;

   public ShadersModule() {
      super("Shaders", "Renders shaders over entities", ModuleCategory.RENDER);
      this.modeConfig = this.register(new EnumConfig("Fill", "The shader mode", ShadersModule.ShaderMode.OFF, ShadersModule.ShaderMode.values()));
      this.gradientConfig = this.register(new ColorConfig("GradientColor", "The gradient color of the shader", new Color(1.0F, 1.0F, 1.0F), true, false, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.GRADIENT));
      this.factorConfig = this.register(new NumberConfig("Factor", "The gradient factor", 0.1F, 8.0F, 10.0F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.GRADIENT));
      this.speedConfig = this.register(new NumberConfig("Speed", "The gradient speed factor", 0.01F, 1.5F, 10.0F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.GRADIENT));
      this.marbleFactorConfig = this.register(new NumberConfig("Flow", "The marble speed factor", 0.001F, 0.003F, 0.01F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.MARBLE));
      this.dotsConfig = this.register(new BooleanConfig("Dots", "Hacker esp", false, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT));
      this.dotRadiusConfig = this.register(new NumberConfig("DotRadius", "Width between the dots", 5, 8, 16, () -> (Boolean)this.dotsConfig.getValue() && this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT));
      this.mixConfig = this.register(new BooleanConfig("Mix", "Mixes the image with the shader colors", true, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE));
      this.mixFactorConfig = this.register(new NumberConfig("MixFactor", "The mix image factor", 0.0F, 0.5F, 1.0F, () -> (Boolean)this.mixConfig.getValue() && this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE));
      this.rainbowFactorConfig = this.register(new NumberConfig("RainbowFactor", "The rainbow speed", 0.001F, 0.005F, 0.02F, () -> this.modeConfig.getValue() == ShadersModule.ShaderMode.RAINBOW));
      this.transparencyConfig = this.register(new NumberConfig("Opacity", "The transparency of the fill", 0.0F, 0.35F, 1.0F, () -> this.modeConfig.getValue() != ShadersModule.ShaderMode.OFF));
      this.handsConfig = this.register(new BooleanConfig("Hands", "Render shaders on first-person hands", true));
      this.handsColorConfig = this.register(new ColorConfig("HandsColor", "The color of the shader", new Color(0, 100, 255), false, () -> (Boolean)this.handsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.selfConfig = this.register(new BooleanConfig("Self", "Render shaders on the player", true));
      this.selfColorConfig = this.register(new ColorConfig("SelfColor", "The render color for self", new Color(200, 60, 60), false, () -> (Boolean)this.selfConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.playersConfig = this.register(new BooleanConfig("Players", "Render shaders on other players", true));
      this.friendsConfig = this.register(new BooleanConfig("FriendsColor", "Render shaders on friends", true));
      this.playersColorConfig = this.register(new ColorConfig("PlayersColor", "The render color for players", new Color(200, 60, 60), false, () -> (Boolean)this.playersConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.monstersConfig = this.register(new BooleanConfig("Monsters", "Render shaders on monsters", true));
      this.monstersColorConfig = this.register(new ColorConfig("MonstersColor", "The render color for monsters", new Color(200, 60, 60), false, () -> (Boolean)this.monstersConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.animalsConfig = this.register(new BooleanConfig("Animals", "Render shaders on animals", true));
      this.animalsColorConfig = this.register(new ColorConfig("AnimalsColor", "The render color for animals", new Color(0, 200, 0), false, () -> (Boolean)this.animalsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.itemsConfig = this.register(new BooleanConfig("Items", "Render shaders on items", true));
      this.itemsColorConfig = this.register(new ColorConfig("ItemsColor", "The render color for items", new Color(200, 100, 0), false, () -> (Boolean)this.itemsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.otherConfig = this.register(new BooleanConfig("Crystals", "Render shaders on crystals", true));
      this.crystalsColorConfig = this.register(new ColorConfig("CrystalsColor", "The render color for end crystals", new Color(200, 100, 200), false, () -> (Boolean)this.otherConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.projectilesConfig = this.register(new BooleanConfig("Projectiles", "Render shaders on projectiles", true));
      this.projectilesColorConfig = this.register(new ColorConfig("ProjectilesColor", "The render color for projectiles", new Color(200, 100, 200), false, () -> (Boolean)this.projectilesConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.chestsConfig = this.register(new BooleanConfig("Chests", "Render chests through walls", false));
      this.chestsColorConfig = this.register(new ColorConfig("ChestsColor", "The render color for chests", new Color(200, 200, 101), false, false, () -> (Boolean)this.chestsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.echestsConfig = this.register(new BooleanConfig("EnderChests", "Render ender chests through walls", false));
      this.echestsColorConfig = this.register(new ColorConfig("EnderChestsColor", "The render color for ender chests", new Color(155, 0, 200), false, false, () -> (Boolean)this.echestsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      this.shulkersConfig = this.register(new BooleanConfig("Shulkers", "Render shulkers through walls", false));
      this.shulkersColorConfig = this.register(new ColorConfig("ShulkersColor", "The render color for shulkers", new Color(200, 0, 106), false, false, () -> (Boolean)this.shulkersConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.RAINBOW));
      INSTANCE = this;
   }

   public static ShadersModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      if (this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE) {
         this.loadShaderImage();
      }

      this.ignoreEntityRender = false;
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (mc.player != null) {
         if (event.getConfig() == this.modeConfig && this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE && event.getStage() == StageEvent.EventStage.POST) {
            this.loadShaderImage();
         }

      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      if (this.modeConfig.getValue() == ShadersModule.ShaderMode.IMAGE) {
         this.loadShaderImage();
      }

   }

   @EventListener
   public void onRenderEntityWorld(RenderShaderEvent event) {
      float lineThickness = Math.max((Boolean)this.fastOutlineConfig.getValue() ? 0.3F : 1.0F, (Float)this.lineWidthConfig.getValue());
      switch (((ShaderMode)this.modeConfig.getValue()).ordinal()) {
         case 0:
         case 6:
            ManagedShaderEffect shaderEffect = Managers.SHADER.getFilledShaderEffect();
            if (shaderEffect == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect, () -> {
               shaderEffect.setUniformValue("sobel", 1);
               shaderEffect.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
               shaderEffect.setUniformValue("color", 1.0F, 1.0F, 1.0F, this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT ? (Float)this.transparencyConfig.getValue() : 0.0F);
               shaderEffect.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
               shaderEffect.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
               shaderEffect.setUniformValue("dots", (Boolean)this.dotsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.OFF ? 1 : 0);
               shaderEffect.setUniformValue("dotRadius", (Integer)this.dotRadiusConfig.getValue());
               shaderEffect.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
               shaderEffect.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
               shaderEffect.render(mc.getRenderTickCounter().getTickDelta(true));
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case 1:
            ManagedShaderEffect shaderEffect_dup = Managers.SHADER.getGradientShaderEffect();
            if (shaderEffect_dup == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect_dup, () -> {
               shaderEffect_dup.setUniformValue("sobel", 1);
               shaderEffect_dup.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup.setUniformValue("color", 1.0F, 1.0F, 1.0F, (Float)this.transparencyConfig.getValue());
               shaderEffect_dup.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
               shaderEffect_dup.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
               shaderEffect_dup.setUniformValue("color1", (float)((Color)this.gradientConfig.getValue()).getRed() / 255.0F, (float)((Color)this.gradientConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.gradientConfig.getValue()).getBlue() / 255.0F, (float)((Color)this.gradientConfig.getValue()).getAlpha() / 255.0F);
               shaderEffect_dup.setUniformValue("factor", (Float)this.factorConfig.getValue() * 10.0F);
               shaderEffect_dup.setUniformValue("time", this.shaderTime);
               shaderEffect_dup.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect_dup.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect_dup.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
               shaderEffect_dup.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
               shaderEffect_dup.render(mc.getRenderTickCounter().getTickDelta(true));
               this.shaderTime += (Float)this.speedConfig.getValue();
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case 2:
            ManagedShaderEffect shaderEffect_dup2 = Managers.SHADER.getGlowingShaderEffect();
            if (shaderEffect_dup2 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect_dup2, () -> {
               shaderEffect_dup2.setUniformValue("sobel", 1);
               shaderEffect_dup2.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup2.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup2.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
               shaderEffect_dup2.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
               shaderEffect_dup2.setUniformValue("color", 1.0F, 1.0F, 1.0F, (Float)this.transparencyConfig.getValue());
               shaderEffect_dup2.setUniformValue("time", this.shaderTime);
               shaderEffect_dup2.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect_dup2.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect_dup2.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
               shaderEffect_dup2.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
               shaderEffect_dup2.render(mc.getRenderTickCounter().getTickDelta(true));
               this.shaderTime += (Float)this.marbleFactorConfig.getValue();
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case 3:
            ManagedShaderEffect shaderEffect_dup3 = Managers.SHADER.getRainbowShaderEffect();
            if (shaderEffect_dup3 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect_dup3, () -> {
               shaderEffect_dup3.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup3.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup3.setUniformValue("color", 1.0F, 1.0F, 1.0F, (Float)this.transparencyConfig.getValue());
               shaderEffect_dup3.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
               shaderEffect_dup3.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
               shaderEffect_dup3.setUniformValue("time", this.shaderTime);
               shaderEffect_dup3.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect_dup3.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect_dup3.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
               shaderEffect_dup3.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
               shaderEffect_dup3.render(mc.getRenderTickCounter().getTickDelta(true));
               this.shaderTime += (Float)this.rainbowFactorConfig.getValue();
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case 4:
            ManagedShaderEffect shaderEffect_dup4 = Managers.SHADER.getBloomShaderEffect();
            if (shaderEffect_dup4 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect_dup4, () -> {
               shaderEffect_dup4.setUniformValue("sobel", 1);
               shaderEffect_dup4.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup4.setUniformValue("color", 1.0F, 1.0F, 1.0F, (Float)this.transparencyConfig.getValue());
               shaderEffect_dup4.render(mc.getRenderTickCounter().getTickDelta(true));
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
            break;
         case 5:
            ManagedShaderEffect shaderEffect_dup5 = Managers.SHADER.getImageShaderEffect();
            if (shaderEffect_dup5 == null) {
               return;
            }

            Managers.SHADER.applyShader(shaderEffect_dup5, () -> {
               GlStateManager._activeTexture(33985);
               GlStateManager._bindTexture(this.textureId);
               shaderEffect_dup5.setUniformValue("sobel", 1);
               shaderEffect_dup5.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
               shaderEffect_dup5.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
               shaderEffect_dup5.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
               shaderEffect_dup5.setUniformValue("imageTexture", 1);
               shaderEffect_dup5.setUniformValue("color", 1.0F, 1.0F, 1.0F, (Float)this.transparencyConfig.getValue());
               shaderEffect_dup5.setUniformValue("mixColor", (Boolean)this.mixConfig.getValue() ? 1 : 0);
               shaderEffect_dup5.setUniformValue("mixFactor", (Float)this.mixFactorConfig.getValue());
               shaderEffect_dup5.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
               shaderEffect_dup5.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
               shaderEffect_dup5.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
               shaderEffect_dup5.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
               shaderEffect_dup5.render(mc.getRenderTickCounter().getTickDelta(true));
            }, () -> this.renderEntities(event.getTickDelta(), event.getMatrices()));
      }

   }

   @EventListener
   public void onRenderLabel(RenderLabelEvent event) {
      if (event.getEntity() instanceof PlayerEntity && this.ignoreEntityRender) {
         event.cancel();
      }

   }

   @EventListener
   public void onRenderCrystal(RenderCrystalEvent event) {
      if (mc.player != null && !(Boolean)this.textureConfig.getValue() && (Boolean)this.otherConfig.getValue() && !this.ignoreEntityRender && mc.player.squaredDistanceTo(event.endCrystalEntity) <= ((NumberConfig)this.rangeConfig).getValueSq()) {
         event.cancel();
      }

   }

   @EventListener
   public void onRenderEntity(RenderEntityEvent event) {
      if (mc.player != null && !(Boolean)this.textureConfig.getValue() && this.checkShaders(event.entity) && !this.ignoreEntityRender && !(mc.player.squaredDistanceTo(event.entity) > ((NumberConfig)this.rangeConfig).getValueSq())) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderItem(RenderItemEvent event) {
      if (mc.player != null && !(Boolean)this.textureConfig.getValue() && (Boolean)this.itemsConfig.getValue() && !this.ignoreEntityRender && mc.player.squaredDistanceTo(event.getItem()) <= ((NumberConfig)this.rangeConfig).getValueSq()) {
         event.cancel();
      }

   }

   @EventListener
   public void onRenderArm(RenderFirstPersonEvent.Head event) {
      if (mc.player != null && !(Boolean)this.textureConfig.getValue() && (Boolean)this.handsConfig.getValue() && !this.ignoreEntityRender) {
         event.cancel();
      }
   }

   private void renderEntities(float tickDelta, MatrixStack matrixStack) {
      matrixStack.push();
      this.ignoreEntityRender = true;
      Iterator var3 = mc.world.getEntities().iterator();

      while(true) {
         while(true) {
            if (!var3.hasNext()) {
               VertexConsumerProvider vertexConsumerProvider = Managers.SHADER.createVertexConsumers(((AccessorWorldRenderer)(Object)mc.worldRenderer).hookGetBufferBuilders().getEntityVertexConsumers(), Color.BLUE);
               OtherClientPlayerEntity fakePlayerEntity = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.fromString("041f2043-a047-482e-a5b2-41c711badc42"), "nigger"));
               fakePlayerEntity.setPosition((double)0.0F, (double)-1.0E8F, (double)0.0F);
               fakePlayerEntity.setId(Integer.MAX_VALUE);
               EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(fakePlayerEntity);
               matrixStack.push();
               matrixStack.translate((double)0.0F, (double)-1.0E8F, (double)0.0F);
               entityRenderer.render(fakePlayerEntity, fakePlayerEntity.getYaw(), tickDelta, matrixStack, vertexConsumerProvider, 0);
               matrixStack.pop();
               this.ignoreEntityRender = false;
               matrixStack.pop();
               return;
            }

            Entity entity = (Entity)var3.next();
            if ((!NoRenderModule.getInstance().isEnabled() || !NoRenderModule.getInstance().shouldSkipEntity(entity)) && RenderManager.isFrustumVisible(entity.getBoundingBox())) {
               if (!this.checkShaders(entity)) {
                  break;
               }

               Vec3d start = FreecamModule.getInstance().isEnabled() ? FreecamModule.getInstance().getCameraPosition() : mc.player.getPos();
               if (!(start.squaredDistanceTo(entity.getPos()) > ((NumberConfig)this.rangeConfig).getValueSq())) {
                  Color color = this.getESPColor(entity);
                  Vec3d camera = mc.gameRenderer.getCamera().getPos();
                  double d = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
                  double e = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
                  double f = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
                  float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
                  EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(entity);
                  VertexConsumerProvider vertexConsumerProvider = Managers.SHADER.createVertexConsumers(((AccessorWorldRenderer)(Object)mc.worldRenderer).hookGetBufferBuilders().getEntityVertexConsumers(), color);
                  int light = mc.getEntityRenderDispatcher().getLight(entity, tickDelta);

                  try {
                     label69: {
                        label68: {
                           Vec3d vec3d = entityRenderer.getPositionOffset(entity, tickDelta);
                           double x = d - camera.x + vec3d.x;
                           double y = e - camera.y + vec3d.y;
                           double z = f - camera.z + vec3d.z;
                           matrixStack.push();
                           if (ChamsModule.getInstance().isEnabled()) {
                              if (entity instanceof LivingEntity) {
                                 LivingEntity entity1 = (LivingEntity)entity;
                                 if (ChamsModule.getInstance().checkChams(entity1)) {
                                    break label68;
                                 }
                              }

                              if (entity instanceof EndCrystalEntity && (Boolean)ChamsModule.getInstance().crystalsConfig.getValue()) {
                                 break label68;
                              }
                           }

                           matrixStack.translate(x, y, z);
                           entityRenderer.render(entity, g, tickDelta, matrixStack, vertexConsumerProvider, light);
                           matrixStack.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
                           break label69;
                        }

                        ChamsModule.getInstance().renderEntityChams(matrixStack, entity, tickDelta);
                     }

                     matrixStack.pop();
                  } catch (Exception exception) {
                     exception.printStackTrace();
                     Hachimi.error("Failed to render shader on entity!");
                  }
                  break;
               }
            }
         }

         RenderBuffers.postRender();
         if ((Boolean)this.echestsConfig.getValue() || (Boolean)this.chestsConfig.getValue() || (Boolean)this.shulkersConfig.getValue()) {
            for(BlockEntity blockEntity : BlockUtil.blockEntities()) {
               Color color = this.getStorageESPColor(blockEntity);
               VertexConsumerProvider vertexConsumerProvider = Managers.SHADER.createVertexConsumers(((AccessorWorldRenderer)(Object)mc.worldRenderer).hookGetBufferBuilders().getEntityVertexConsumers(), color);
               if (this.checkStorageShaders(blockEntity)) {
                  Vec3d vec3d = mc.gameRenderer.getCamera().getPos();
                  double d = vec3d.getX();
                  double e = vec3d.getY();
                  double g = vec3d.getZ();
                  BlockPos blockPos3 = blockEntity.getPos();
                  matrixStack.push();
                  matrixStack.translate((double)blockPos3.getX() - d, (double)blockPos3.getY() - e, (double)blockPos3.getZ() - g);
                  mc.getBlockEntityRenderDispatcher().render(blockEntity, tickDelta, matrixStack, vertexConsumerProvider);
                  matrixStack.pop();
               }
            }
         }
      }
   }

   @EventListener
   public void onReloadShader(RenderWorldEvent.Hand event) {
      if ((Boolean)this.handsConfig.getValue()) {
         float lineThickness = Math.max((Boolean)this.fastOutlineConfig.getValue() ? 0.3F : 1.0F, (Float)this.lineWidthConfig.getValue());
         switch (((ShaderMode)this.modeConfig.getValue()).ordinal()) {
            case 0:
            case 6:
               ManagedShaderEffect shaderEffect = Managers.SHADER.getFilledShaderEffect();
               if (shaderEffect == null) {
                  return;
               }

               Managers.SHADER.applyShader(shaderEffect, () -> {
                  shaderEffect.setUniformValue("sobel", 0);
                  shaderEffect.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
                  shaderEffect.setUniformValue("color", (float)((Color)this.handsColorConfig.getValue()).getRed() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getBlue() / 255.0F, this.modeConfig.getValue() == ShadersModule.ShaderMode.DEFAULT ? (Float)this.transparencyConfig.getValue() : 0.0F);
                  shaderEffect.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
                  shaderEffect.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
                  shaderEffect.setUniformValue("dots", (Boolean)this.dotsConfig.getValue() && this.modeConfig.getValue() != ShadersModule.ShaderMode.OFF ? 1 : 0);
                  shaderEffect.setUniformValue("dotRadius", (Integer)this.dotRadiusConfig.getValue());
                  shaderEffect.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
                  shaderEffect.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
                  shaderEffect.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
                  shaderEffect.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
                  shaderEffect.render(mc.getRenderTickCounter().getTickDelta(true));
               }, () -> {
                  this.ignoreEntityRender = true;
                  ((AccessorGameRenderer)(Object)mc.gameRenderer).hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                  this.ignoreEntityRender = false;
               });
               break;
            case 1:
               ManagedShaderEffect shaderEffect_dup6 = Managers.SHADER.getGradientShaderEffect();
               if (shaderEffect_dup6 == null) {
                  return;
               }

               Managers.SHADER.applyShader(shaderEffect_dup6, () -> {
                  shaderEffect_dup6.setUniformValue("sobel", 0);
                  shaderEffect_dup6.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup6.setUniformValue("color", (float)((Color)this.handsColorConfig.getValue()).getRed() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getBlue() / 255.0F, (Float)this.transparencyConfig.getValue());
                  shaderEffect_dup6.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
                  shaderEffect_dup6.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
                  shaderEffect_dup6.setUniformValue("color1", (float)((Color)this.gradientConfig.getValue()).getRed() / 255.0F, (float)((Color)this.gradientConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.gradientConfig.getValue()).getBlue() / 255.0F, (float)((Color)this.gradientConfig.getValue()).getAlpha() / 255.0F);
                  shaderEffect_dup6.setUniformValue("factor", (Float)this.factorConfig.getValue() * 10.0F);
                  shaderEffect_dup6.setUniformValue("time", this.shaderTime);
                  shaderEffect_dup6.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
                  shaderEffect_dup6.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
                  shaderEffect_dup6.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
                  shaderEffect_dup6.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
                  shaderEffect_dup6.render(mc.getRenderTickCounter().getTickDelta(true));
                  this.shaderTime += (Float)this.speedConfig.getValue();
               }, () -> {
                  this.ignoreEntityRender = true;
                  ((AccessorGameRenderer)(Object)mc.gameRenderer).hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                  this.ignoreEntityRender = false;
               });
               break;
            case 2:
               ManagedShaderEffect shaderEffect_dup7 = Managers.SHADER.getGlowingShaderEffect();
               if (shaderEffect_dup7 == null) {
                  return;
               }

               Managers.SHADER.applyShader(shaderEffect_dup7, () -> {
                  shaderEffect_dup7.setUniformValue("sobel", 0);
                  shaderEffect_dup7.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup7.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup7.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
                  shaderEffect_dup7.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
                  shaderEffect_dup7.setUniformValue("color", (float)((Color)this.handsColorConfig.getValue()).getRed() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getBlue() / 255.0F, (Float)this.transparencyConfig.getValue());
                  shaderEffect_dup7.setUniformValue("time", this.shaderTime);
                  shaderEffect_dup7.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
                  shaderEffect_dup7.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
                  shaderEffect_dup7.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
                  shaderEffect_dup7.render(mc.getRenderTickCounter().getTickDelta(true));
                  this.shaderTime += (Float)this.marbleFactorConfig.getValue();
               }, () -> {
                  this.ignoreEntityRender = true;
                  ((AccessorGameRenderer)(Object)mc.gameRenderer).hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                  this.ignoreEntityRender = false;
               });
               break;
            case 3:
               ManagedShaderEffect shaderEffect_dup8 = Managers.SHADER.getRainbowShaderEffect();
               if (shaderEffect_dup8 == null) {
                  return;
               }

               Managers.SHADER.applyShader(shaderEffect_dup8, () -> {
                  shaderEffect_dup8.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup8.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup8.setUniformValue("color", (float)((Color)this.handsColorConfig.getValue()).getRed() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getBlue() / 255.0F, (Float)this.transparencyConfig.getValue());
                  shaderEffect_dup8.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
                  shaderEffect_dup8.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
                  shaderEffect_dup8.setUniformValue("time", this.shaderTime);
                  shaderEffect_dup8.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
                  shaderEffect_dup8.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
                  shaderEffect_dup8.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
                  shaderEffect_dup8.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
                  shaderEffect_dup8.render(mc.getRenderTickCounter().getTickDelta(true));
                  this.shaderTime += (Float)this.rainbowFactorConfig.getValue();
               }, () -> {
                  this.ignoreEntityRender = true;
                  ((AccessorGameRenderer)(Object)mc.gameRenderer).hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                  this.ignoreEntityRender = false;
               });
               break;
            case 4:
               ManagedShaderEffect shaderEffect_dup9 = Managers.SHADER.getBloomShaderEffect();
               if (shaderEffect_dup9 == null) {
                  return;
               }

               Managers.SHADER.applyShader(shaderEffect_dup9, () -> {
                  shaderEffect_dup9.setUniformValue("sobel", 1);
                  shaderEffect_dup9.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup9.setUniformValue("color", (float)((Color)this.handsColorConfig.getValue()).getRed() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getBlue() / 255.0F, (Float)this.transparencyConfig.getValue());
                  shaderEffect_dup9.render(mc.getRenderTickCounter().getTickDelta(true));
               }, () -> {
                  this.ignoreEntityRender = true;
                  ((AccessorGameRenderer)(Object)mc.gameRenderer).hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                  this.ignoreEntityRender = false;
               });
               break;
            case 5:
               ManagedShaderEffect shaderEffect_dup10 = Managers.SHADER.getImageShaderEffect();
               if (shaderEffect_dup10 == null) {
                  return;
               }

               Managers.SHADER.applyShader(shaderEffect_dup10, () -> {
                  GlStateManager._activeTexture(33985);
                  GlStateManager._bindTexture(this.textureId);
                  shaderEffect_dup10.setUniformValue("sobel", 0);
                  shaderEffect_dup10.setUniformValue("texelSize", 1.0F / (float)mc.getWindow().getScaledWidth(), 1.0F / (float)mc.getWindow().getScaledHeight());
                  shaderEffect_dup10.setUniformValue("samples", (Integer)this.qualityConfig.getValue());
                  shaderEffect_dup10.setUniformValue("steps", (Integer)this.stepsConfig.getValue());
                  shaderEffect_dup10.setUniformValue("imageTexture", 1);
                  shaderEffect_dup10.setUniformValue("color", (float)((Color)this.handsColorConfig.getValue()).getRed() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getGreen() / 255.0F, (float)((Color)this.handsColorConfig.getValue()).getBlue() / 255.0F, (Float)this.transparencyConfig.getValue());
                  shaderEffect_dup10.setUniformValue("mixColor", (Boolean)this.mixConfig.getValue() ? 1 : 0);
                  shaderEffect_dup10.setUniformValue("mixFactor", (Float)this.mixFactorConfig.getValue());
                  shaderEffect_dup10.setUniformValue("fastOutline", (Boolean)this.fastOutlineConfig.getValue() ? 1 : 0);
                  shaderEffect_dup10.setUniformValue("radius", (Boolean)this.outlineConfig.getValue() ? lineThickness : 0.0F);
                  shaderEffect_dup10.setUniformValue("glow", (Boolean)this.glowConfig.getValue() ? 1 : 0);
                  shaderEffect_dup10.setUniformValue("glowRadius", (Float)this.glowRadiusConfig.getValue());
                  shaderEffect_dup10.render(mc.getRenderTickCounter().getTickDelta(true));
               }, () -> {
                  this.ignoreEntityRender = true;
                  ((AccessorGameRenderer)(Object)mc.gameRenderer).hookRenderHand(mc.gameRenderer.getCamera(), event.getTickDelta(), event.getMatrices().peek().getPositionMatrix());
                  this.ignoreEntityRender = false;
               });
         }

      }
   }

   @EventListener
   public void onEntityOutline(EntityOutlineEvent event) {
      if (mc.player == null || !this.checkShaders(event.getEntity()) || !(mc.player.squaredDistanceTo(event.getEntity()) > ((NumberConfig)this.rangeConfig).getValueSq())) {
         ;
      }
   }

   private void loadShaderImage() {
      try {
         ByteBuffer data = null;
         String[] fileFormats = new String[]{"png", "jpg"};

         for(String fileFormat : fileFormats) {
            File shaderFile = Hachimi.CONFIG.getClientDirectory().resolve("shader." + fileFormat).toFile();
            if (shaderFile.exists()) {
               FileInputStream fileInputStream = new FileInputStream(shaderFile);
               data = TextureUtil.readResource(fileInputStream);
               break;
            }

            Optional<Resource> optional = mc.getResourceManager().getResource(Identifier.of("hachimiclient", "shaders/shader." + fileFormat));
            if (!optional.isEmpty() && ((Resource)optional.get()).getInputStream() != null) {
               data = TextureUtil.readResource(((Resource)optional.get()).getInputStream());
               break;
            }
         }

         if (data != null) {
            data.rewind();
            MemoryStack stack = MemoryStack.stackPush();

            label79: {
               try {
                  IntBuffer width = stack.mallocInt(1);
                  IntBuffer height = stack.mallocInt(1);
                  IntBuffer comp = stack.mallocInt(1);
                  STBImage.stbi_set_flip_vertically_on_load(true);
                  ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);
                  if (image == null) {
                     break label79;
                  }

                  this.textureId = GlStateManager._genTexture();
                  GlStateManager._activeTexture(33984);
                  GlStateManager._bindTexture(this.textureId);
                  GlStateManager._pixelStore(3312, 0);
                  GlStateManager._pixelStore(3313, 0);
                  GlStateManager._pixelStore(3314, 0);
                  GlStateManager._pixelStore(32878, 0);
                  GlStateManager._pixelStore(3315, 0);
                  GlStateManager._pixelStore(3316, 0);
                  GlStateManager._pixelStore(32877, 0);
                  GlStateManager._pixelStore(3317, 4);
                  GlStateManager._texParameter(3553, 10242, 10497);
                  GlStateManager._texParameter(3553, 10243, 10497);
                  GlStateManager._texParameter(3553, 10241, 9728);
                  GlStateManager._texParameter(3553, 10240, 9728);
                  image.rewind();
                  GL32C.glTexImage2D(3553, 0, 6407, width.get(0), height.get(0), 0, 6407, 5121, image);
                  STBImage.stbi_image_free(image);
                  STBImage.stbi_set_flip_vertically_on_load(false);
               } catch (Throwable var10) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (stack != null) {
                  stack.close();
               }

               return;
            }

            if (stack != null) {
               stack.close();
            }

         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public Color getESPColor(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         if (entity == mc.player) {
            return this.selfColorConfig.getValue();
         } else {
            return (Boolean)this.friendsConfig.getValue() && Managers.SOCIAL.isFriend(player.getName()) ? SocialsModule.getInstance().getFriendColor() : (Color)this.playersColorConfig.getValue();
         }
      } else if (EntityUtil.isMonster(entity)) {
         return this.monstersColorConfig.getValue();
      } else if (!EntityUtil.isNeutral(entity) && !EntityUtil.isPassive(entity)) {
         if (entity instanceof EndCrystalEntity) {
            return this.crystalsColorConfig.getValue();
         } else if (entity instanceof ItemEntity) {
            return this.itemsColorConfig.getValue();
         } else {
            return !(entity instanceof ExperienceBottleEntity) && !(entity instanceof EnderPearlEntity) ? null : (Color)this.projectilesColorConfig.getValue();
         }
      } else {
         return this.animalsColorConfig.getValue();
      }
   }

   public Color getStorageESPColor(BlockEntity tileEntity) {
      if (tileEntity instanceof ChestBlockEntity chestBlockEntity) {
         return this.chestsColorConfig.getValue();
      } else if (tileEntity instanceof EnderChestBlockEntity) {
         return this.echestsColorConfig.getValue();
      } else {
         return tileEntity instanceof ShulkerBoxBlockEntity ? (Color)this.shulkersColorConfig.getValue() : null;
      }
   }

   public boolean checkShaders(Entity entity) {
      if (entity instanceof PlayerEntity && (Boolean)this.playersConfig.getValue()) {
         return (Boolean)this.selfConfig.getValue() && (!mc.options.getPerspective().isFirstPerson() || FreecamModule.getInstance().isEnabled()) || entity != mc.player;
      } else {
         return EntityUtil.isMonster(entity) && (Boolean)this.monstersConfig.getValue() || EntityUtil.isPassive(entity) && (Boolean)this.animalsConfig.getValue() || entity instanceof EndCrystalEntity && (Boolean)this.otherConfig.getValue() || entity instanceof ItemEntity && (Boolean)this.itemsConfig.getValue() || entity instanceof ExperienceBottleEntity && (Boolean)this.projectilesConfig.getValue() || entity instanceof EnderPearlEntity && (Boolean)this.projectilesConfig.getValue();
      }
   }

   private boolean checkStorageShaders(BlockEntity blockEntity) {
      return blockEntity instanceof ChestBlockEntity && (Boolean)this.chestsConfig.getValue() || blockEntity instanceof EnderChestBlockEntity && (Boolean)this.echestsConfig.getValue() || blockEntity instanceof ShulkerBoxBlockEntity && (Boolean)this.shulkersConfig.getValue();
   }

   private static enum ShaderMode {
      DEFAULT,
      GRADIENT,
      MARBLE,
      RAINBOW,
      BLOOM,
      IMAGE,
      OFF;

      // $FF: synthetic method
      private static ShaderMode[] $values() {
         return new ShaderMode[]{DEFAULT, GRADIENT, MARBLE, RAINBOW, BLOOM, IMAGE, OFF};
      }
   }
}

package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.MacroConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.macro.Macro;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.MouseUpdateEvent;
import net.hachimi.client.impl.event.PerspectiveEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.camera.CameraPositionEvent;
import net.hachimi.client.impl.event.camera.CameraRotationEvent;
import net.hachimi.client.impl.event.camera.EntityCameraPositionEvent;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.entity.EntityRotationVectorEvent;
import net.hachimi.client.impl.event.keyboard.KeyboardInputEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.render.BobViewEvent;
import net.hachimi.client.impl.event.render.item.RenderFirstPersonEvent;
import net.hachimi.client.impl.manager.player.rotation.Rotation;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.RayCastUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.hit.HitResult.Type;

public class FreecamModule extends ToggleModule {
   private static FreecamModule INSTANCE;
   Config<Float> speedConfig = this.register(new NumberConfig("Speed", "The move speed of the camera", 0.1F, 4.0F, 10.0F));
   Config<Macro> controlConfig = this.register(new MacroConfig("ControlKey", "", new Macro(this.getId() + "-control", 342, () -> {
   })));
   Config<Boolean> toggleControlConfig = this.register(new BooleanConfig("ToggleControl", "Allows toggling control key instead of holding", false));
   Config<Interact> interactConfig;
   Config<Boolean> rotateConfig;
   public Vec3d position;
   public Vec3d lastPosition;
   public float yaw;
   public float pitch;
   public boolean control;

   public FreecamModule() {
      super("Freecam", "Allows you to control the camera separately from the player", ModuleCategory.PLAYER);
      this.interactConfig = this.register(new EnumConfig("Interact", "The interaction type of the camera", FreecamModule.Interact.CAMERA, FreecamModule.Interact.values()));
      this.rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate to the point of interaction", false));
      this.control = false;
      INSTANCE = this;
   }

   public static FreecamModule getInstance() {
      return INSTANCE;
   }

   protected void onEnable() {
      if (mc.player != null) {
         this.control = false;
         this.position = mc.gameRenderer.getCamera().getPos();
         this.lastPosition = this.position;
         this.yaw = mc.player.getYaw();
         this.pitch = mc.player.getPitch();
         mc.player.input = new FreecamKeyboardInput(mc.options);
      }
   }

   protected void onDisable() {
      if (mc.player != null) {
         mc.player.input = new KeyboardInput(mc.options);
      }
   }

   @EventListener
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.disable();
      }

   }

   @EventListener
   public void onKey(KeyboardInputEvent event) {
      if (event.getAction() != 2 && event.getKeycode() == ((Macro)this.controlConfig.getValue()).getKeycode()) {
         if (!(Boolean)this.toggleControlConfig.getValue()) {
            this.control = event.getAction() == 1;
         } else if (event.getAction() == 1) {
            this.control = !this.control;
         }
      }

   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onCameraPosition(CameraPositionEvent event) {
      event.setPosition(this.control ? this.position : this.lastPosition.lerp(this.position, (double)event.getTickDelta()));
   }

   @EventListener
   public void onCameraRotation(CameraRotationEvent event) {
      event.setRotation(new Vec2f(this.yaw, this.pitch));
   }

   @EventListener
   public void onMouseUpdate(MouseUpdateEvent event) {
      if (!this.control) {
         event.cancel();
         this.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
      }

   }

   @EventListener
   public void onEntityCameraPosition(EntityCameraPositionEvent event) {
      if (event.getEntity() == mc.player) {
         if (!this.control && this.interactConfig.getValue() == FreecamModule.Interact.CAMERA) {
            event.setPosition(this.position);
         }

      }
   }

   @EventListener
   public void onEntityRotation(EntityRotationVectorEvent event) {
      if (event.getEntity() == mc.player) {
         if (!this.control && this.interactConfig.getValue() == FreecamModule.Interact.CAMERA) {
            event.setPosition(RotationUtil.getRotationVector(this.pitch, this.yaw));
         }

      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!this.control && (Boolean)this.rotateConfig.getValue()) {
            float[] currentAngles = new float[]{this.yaw, this.pitch};
            Vec3d eyePos = this.position;
            net.minecraft.util.hit.HitResult result = RayCastUtil.rayCast(mc.player.getBlockInteractionRange(), eyePos, currentAngles);
            if (result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
               float[] newAngles = RotationUtil.getRotationsTo(mc.player.getEyePos(), result.getPos());
               Managers.ROTATION.setRotation(new Rotation(1, newAngles[0], newAngles[1]));
            }
         }

      }
   }

   @EventListener
   public void onPerspective(PerspectiveEvent event) {
      event.cancel();
   }

   @EventListener
   public void onRenderArm(RenderFirstPersonEvent.Head event) {
      event.cancel();
   }

   @EventListener
   public void onBob(BobViewEvent event) {
      if (this.control) {
         event.cancel();
      }

   }

   private float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   private Vec2f handleVanillaMotion(float speed, float forward, float strafe) {
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F && strafe != 0.0F) {
            forward *= (float)Math.sin((Math.PI / 4D));
            strafe *= (float)Math.cos((Math.PI / 4D));
         }

         return new Vec2f((float)((double)(forward * speed) * -Math.sin(Math.toRadians((double)this.yaw)) + (double)(strafe * speed) * Math.cos(Math.toRadians((double)this.yaw))), (float)((double)(forward * speed) * Math.cos(Math.toRadians((double)this.yaw)) - (double)(strafe * speed) * -Math.sin(Math.toRadians((double)this.yaw))));
      }
   }

   private void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
      float f = (float)cursorDeltaY * 0.15F;
      float g = (float)cursorDeltaX * 0.15F;
      this.pitch += f;
      this.yaw += g;
      this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
   }

   public Vec3d getCameraPosition() {
      return this.position;
   }

   public Vec3d getLastCameraPosition() {
      return this.lastPosition;
   }

   public float[] getCameraRotations() {
      return new float[]{this.yaw, this.pitch};
   }

   public class FreecamKeyboardInput extends KeyboardInput {
      private final GameOptions options;

      public FreecamKeyboardInput(GameOptions options) {
         super(options);
         this.options = options;
      }

      public void tick(boolean slowDown, float slowDownFactor) {
         if (FreecamModule.this.control) {
            super.tick(slowDown, slowDownFactor);
         } else {
            this.unset();
            float speed = (Float)FreecamModule.this.speedConfig.getValue() / 10.0F;
            float fakeMovementForward = FreecamModule.this.getMovementMultiplier(this.options.forwardKey.isPressed(), this.options.backKey.isPressed());
            float fakeMovementSideways = FreecamModule.this.getMovementMultiplier(this.options.leftKey.isPressed(), this.options.rightKey.isPressed());
            Vec2f dir = FreecamModule.this.handleVanillaMotion(speed, fakeMovementForward, fakeMovementSideways);
            float y = 0.0F;
            if (this.options.jumpKey.isPressed()) {
               y += speed;
            } else if (this.options.sneakKey.isPressed()) {
               y -= speed;
            }

            FreecamModule.this.lastPosition = FreecamModule.this.position;
            FreecamModule.this.position = FreecamModule.this.position.add((double)dir.x, (double)y, (double)dir.y);
         }

      }

      private void unset() {
         this.pressingForward = false;
         this.pressingBack = false;
         this.pressingLeft = false;
         this.pressingRight = false;
         this.movementForward = 0.0F;
         this.movementSideways = 0.0F;
         this.jumping = false;
         this.sneaking = false;
      }
   }

   public static enum Interact {
      PLAYER,
      CAMERA;

      // $FF: synthetic method
      private static Interact[] $values() {
         return new Interact[]{PLAYER, CAMERA};
      }
   }
}

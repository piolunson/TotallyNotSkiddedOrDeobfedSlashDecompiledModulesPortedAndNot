package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.impl.module.misc.TimerModule;
import net.hachimi.client.impl.module.player.FreecamModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.client.util.math.MathUtil;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

public class SpeedModule extends ToggleModule {
   private static SpeedModule INSTANCE;
   Config<Speed> speedModeConfig;
   Config<Float> collisionDistanceConfig;
   Config<Float> speedConfig;
   Config<Boolean> timerConfig;
   Config<Boolean> fastConfig;
   Config<Boolean> strafeBoostConfig;
   Config<Integer> boostTicksConfig;
   Config<Boolean> speedWaterConfig;
   private int strafe;
   private boolean accel;
   private int strictTicks;
   private int strictFastTicks;
   private int boostTicks;
   private double speed;
   private double boostSpeed;
   private double distance;
   private boolean prevTimer;
   private static final float FRICTION = 159.077F;

   public SpeedModule() {
      super("Speed", "Move faster", ModuleCategory.MOVEMENT);
      this.speedModeConfig = this.register(new EnumConfig("Mode", "Speed mode", SpeedModule.Speed.STRAFE, SpeedModule.Speed.values()));
      this.collisionDistanceConfig = this.register(new NumberConfig("CollisionDistance", "The distance to apply collision speed", 0.5F, 1.5F, 2.0F, () -> this.speedModeConfig.getValue() == SpeedModule.Speed.GRIM_COLLIDE));
      this.speedConfig = this.register(new NumberConfig("Speed", "The speed for alternative modes", 0.1F, 4.0F, 50.0F, () -> this.speedModeConfig.getValue() == SpeedModule.Speed.VANILLA));
      this.timerConfig = this.register(new BooleanConfig("UseTimer", "Uses timer to increase acceleration", false, () -> this.isStrafe()));
      this.fastConfig = this.register(new BooleanConfig("Fast", "Fast timer speed", false, () -> this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_STRICT && (Boolean)this.timerConfig.getValue()));
      this.strafeBoostConfig = this.register(new BooleanConfig("StrafeBoost", "Uses explosion velocity to boost Strafe", false, () -> this.isStrafe()));
      this.boostTicksConfig = this.register(new NumberConfig("BoostTicks", "The number of ticks to boost strafe", 10, 20, 40, () -> this.isStrafe() && (Boolean)this.strafeBoostConfig.getValue()));
      this.speedWaterConfig = this.register(new BooleanConfig("SpeedInWater", "Applies speed even in water and lava", false));
      this.strafe = 4;
      INSTANCE = this;
   }

   public static SpeedModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return this.speedModeConfig.getValue() == SpeedModule.Speed.GRIM_COLLIDE ? "Grim" : EnumFormatter.formatEnum(this.speedModeConfig.getValue());
   }

   public void onEnable() {
      this.prevTimer = TimerModule.getInstance().isEnabled();
      if ((Boolean)this.timerConfig.getValue() && !this.prevTimer && this.isStrafe()) {
         TimerModule.getInstance().enable();
      }

   }

   public void onDisable() {
      this.resetStrafe();
      if (TimerModule.getInstance().isEnabled()) {
         TimerModule.getInstance().resetTimer();
         if (!this.prevTimer) {
            TimerModule.getInstance().disable();
         }
      }

   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         ++this.boostTicks;
         if (this.boostTicks > (Integer)this.boostTicksConfig.getValue()) {
            this.boostSpeed = (double)0.0F;
         }

         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.distance = Math.sqrt(dx * dx + dz * dz);
         if (this.speedModeConfig.getValue() == SpeedModule.Speed.GRIM_COLLIDE && MovementUtil.isInputtingMovement()) {
            int collisions = 0;

            for(Entity entity : mc.world.getEntities()) {
               if (this.checkIsCollidingEntity(entity) && MathHelper.sqrt((float)mc.player.squaredDistanceTo(entity)) <= (Float)this.collisionDistanceConfig.getValue()) {
                  ++collisions;
               }
            }

            if (collisions > 0) {
               Vec3d velocity = mc.player.getVelocity();
               double factor = 0.08 * (double)collisions;
               Vec2f strafe = this.handleStrafeMotion((float)factor);
               mc.player.setVelocity(velocity.x + (double)strafe.x, velocity.y, velocity.z + (double)strafe.y);
            }
         }
      }

   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (mc.player != null && mc.world != null && event.getType() == MovementType.SELF) {
         if (!MovementUtil.isInputtingMovement() || FlightModule.getInstance().isEnabled() || LongJumpModule.getInstance().isEnabled() || DisablerModule.getInstance().grimFireworkCheck() || FreecamModule.getInstance().isEnabled() && !FreecamModule.getInstance().control || mc.player.getAbilities().flying || mc.player.isRiding() || mc.player.isFallFlying() || mc.player.isHoldingOntoLadder() || mc.player.fallDistance > 2.0F || (mc.player.isInLava() || mc.player.isTouchingWater()) && !(Boolean)this.speedWaterConfig.getValue()) {
            this.resetStrafe();
            TimerModule.getInstance().setTimer(1.0F);
            return;
         }

         event.cancel();
         double speedEffect = (double)1.0F;
         double slowEffect = (double)1.0F;
         if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            speedEffect = (double)1.0F + 0.2 * (amplifier + (double)1.0F);
         }

         if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            slowEffect = (double)1.0F + 0.2 * (amplifier + (double)1.0F);
         }

         double base = (double)0.2873F * speedEffect / slowEffect;
         float jumpEffect = 0.0F;
         if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            jumpEffect += (float)(mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
         }

         if (this.speedModeConfig.getValue() != SpeedModule.Speed.STRAFE && this.speedModeConfig.getValue() != SpeedModule.Speed.STRAFE_B_HOP) {
            if (this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_STRICT) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  return;
               }

               if ((Boolean)this.timerConfig.getValue()) {
                  if ((Boolean)this.fastConfig.getValue()) {
                     ++this.strictFastTicks;
                     if (this.strictFastTicks > 10) {
                        this.strictFastTicks = 0;
                     }

                     float res = 1.0F + (float)this.strictFastTicks / 100.0F;
                     TimerModule.getInstance().setTimer(Math.max(1.0F, res));
                  } else {
                     TimerModule.getInstance().setTimer(1.0888F);
                  }
               }

               if (this.strafe == 1) {
                  this.speed = (double)1.35F * base - (double)0.01F;
               } else if (this.strafe == 2) {
                  if (mc.player.input.jumping || !mc.player.isOnGround()) {
                     return;
                  }

                  float jump = 0.39999995F + jumpEffect;
                  event.setY((double)jump);
                  Managers.MOVEMENT.setMotionY((double)jump);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
               } else {
                  if ((!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset((double)0.0F, mc.player.getVelocity().getY(), (double)0.0F)) || mc.player.verticalCollision) && this.strafe > 0) {
                     this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                  }

                  this.speed = this.distance - this.distance / (double)159.077F;
               }

               ++this.strictTicks;
               this.speed = Math.max(this.speed, base);
               if ((Boolean)this.timerConfig.getValue()) {
                  TimerModule.getInstance().setTimer(1.0888F);
               }

               double baseMax = 0.465 * speedEffect / slowEffect;
               double baseMin = 0.44 * speedEffect / slowEffect;
               this.speed = Math.min(this.speed, this.strictTicks > 25 ? baseMax : baseMin);
               if ((Boolean)this.strafeBoostConfig.getValue()) {
                  this.speed += this.boostSpeed;
               }

               if (this.strictTicks > 50) {
                  this.strictTicks = 0;
               }

               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX((double)motion.x);
               event.setZ((double)motion.y);
               ++this.strafe;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.LOW_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  return;
               }

               if ((Boolean)this.timerConfig.getValue()) {
                  TimerModule.getInstance().setTimer(1.0888F);
               }

               if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.4, 3)) {
                  Managers.MOVEMENT.setMotionY(0.31 + (double)jumpEffect);
                  event.setY(0.31 + (double)jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.71, 3)) {
                  Managers.MOVEMENT.setMotionY(0.04 + (double)jumpEffect);
                  event.setY(0.04 + (double)jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round((double)0.75F, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.2 - (double)jumpEffect);
                  event.setY(-0.2 - (double)jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.55, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.14 + (double)jumpEffect);
                  event.setY(-0.14 + (double)jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.41, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.2 + (double)jumpEffect);
                  event.setY(-0.2 + (double)jumpEffect);
               }

               if (this.strafe == 1) {
                  this.speed = (double)1.35F * base - (double)0.01F;
               } else if (this.strafe == 2) {
                  double jump = (this.isBoxColliding() ? 0.2 : 0.3999) + (double)jumpEffect;
                  Managers.MOVEMENT.setMotionY(jump);
                  event.setY(jump);
                  this.speed *= this.accel ? 1.5685 : 1.3445;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
                  this.accel = !this.accel;
               } else {
                  if (mc.player.isOnGround() && this.strafe > 0) {
                     this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                  }

                  this.speed = this.distance - this.distance / (double)159.077F;
               }

               this.speed = Math.max(this.speed, base);
               Vec2f motion = this.handleVanillaMotion((float)this.speed);
               event.setX((double)motion.x);
               event.setZ((double)motion.y);
               ++this.strafe;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.GAY_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  this.strafe = 1;
                  return;
               }

               if (this.strafe == 1 && mc.player.verticalCollision && MovementUtil.isInputtingMovement()) {
                  this.speed = (double)1.25F * base - (double)0.01F;
               } else if (this.strafe == 2 && mc.player.verticalCollision && MovementUtil.isInputtingMovement()) {
                  float jump = (this.isBoxColliding() ? 0.2F : 0.4F) + jumpEffect;
                  event.setY((double)jump);
                  Managers.MOVEMENT.setMotionY((double)jump);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
               } else {
                  if (mc.player.isOnGround() && this.strafe > 0) {
                     if (1.35 * base - 0.01 > this.speed) {
                        this.strafe = 0;
                     } else {
                        this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                     }
                  }

                  this.speed = this.distance - this.distance / (double)159.077F;
               }

               this.speed = Math.max(this.speed, base);
               if (this.strafe > 0) {
                  Vec2f motion = this.handleStrafeMotion((float)this.speed);
                  event.setX((double)motion.x);
                  event.setZ((double)motion.y);
               }

               ++this.strafe;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.V_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  this.strafe = 1;
                  return;
               }

               if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.4, 3)) {
                  Managers.MOVEMENT.setMotionY(0.31 + (double)jumpEffect);
                  event.setY(0.31 + (double)jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.71, 3)) {
                  Managers.MOVEMENT.setMotionY(0.04 + (double)jumpEffect);
                  event.setY(0.04 + (double)jumpEffect);
               } else if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round((double)0.75F, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.2 - (double)jumpEffect);
                  event.setY(-0.2 - (double)jumpEffect);
               }

               if (!mc.world.isSpaceEmpty((Entity)null, mc.player.getBoundingBox().offset((double)0.0F, -0.56, (double)0.0F)) && MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.55, 3)) {
                  Managers.MOVEMENT.setMotionY(-0.14 + (double)jumpEffect);
                  event.setY(-0.14 + (double)jumpEffect);
               }

               if (this.strafe != 1 || !mc.player.verticalCollision || mc.player.forwardSpeed == 0.0F && mc.player.sidewaysSpeed == 0.0F) {
                  if (this.strafe != 2 || !mc.player.verticalCollision || mc.player.forwardSpeed == 0.0F && mc.player.sidewaysSpeed == 0.0F) {
                     if (this.strafe == 3) {
                        double moveSpeed = 0.66 * (this.distance - base);
                        this.speed = this.distance - moveSpeed;
                     } else {
                        if (mc.player.isOnGround() && this.strafe > 0) {
                           if (1.35 * base - 0.01 > this.speed) {
                              this.strafe = 0;
                           } else {
                              this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                           }
                        }

                        this.speed = this.distance - this.distance / (double)159.077F;
                     }
                  } else {
                     double jump = (this.isBoxColliding() ? 0.2 : 0.4) + (double)jumpEffect;
                     Managers.MOVEMENT.setMotionY(jump);
                     event.setY(jump);
                     this.speed *= 2.149;
                  }
               } else {
                  this.speed = (double)2.0F * base - 0.01;
               }

               if (this.strafe > 8) {
                  this.speed = base;
               }

               this.speed = Math.max(this.speed, base);
               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX((double)motion.x);
               event.setZ((double)motion.y);
               ++this.strafe;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.B_HOP) {
               if (!Managers.ANTICHEAT.hasPassed(100L)) {
                  this.strafe = 4;
                  return;
               }

               if (MathUtil.round(mc.player.getY() - (double)((int)mc.player.getY()), 3) == MathUtil.round(0.138, 3)) {
                  Managers.MOVEMENT.setMotionY(mc.player.getVelocity().y - (0.08 + (double)jumpEffect));
                  event.setY(event.getY() - (0.0931 + (double)jumpEffect));
                  Managers.POSITION.setPositionY(mc.player.getY() - (0.0931 + (double)jumpEffect));
               }

               if (this.strafe != 2 || mc.player.forwardSpeed == 0.0F && mc.player.sidewaysSpeed == 0.0F) {
                  if (this.strafe == 3) {
                     double moveSpeed = 0.66 * (this.distance - base);
                     this.speed = this.distance - moveSpeed;
                  } else {
                     if (mc.player.isOnGround()) {
                        this.strafe = 1;
                     }

                     this.speed = this.distance - this.distance / (double)159.077F;
                  }
               } else {
                  double jump = (this.isBoxColliding() ? 0.2 : 0.4) + (double)jumpEffect;
                  Managers.MOVEMENT.setMotionY(jump);
                  event.setY(jump);
                  this.speed *= 2.149;
               }

               this.speed = Math.max(this.speed, base);
               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX((double)motion.x);
               event.setZ((double)motion.y);
               ++this.strafe;
            } else if (this.speedModeConfig.getValue() == SpeedModule.Speed.VANILLA) {
               Vec2f motion = this.handleStrafeMotion((Float)this.speedConfig.getValue() / 10.0F);
               event.setX((double)motion.x);
               event.setZ((double)motion.y);
            }
         } else {
            if (!Managers.ANTICHEAT.hasPassed(100L)) {
               return;
            }

            if ((Boolean)this.timerConfig.getValue()) {
               TimerModule.getInstance().setTimer(1.0888F);
            }

            if (this.strafe == 1) {
               this.speed = (double)1.35F * base - (double)0.01F;
            } else if (this.strafe == 2) {
               if (mc.player.input.jumping || !mc.player.isOnGround()) {
                  return;
               }

               float jump = (this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_B_HOP ? 0.4F : 0.39999995F) + jumpEffect;
               event.setY((double)jump);
               Managers.MOVEMENT.setMotionY((double)jump);
               this.speed *= this.speedModeConfig.getValue() == SpeedModule.Speed.STRAFE_B_HOP ? 1.535 : (this.accel ? 1.6835 : 1.395);
            } else if (this.strafe == 3) {
               double moveSpeed = 0.66 * (this.distance - base);
               this.speed = this.distance - moveSpeed;
               this.accel = !this.accel;
            } else {
               if ((!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset((double)0.0F, mc.player.getVelocity().getY(), (double)0.0F)) || mc.player.verticalCollision) && this.strafe > 0) {
                  this.strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
               }

               this.speed = this.distance - this.distance / (double)159.077F;
            }

            this.speed = Math.max(this.speed, base);
            if ((Boolean)this.strafeBoostConfig.getValue()) {
               this.speed += this.boostSpeed;
            }

            Vec2f motion = this.handleStrafeMotion((float)this.speed);
            event.setX((double)motion.x);
            event.setZ((double)motion.y);
            ++this.strafe;
         }
      }

   }

   public Vec2f handleStrafeMotion(float speed) {
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true);
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F) {
            if (strafe >= 1.0F) {
               yaw += forward > 0.0F ? -45.0F : 45.0F;
               strafe = 0.0F;
            } else if (strafe <= -1.0F) {
               yaw += forward > 0.0F ? 45.0F : -45.0F;
               strafe = 0.0F;
            }

            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         float rx = (float)Math.cos(Math.toRadians((double)yaw));
         float rz = (float)(-Math.sin(Math.toRadians((double)yaw)));
         return new Vec2f(forward * speed * rz + strafe * speed * rx, forward * speed * rx - strafe * speed * rz);
      }
   }

   public Vec2f handleVanillaMotion(float speed) {
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F && strafe != 0.0F) {
            forward *= (float)Math.sin((Math.PI / 4D));
            strafe *= (float)Math.cos((Math.PI / 4D));
         }

         return new Vec2f((float)((double)(forward * speed) * -Math.sin(Math.toRadians((double)mc.player.getYaw())) + (double)(strafe * speed) * Math.cos(Math.toRadians((double)mc.player.getYaw()))), (float)((double)(forward * speed) * Math.cos(Math.toRadians((double)mc.player.getYaw())) - (double)(strafe * speed) * -Math.sin(Math.toRadians((double)mc.player.getYaw()))));
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         Packet x = event.getPacket();
         if (x instanceof ExplosionS2CPacket) {
            ExplosionS2CPacket packet = (ExplosionS2CPacket)x;
            double velocityX = (double)packet.getPlayerVelocityX();
            double var6 = (double)packet.getPlayerVelocityZ();
         } else {
            x = event.getPacket();
            if (x instanceof EntityVelocityUpdateS2CPacket) {
               EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket)x;
               if (packet.getEntityId() == mc.player.getId()) {
                  double velocityX2 = packet.getVelocityX();
                  double var11 = packet.getVelocityZ();
                  return;
               }
            }

            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
               this.resetStrafe();
            }
         }

      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getConfig() == this.timerConfig && event.getStage() == StageEvent.EventStage.POST && this.isStrafe()) {
         if ((Boolean)this.timerConfig.getValue()) {
            this.prevTimer = TimerModule.getInstance().isEnabled();
            if (!this.prevTimer) {
               TimerModule.getInstance().enable();
            }
         } else if (TimerModule.getInstance().isEnabled()) {
            TimerModule.getInstance().resetTimer();
            if (!this.prevTimer) {
               TimerModule.getInstance().disable();
            }
         }
      }

   }

   public boolean isBoxColliding() {
      return !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset((double)0.0F, 0.21, (double)0.0F));
   }

   public boolean checkIsCollidingEntity(Entity entity) {
      return entity != null && entity != mc.player && entity instanceof LivingEntity && !(entity instanceof FakePlayerEntity) && !(entity instanceof ArmorStandEntity);
   }

   public void setPrevTimer() {
      this.prevTimer = !this.prevTimer;
   }

   public boolean isUsingTimer() {
      return this.isEnabled() && (Boolean)this.timerConfig.getValue();
   }

   public void resetStrafe() {
      this.strafe = 4;
      this.strictTicks = 0;
      this.strictFastTicks = 0;
      this.speed = (double)0.0F;
      this.distance = (double)0.0F;
      this.accel = false;
   }

   public boolean isStrafe() {
      return this.speedModeConfig.getValue() != SpeedModule.Speed.GRIM_COLLIDE && this.speedModeConfig.getValue() != SpeedModule.Speed.VANILLA;
   }

   private static enum Speed {
      STRAFE,
      STRAFE_STRICT,
      STRAFE_B_HOP,
      LOW_HOP,
      GAY_HOP,
      V_HOP,
      B_HOP,
      VANILLA,
      GRIM_COLLIDE;

      // $FF: synthetic method
      private static Speed[] $values() {
         return new Speed[]{STRAFE, STRAFE_STRICT, STRAFE_B_HOP, LOW_HOP, GAY_HOP, V_HOP, B_HOP, VANILLA, GRIM_COLLIDE};
      }
   }
}

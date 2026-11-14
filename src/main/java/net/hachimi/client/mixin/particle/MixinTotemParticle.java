package net.hachimi.client.mixin.particle;

import java.awt.Color;
import net.hachimi.client.impl.event.particle.TotemParticleEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.particle.TotemParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TotemParticle.class})
public abstract class MixinTotemParticle extends MixinParticle {
   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void hookInit(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider, CallbackInfo ci) {
      TotemParticleEvent totemParticleEvent = new TotemParticleEvent();
      EventBus.INSTANCE.dispatch(totemParticleEvent);
      if (totemParticleEvent.isCanceled()) {
         Color color = totemParticleEvent.getColor();
         this.setColor((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
      }

   }
}

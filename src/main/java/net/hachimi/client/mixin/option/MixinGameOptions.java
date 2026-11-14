package net.hachimi.client.mixin.option;

import com.mojang.serialization.Codec;
import java.io.File;
import net.hachimi.client.impl.event.option.PerspectiveUpdateEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameOptions.class})
public class MixinGameOptions {
   @Mutable
   @Shadow
   @Final
   private net.minecraft.client.option.SimpleOption<Integer> fov;

   @Inject(
      method = {"<init>"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/option/GameOptions;load()V",
   shift = Shift.BEFORE
)}
   )
   private void hookInit(MinecraftClient client, File optionsFile, CallbackInfo ci) {
      this.fov = new net.minecraft.client.option.SimpleOption<Integer>("options.fov", net.minecraft.client.option.SimpleOption.emptyTooltip(), (optionText, value) -> {
         Text var10000;
         switch (value) {
            case 70 -> var10000 = GameOptions.getGenericValueText(optionText, Text.translatable("options.fov.min"));
            case 110 -> var10000 = GameOptions.getGenericValueText(optionText, Text.translatable("options.fov.max"));
            default -> var10000 = GameOptions.getGenericValueText(optionText, value);
         }

         return var10000;
      }, new net.minecraft.client.option.SimpleOption.ValidatingIntSliderCallbacks(30, 180), Codec.DOUBLE.xmap((value) -> (int)(value * (double)40.0F + (double)70.0F), (value) -> ((double)value - (double)70.0F) / (double)40.0F), 70, (value) -> MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate());
   }

   @Inject(
      method = {"setPerspective"},
      at = {@At("HEAD")}
   )
   private void hookSetPerspective(Perspective perspective, CallbackInfo ci) {
      PerspectiveUpdateEvent perspectiveEvent = new PerspectiveUpdateEvent(perspective);
      EventBus.INSTANCE.dispatch(perspectiveEvent);
   }
}

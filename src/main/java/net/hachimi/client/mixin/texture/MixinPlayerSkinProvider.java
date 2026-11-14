package net.hachimi.client.mixin.texture;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({net.minecraft.client.texture.PlayerSkinProvider.FileCache.class})
public class MixinPlayerSkinProvider {
   @Shadow
   @Final
   private MinecraftProfileTexture.Type type;
}

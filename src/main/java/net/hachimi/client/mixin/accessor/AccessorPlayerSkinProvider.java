package net.hachimi.client.mixin.accessor;

import net.minecraft.client.texture.PlayerSkinProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({net.minecraft.client.texture.PlayerSkinProvider.class})
public interface AccessorPlayerSkinProvider {
   @Accessor("skinCache")
   net.minecraft.client.texture.PlayerSkinProvider.FileCache getSkinCacheDir();
}

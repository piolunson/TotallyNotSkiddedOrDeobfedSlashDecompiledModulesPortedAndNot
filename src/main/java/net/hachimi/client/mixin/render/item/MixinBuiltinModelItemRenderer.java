package net.hachimi.client.mixin.render.item;

import com.google.common.collect.Maps;
import java.util.Map;
import net.hachimi.client.api.render.layers.RenderLayersClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Util;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.block.SkullBlock;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.block.SkullBlock.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BuiltinModelItemRenderer.class})
public class MixinBuiltinModelItemRenderer {
   @Unique
   private static final Map<net.minecraft.block.SkullBlock.SkullType, Identifier> TEXTURES = (Map)Util.make(Maps.newHashMap(), (map) -> {
      map.put(net.minecraft.block.SkullBlock.Type.SKELETON, Identifier.of("textures/entity/skeleton/skeleton.png"));
      map.put(net.minecraft.block.SkullBlock.Type.WITHER_SKELETON, Identifier.of("textures/entity/skeleton/wither_skeleton.png"));
      map.put(net.minecraft.block.SkullBlock.Type.ZOMBIE, Identifier.of("textures/entity/zombie/zombie.png"));
      map.put(net.minecraft.block.SkullBlock.Type.CREEPER, Identifier.of("textures/entity/creeper/creeper.png"));
      map.put(net.minecraft.block.SkullBlock.Type.DRAGON, Identifier.of("textures/entity/enderdragon/dragon.png"));
      map.put(net.minecraft.block.SkullBlock.Type.PIGLIN, Identifier.of("textures/entity/piglin/piglin.png"));
      map.put(net.minecraft.block.SkullBlock.Type.PLAYER, DefaultSkinHelper.getTexture());
   });

   @Redirect(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/block/entity/SkullBlockEntityRenderer;getRenderLayer(Lnet/minecraft/block/SkullBlock$SkullType;Lnet/minecraft/component/type/ProfileComponent;)Lnet/minecraft/client/render/RenderLayer;"
)
   )
   public RenderLayer hookGetRenderLayer(net.minecraft.block.SkullBlock.SkullType type, ProfileComponent profile) {
      Identifier identifier = (Identifier)TEXTURES.get(type);
      if (type == net.minecraft.block.SkullBlock.Type.PLAYER && profile != null) {
         PlayerSkinProvider playerSkinProvider = MinecraftClient.getInstance().getSkinProvider();
         return (RenderLayer)RenderLayersClient.ENTITY_TRANSLUCENT.apply(playerSkinProvider.getSkinTextures(profile.gameProfile()).texture(), true);
      } else {
         return (RenderLayer)RenderLayersClient.ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(identifier, true);
      }
   }
}

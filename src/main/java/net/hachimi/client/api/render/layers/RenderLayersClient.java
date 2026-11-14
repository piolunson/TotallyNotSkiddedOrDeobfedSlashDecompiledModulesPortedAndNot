package net.hachimi.client.api.render.layers;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorRenderPhase;
import net.hachimi.client.util.Globals;
import net.minecraft.util.Util;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.lwjgl.opengl.GL11;

public class RenderLayersClient implements Globals {
   public static final Identifier SHULKER_BOXES_ATLAS_TEXTURE = Identifier.of("textures/atlas/shulker_boxes.png");
   public static final Identifier CHEST_ATLAS_TEXTURE = Identifier.of("textures/atlas/chest.png");
   public static final net.minecraft.client.render.RenderLayer GLINT;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> ENTITY_NO_OUTLINE;
   public static final BiFunction<Identifier, Boolean, net.minecraft.client.render.RenderLayer> ENTITY_TRANSLUCENT;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> ENTITY_SOLID;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> ITEM_ENTITY_TRANSLUCENT_CULL_2;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> ENTITY_TRANSLUCENT_CULL;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> ENTITY_CUTOUT;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> ENTITY_CUTOUT_NO_CULL;
   public static final BiFunction<Identifier, Boolean, net.minecraft.client.render.RenderLayer> ENTITY_CUTOUT_NO_CULL_Z_OFFSET;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> TEXT;
   public static final net.minecraft.client.render.RenderLayer TEXT_BACKGROUND;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> TEXT_INTENSITY;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> TEXT_POLYGON_OFFSET;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> TEXT_INTENSITY_POLYGON_OFFSET;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> TEXT_SEE_THROUGH;
   public static final net.minecraft.client.render.RenderLayer TEXT_BACKGROUND_SEE_THROUGH;
   public static final Function<Identifier, net.minecraft.client.render.RenderLayer> TEXT_INTENSITY_SEE_THROUGH;

   static {
      GLINT = net.minecraft.client.render.RenderLayer.of("glint", VertexFormats.POSITION_TEXTURE, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 256, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderPhase.GLINT_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(ItemRenderer.ITEM_ENCHANTMENT_GLINT, true, false)).writeMaskState(net.minecraft.client.render.RenderPhase.COLOR_MASK).cull(net.minecraft.client.render.RenderPhase.DISABLE_CULLING).depthTest(new DepthTest()).transparency(net.minecraft.client.render.RenderPhase.GLINT_TRANSPARENCY).texturing(net.minecraft.client.render.RenderPhase.GLINT_TEXTURING).build(false));
      ENTITY_NO_OUTLINE = Util.memoize((texture) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_NO_OUTLINE_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).cull(net.minecraft.client.render.RenderLayer.DISABLE_CULLING).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).writeMaskState(net.minecraft.client.render.RenderLayer.COLOR_MASK).build(false);
         return net.minecraft.client.render.RenderLayer.of("entity_no_outline", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, multiPhaseParameters);
      });
      ENTITY_TRANSLUCENT = Util.memoize((texture, affectsOutline) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_TRANSLUCENT_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).cull(net.minecraft.client.render.RenderLayer.DISABLE_CULLING).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).build(affectsOutline);
         return net.minecraft.client.render.RenderLayer.of("entity_translucent", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, true, multiPhaseParameters);
      });
      ENTITY_SOLID = Util.memoize((texture) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_SOLID_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.NO_TRANSPARENCY).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).build(true);
         return net.minecraft.client.render.RenderLayer.of("entity_solid", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, false, multiPhaseParameters);
      });
      ITEM_ENTITY_TRANSLUCENT_CULL_2 = Util.memoize((texture) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).target(net.minecraft.client.render.RenderLayer.ITEM_ENTITY_TARGET).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).writeMaskState(net.minecraft.client.render.RenderPhase.ALL_MASK).build(true);
         return net.minecraft.client.render.RenderLayer.of("item_entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, true, multiPhaseParameters);
      });
      ENTITY_TRANSLUCENT_CULL = Util.memoize((texture) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_TRANSLUCENT_CULL_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).build(true);
         return net.minecraft.client.render.RenderLayer.of("entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, true, multiPhaseParameters);
      });
      ENTITY_CUTOUT = Util.memoize((texture) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_CUTOUT_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.NO_TRANSPARENCY).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).build(true);
         return net.minecraft.client.render.RenderLayer.of("entity_cutout", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, false, multiPhaseParameters);
      });
      ENTITY_CUTOUT_NO_CULL = Util.memoize((texture) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_CUTOUT_NONULL_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.NO_TRANSPARENCY).cull(net.minecraft.client.render.RenderLayer.DISABLE_CULLING).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).build(true);
         return net.minecraft.client.render.RenderLayer.of("entity_cutout_no_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, false, multiPhaseParameters);
      });
      ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize((texture, affectsOutline) -> {
         net.minecraft.client.render.RenderLayer.MultiPhaseParameters multiPhaseParameters = net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.ENTITY_CUTOUT_NONULL_OFFSET_Z_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.NO_TRANSPARENCY).cull(net.minecraft.client.render.RenderLayer.DISABLE_CULLING).lightmap(new Lightmap()).overlay(net.minecraft.client.render.RenderLayer.ENABLE_OVERLAY_COLOR).layering(net.minecraft.client.render.RenderLayer.VIEW_OFFSET_Z_LAYERING).build(affectsOutline);
         return net.minecraft.client.render.RenderLayer.of("entity_cutout_no_cull_z_offset", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, true, false, multiPhaseParameters);
      });
      TEXT = Util.memoize((texture) -> net.minecraft.client.render.RenderLayer.of("text", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 786432, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TEXT_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).build(false)));
      TEXT_BACKGROUND = net.minecraft.client.render.RenderLayer.of("text_background", VertexFormats.POSITION_COLOR_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TEXT_BACKGROUND_PROGRAM).texture(net.minecraft.client.render.RenderLayer.NO_TEXTURE).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).build(false));
      TEXT_INTENSITY = Util.memoize((texture) -> net.minecraft.client.render.RenderLayer.of("text_intensity", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 786432, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TEXT_INTENSITY_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).build(false)));
      TEXT_POLYGON_OFFSET = Util.memoize((texture) -> net.minecraft.client.render.RenderLayer.of("text_polygon_offset", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TEXT_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).layering(net.minecraft.client.render.RenderLayer.POLYGON_OFFSET_LAYERING).build(false)));
      TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize((texture) -> net.minecraft.client.render.RenderLayer.of("text_intensity_polygon_offset", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TEXT_INTENSITY_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).layering(net.minecraft.client.render.RenderLayer.POLYGON_OFFSET_LAYERING).build(false)));
      TEXT_SEE_THROUGH = Util.memoize((texture) -> net.minecraft.client.render.RenderLayer.of("text_see_through", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TRANSPARENT_TEXT_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).depthTest(net.minecraft.client.render.RenderLayer.ALWAYS_DEPTH_TEST).writeMaskState(net.minecraft.client.render.RenderLayer.COLOR_MASK).build(false)));
      TEXT_BACKGROUND_SEE_THROUGH = net.minecraft.client.render.RenderLayer.of("text_background_see_through", VertexFormats.POSITION_COLOR_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TRANSPARENT_TEXT_BACKGROUND_PROGRAM).texture(net.minecraft.client.render.RenderLayer.NO_TEXTURE).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).depthTest(net.minecraft.client.render.RenderLayer.ALWAYS_DEPTH_TEST).writeMaskState(net.minecraft.client.render.RenderLayer.COLOR_MASK).build(false));
      TEXT_INTENSITY_SEE_THROUGH = Util.memoize((texture) -> net.minecraft.client.render.RenderLayer.of("text_intensity_see_through", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, net.minecraft.client.render.VertexFormat.DrawMode.QUADS, 1536, false, true, net.minecraft.client.render.RenderLayer.MultiPhaseParameters.builder().program(net.minecraft.client.render.RenderLayer.TRANSPARENT_TEXT_INTENSITY_PROGRAM).texture(new net.minecraft.client.render.RenderPhase.Texture(texture, false, false)).transparency(net.minecraft.client.render.RenderLayer.TRANSLUCENT_TRANSPARENCY).lightmap(new Lightmap()).depthTest(net.minecraft.client.render.RenderLayer.ALWAYS_DEPTH_TEST).writeMaskState(net.minecraft.client.render.RenderLayer.COLOR_MASK).build(false)));
   }

   protected static class DepthTest extends net.minecraft.client.render.RenderPhase.DepthTest {
      public DepthTest() {
         super("depth_test", 519);
      }

      public void startDrawing() {
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDepthFunc(514);
      }

      public void endDrawing() {
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glDepthFunc(515);
         GL11.glDepthFunc(519);
      }
   }

   protected static class Lightmap extends net.minecraft.client.render.RenderPhase.Lightmap {
      public Lightmap() {
         super(false);
         ((AccessorRenderPhase)(Object)this).hookSetBeginAction(() -> Managers.LIGHT_MAP.enable());
         ((AccessorRenderPhase)(Object)this).hookSetEndAction(() -> Managers.LIGHT_MAP.disable());
      }
   }
}

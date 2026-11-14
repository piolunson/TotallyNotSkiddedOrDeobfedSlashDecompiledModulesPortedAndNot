package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.gui.RenderTooltipEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.util.world.ItemUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.ItemStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.DataComponentTypes;

public class TooltipsModule extends ToggleModule {
   Config<Boolean> shulkersConfig = this.register(new BooleanConfig("Shulkers", "Renders all the contents of shulkers in tooltips", true));
   Config<Boolean> mapsConfig = this.register(new BooleanConfig("Maps", "Renders a preview of maps in tooltips", false));

   public TooltipsModule() {
      super("Tooltips", "Renders detailed tooltips showing items", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderTooltip(RenderTooltipEvent event) {
      ItemStack stack = event.getStack();
      if (!stack.isEmpty()) {
         if ((Boolean)this.shulkersConfig.getValue() && ItemUtil.isShulker(stack.getItem())) {
            ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
            if (container == null || container.streamNonEmpty().findAny().isEmpty()) {
               return;
            }

            event.cancel();
            event.context.getMatrices().push();
            event.context.getMatrices().translate(0.0F, 0.0F, 600.0F);
            DefaultedList<ItemStack> shulkerInventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.stream().filter((itemStackx) -> !itemStackx.isEmpty()).forEach((itemStackx) -> {
               for(int i = 0; i < shulkerInventory.size(); ++i) {
                  if (((ItemStack)shulkerInventory.get(i)).isEmpty()) {
                     shulkerInventory.set(i, itemStackx.copy());
                     break;
                  }
               }

            });
            RenderManager.rect(event.context.getMatrices(), (double)event.getX() + (double)8.0F, (double)event.getY() - (double)21.0F, (double)150.0F, (double)13.0F, ColorsModule.getInstance().getRGB(170));
            RenderManager.enableScissor((double)event.getX() + (double)8.0F, (double)event.getY() - (double)21.0F, (double)event.getX() + (double)158.0F, (double)event.getY() - (double)8.0F);
            RenderManager.renderText(event.getContext(), stack.getName().getString(), (float)event.getX() + 11.0F, (float)event.getY() - 18.0F, -1);
            RenderManager.disableScissor();
            RenderManager.rect(event.context.getMatrices(), (double)event.getX() + (double)8.0F, (double)event.getY() - (double)8.0F, (double)150.0F, (double)55.0F, 1996488704);

            for(int i = 0; i < shulkerInventory.size(); ++i) {
               ItemStack itemStack = (ItemStack)shulkerInventory.get(i);
               if (!itemStack.isEmpty()) {
                  int x = event.getX() + i % 9 * 16 + 9;
                  int y = event.getY() + i / 9 * 16 - 5;
                  event.context.drawItem(itemStack, x, y);
                  event.context.drawItemInSlot(mc.textRenderer, itemStack, x, y);
               }
            }

            event.context.getMatrices().pop();
         } else if ((Boolean)this.mapsConfig.getValue() && stack.getItem() instanceof FilledMapItem) {
            event.cancel();
            event.context.getMatrices().push();
            event.context.getMatrices().translate(0.0F, 0.0F, 600.0F);
            RenderManager.rect(event.context.getMatrices(), (double)event.getX() + (double)8.0F, (double)event.getY() - (double)21.0F, (double)128.0F, (double)13.0F, ColorsModule.getInstance().getRGB(170));
            RenderManager.enableScissor((double)event.getX() + (double)8.0F, (double)event.getY() - (double)21.0F, (double)event.getX() + (double)132.0F, (double)event.getY() - (double)8.0F);
            RenderManager.renderText(event.getContext(), stack.getName().getString(), (float)event.getX() + 11.0F, (float)event.getY() - 18.0F, -1);
            RenderManager.disableScissor();
            event.getContext().getMatrices().translate((float)event.getX() + 8.0F, (float)event.getY() - 8.0F, 0.0F);
            MapIdComponent mapIdComponent = (MapIdComponent)stack.get(DataComponentTypes.MAP_ID);
            MapState mapState = FilledMapItem.getMapState(mapIdComponent, mc.world);
            if (mapState != null) {
               mc.gameRenderer.getMapRenderer().draw(event.getContext().getMatrices(), event.getContext().getVertexConsumers(), mapIdComponent, mapState, true, 1);
            }

            event.context.getMatrices().pop();
         }

      }
   }
}

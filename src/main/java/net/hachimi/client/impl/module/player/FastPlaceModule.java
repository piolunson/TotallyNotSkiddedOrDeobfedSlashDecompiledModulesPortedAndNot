package net.hachimi.client.impl.module.player;

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorMinecraftClient;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.world.SneakBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class FastPlaceModule extends ToggleModule {
   Config<Selection> selectionConfig;
   Config<Integer> delayConfig;
   Config<Float> startDelayConfig;
   Config<Boolean> ghostFixConfig;
   Config<List<Item>> whitelistConfig;
   Config<List<Item>> blacklistConfig;
   private final CacheTimer startTimer;

   public FastPlaceModule() {
      super("FastPlace", "Place items and blocks faster", ModuleCategory.PLAYER);
      this.selectionConfig = this.register(new EnumConfig("Selection", "The selection of items to apply fast placements", FastPlaceModule.Selection.WHITELIST, FastPlaceModule.Selection.values()));
      this.delayConfig = this.register(new NumberConfig("Delay", "Fast place click delay", 0, 1, 4));
      this.startDelayConfig = this.register(new NumberConfig("StartDelay", "Fast place start delay", 0.0F, 0.0F, 1.0F));
      this.ghostFixConfig = this.register(new BooleanConfig("GhostFix", "Fixes item ghosting issue on some servers", false));
      this.whitelistConfig = this.register(new ItemListConfig("Whitelist", "Valid item whitelist", new Item[]{Items.EXPERIENCE_BOTTLE, Items.SNOWBALL, Items.EGG}));
      this.blacklistConfig = this.register(new ItemListConfig("Blacklist", "Valid item blacklist", new Item[]{Items.ENDER_PEARL, Items.ENDER_EYE}));
      this.startTimer = new CacheTimer();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!mc.options.useKey.isPressed()) {
            this.startTimer.reset();
         } else if (this.startTimer.passed((Number)this.startDelayConfig.getValue(), TimeUnit.SECONDS) && ((AccessorMinecraftClient)(Object)mc).hookGetItemUseCooldown() > (Integer)this.delayConfig.getValue() && this.placeCheck(mc.player.getMainHandStack())) {
            if ((Boolean)this.ghostFixConfig.getValue()) {
               Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), id, mc.player.getYaw(), mc.player.getPitch()));
            }

            ((AccessorMinecraftClient)(Object)mc).hookSetItemUseCooldown((Integer)this.delayConfig.getValue());
         }

      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof PlayerInteractBlockC2SPacket) {
            PlayerInteractBlockC2SPacket packet = (PlayerInteractBlockC2SPacket)var3;
            if ((Boolean)this.ghostFixConfig.getValue() && !event.isClientPacket() && this.placeCheck(mc.player.getStackInHand(packet.getHand()))) {
               BlockState state = mc.world.getBlockState(packet.getBlockHitResult().getBlockPos());
               if (!SneakBlocks.isSneakBlock(state)) {
                  event.cancel();
               }
            }
         }

      }
   }

   private boolean placeCheck(ItemStack held) {
      boolean var10000;
      switch (((Selection)this.selectionConfig.getValue()).ordinal()) {
         case 0 -> var10000 = ((ItemListConfig)this.whitelistConfig).contains(held.getItem());
         case 1 -> var10000 = !((ItemListConfig)this.blacklistConfig).contains(held.getItem());
         case 2 -> var10000 = true;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;

      // $FF: synthetic method
      private static Selection[] $values() {
         return new Selection[]{WHITELIST, BLACKLIST, ALL};
      }
   }
}

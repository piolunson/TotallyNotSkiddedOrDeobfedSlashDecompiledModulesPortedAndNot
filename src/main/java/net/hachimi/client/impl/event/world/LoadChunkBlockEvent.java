package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;

public class LoadChunkBlockEvent extends Event {
   private final BlockPos pos;
   private final BlockState state;

   public LoadChunkBlockEvent(BlockPos pos, BlockState state) {
      this.pos = pos;
      this.state = state;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }
}

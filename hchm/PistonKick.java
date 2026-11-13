package net.hachimi.client.impl.module.combat;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.eventbus.annotation.EventListener;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class PistonKickModule extends ToggleModule {

    public static PistonKickModule INSTANCE;

    private final Config<Integer> delay         = this.register(new NumberConfig("Delay", "Delay between pushes (ticks)", 0, 1, 10));
    private final Config<Boolean> oneTwelve     = this.register(new BooleanConfig("1.12", "Place piston and power in the same tick", true));
    private final Config<Float>   targetRange   = this.register(new NumberConfig("TargetRange", "Target range", 1.0F, 5.0F, 6.0F));
    private final Config<Boolean> inAirTarget   = this.register(new BooleanConfig("InAirTarget", "Target players in the air", true));
    private final Config<Boolean> rotate        = this.register(new BooleanConfig("Rotate", "Silently rotate when placing", true));
    private final Config<Boolean> outOfBlock    = this.register(new BooleanConfig("OutOfBlock", "Target players partially outside of blocks", true));
    private final Config<Boolean> breakCrystal  = this.register(new BooleanConfig("BreakCrystal", "Break end crystals blocking piston path", false));
    private final Config<Float>   placeRange    = this.register(new NumberConfig("PlaceRange", "Block place range", 1.0F, 5.0F, 6.0F));
    private final Config<Float>   breakRange    = this.register(new NumberConfig("BreakRange", "Crystal break range", 1.0F, 4.5F, 6.0F, () -> this.breakCrystal.getValue()));

    private final Config<SwapAction> swapAction;
    private final Config<Boolean>    airPlace;
    private final Config<Boolean>    consecutive;
    private final Config<Boolean>    merge;

    private int tickCount;
    private Player lastTarget;

    public PistonKickModule() {
        super("PistonKick", "Pushes players out of bedrock holes using pistons", ModuleCategory.COMBAT);
        this.swapAction  = this.register(new EnumConfig("SwapAction", "How to swap to required items", SwapAction.SCREEN, SwapAction.values()));
        this.airPlace    = this.register(new BooleanConfig("AirPlace", "Allow placing blocks in the air", false));
        this.consecutive = this.register(new BooleanConfig("Consecutive", "Keep targeting the same player consecutively", true));
        this.merge       = this.register(new BooleanConfig("Merge", "Burst piston and power packets together", false));
        this.tickCount   = 0;
        this.lastTarget  = null;
        INSTANCE = this;
    }

    public static PistonKickModule getInstance() {
        return INSTANCE;
    }

    public void onEnable() {
        this.tickCount = 0;
        this.lastTarget = null;
        SnapRotation.INSTANCE.subscribe();
    }

    public void onDisable() {
        this.lastTarget = null;
        SnapRotation.INSTANCE.unsubscribe();
        SnapRotation.reset();
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null) return;

        ++this.tickCount;
        if (this.tickCount < (Integer) this.delay.getValue()) return;
        this.tickCount = 0;

        int pistonSlot = this.findClass(PistonBaseBlock.class);
        int powerSlot  = this.findPower();
        if (pistonSlot == -1 || powerSlot == -1) return;

        List<Player> enemies = this.getEnemies();
        if (enemies.isEmpty()) {
            this.lastTarget = null; // FIX #4
            return;
        }

        if ((Boolean) this.consecutive.getValue() && this.lastTarget != null && enemies.contains(this.lastTarget)) {
            enemies.remove(this.lastTarget);
            enemies.add(0, this.lastTarget);
        }

        for (Player target : enemies) {
            if ((Boolean) this.inAirTarget.getValue() || target.onGround()) {
                if ((Boolean) this.breakCrystal.getValue()) {
                    this.breakNearbyCrystals(target);
                }
                if (this.tryKick(target, pistonSlot, powerSlot)) {
                    this.lastTarget = target;
                    return;
                }
            }
        }
    }

    private boolean tryKick(Player target, int pistonSlot, int powerSlot) {
        float[] offsets = new float[]{-0.25F, 0.0F, 0.25F};

        // 1) przez ścianę, w której siedzi target
        for (float ox : offsets) {
            for (float oz : offsets) {
                BlockPos playerPos = BlockPos.containing(
                        target.getX() + ox,
                        target.getY() + 0.5,
                        target.getZ() + oz);
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = playerPos.relative(dir);
                    if (this.isTargetHere(wall, target)
                            && mc.level.noCollision(target, new AABB(wall))) {
                        if (this.tryPush(playerPos.relative(dir.getOpposite()), dir, pistonSlot, powerSlot)) return true;
                        if (this.tryPush(playerPos.relative(dir.getOpposite()).above(), dir, pistonSlot, powerSlot)) return true;
                    }
                }
            }
        }

        // 2) target częściowo nad blokiem
        if (!mc.level.noCollision(target, new AABB(BlockPos.containing(
                target.getX(), target.getY() + 2.5, target.getZ())))) {
            BlockPos entityPos = target.blockPosition();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos wall = entityPos.relative(dir);
                AABB shifted = target.getBoundingBox().move(
                        dir.getStepX(), dir.getStepY(), dir.getStepZ());
                if (mc.level.getBlockState(wall.above()).getBlock() != Blocks.BEDROCK
                        && !mc.level.noCollision(target, shifted.move(0, 1, 0))
                        && !this.isTargetHere(wall, target)) {
                    if (this.tryPush(entityPos.relative(dir.getOpposite()).above(), dir, pistonSlot, powerSlot)) return true;
                    if (this.tryPush(entityPos.relative(dir.getOpposite()), dir, pistonSlot, powerSlot)) return true;
                }
            }
        }

        // 3) fallback: dowolna ściana
        for (float ox : offsets) {
            for (float oz : offsets) {
                BlockPos playerPos = BlockPos.containing(
                        target.getX() + ox,
                        target.getY() + 0.5,
                        target.getZ() + oz);
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = playerPos.relative(dir);
                    if (this.isTargetHere(wall, target)) {
                        if (this.tryPush(playerPos.relative(dir.getOpposite()).above(), dir, pistonSlot, powerSlot)) return true;
                        if (this.tryPush(playerPos.relative(dir.getOpposite()), dir, pistonSlot, powerSlot)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean tryPush(BlockPos pistonPos, Direction pushDir, int pistonSlot, int powerSlot) {
        if (!mc.level.isEmptyBlock(pistonPos.relative(pushDir))) return false;
        if (!this.canPlaceAt(pistonPos)) return false;

        Direction placeSide = this.getPlaceSide(pistonPos);

        if (placeSide != null) {
            boolean canPower = false;
            for (Direction d : Direction.values()) {
                if (this.isRedstone(pistonPos.relative(d)) || this.canPlaceAt(pistonPos.relative(d))) {
                    canPower = true;
                    break;
                }
            }
            if (!canPower) return false;

            this.doSwap(pistonSlot);
            if ((Boolean) this.rotate.getValue()) this.pistonFacingRotation(pushDir, placeSide);
            this.placeBlockDirect(pistonPos, placeSide);
            this.restoreSlot(pistonSlot);

            if ((Boolean) this.oneTwelve.getValue() || (Boolean) this.merge.getValue()
                    || mc.level.getBlockState(pistonPos).getBlock() instanceof PistonBaseBlock) {
                for (Direction d : Direction.values()) {
                    if (this.isRedstone(pistonPos.relative(d))) return true;
                }
                for (Direction d : Direction.values()) {
                    if (d == Direction.UP) continue;
                    BlockPos powerPos = pistonPos.relative(d);
                    if (this.canPlaceAt(powerPos) && this.inPlaceRange(powerPos)) {
                        Direction pSide = this.getPlaceSide(powerPos);
                        if (pSide != null) {
                            this.doSwap(powerSlot);
                            if ((Boolean) this.rotate.getValue()) this.doRotate(powerPos, pSide);
                            this.placeBlockDirect(powerPos, pSide);
                            this.restoreSlot(powerSlot);
                            return true;
                        }
                    }
                }
                return true;
            }
        } else {
            // FIX #2: sprawdzamy getPlaceSide(powerPos), nie pistonPos
            Direction powerDir = null;
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                BlockPos powerPos = pistonPos.relative(d);
                if (this.canPlaceAt(powerPos) && this.inPlaceRange(powerPos)
                        && BlockUtilPlus.getPlaceSide(powerPos, (Predicate) null) != null) {
                    powerDir = d;
                    break;
                }
            }
            if (powerDir == null) return false;

            BlockPos powerPos = pistonPos.relative(powerDir);
            Direction pSide = this.getPlaceSide(powerPos);
            if (pSide == null) return false;

            this.doSwap(powerSlot);
            if ((Boolean) this.rotate.getValue()) this.doRotate(powerPos, pSide);
            this.placeBlockDirect(powerPos, pSide);
            this.restoreSlot(powerSlot);

            Direction newSide = this.getPlaceSide(pistonPos);
            if (newSide != null) {
                this.doSwap(pistonSlot);
                if ((Boolean) this.rotate.getValue()) this.pistonFacingRotation(pushDir, newSide);
                this.placeBlockDirect(pistonPos, newSide);
                this.restoreSlot(pistonSlot);
            }
            return true;
        }
        return false;
    }

    private boolean isTargetHere(BlockPos pos, Player target) {
        return new AABB(pos).intersects(target.getBoundingBox());
    }

    private boolean isRedstone(BlockPos pos) {
        Block b = mc.level.getBlockState(pos).getBlock();
        return b == Blocks.REDSTONE_BLOCK || b == Blocks.REDSTONE_TORCH;
    }

    private boolean canPlaceAt(BlockPos pos) {
        return mc.level.getBlockState(pos).isAir() || mc.level.isEmptyBlock(pos);
    }

    private boolean inPlaceRange(BlockPos pos) {
        double r = (double) (Float) this.placeRange.getValue();
        return mc.player.distanceToSqr(
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5) <= r * r;
    }

    private Direction getPlaceSide(BlockPos pos) {
        return (Boolean) this.airPlace.getValue()
                ? BlockUtilPlus.getClickSideStrict(pos)
                : BlockUtilPlus.getPlaceSide(pos, (Predicate) null);
    }

    private void breakNearbyCrystals(Player target) {
        double r = (double) (Float) this.breakRange.getValue();
        mc.level.getEntitiesOfClass(EndCrystal.class, target.getBoundingBox().inflate(r),
                        e -> e.isAlive() && mc.player.distanceToSqr(e) <= r * r)
                .stream()
                .min(Comparator.comparingDouble(e -> mc.player.distanceToSqr(e)))
                .ifPresent(crystal -> {
                    mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(
                            crystal, mc.player.isShiftKeyDown()));
                    mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                });
    }

    private void placeBlockDirect(BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        Vec3 hitVec = new Vec3(
                (double) neighbor.getX() + 0.5 + (double) side.getStepX() * 0.5,
                (double) neighbor.getY() + 0.5 + (double) side.getStepY() * 0.5,
                (double) neighbor.getZ() + 0.5 + (double) side.getStepZ() * 0.5);
        BlockHitResult result = new BlockHitResult(hitVec, side.getOpposite(), neighbor, false);
        Managers.NETWORK.sendSequencedPacket(id ->
                new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, id));
        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        BlockUtilPlus.placeList.add(pos);
    }

    private void applyAnticheatFlags() {
        AnticheatModule ac = AnticheatModule.getInstance();
        SnapRotation.grimFull   = ac != null ? ac.isGrimRotation() : true;
        SnapRotation.doSnapBack = ac != null ? ac.isSnapBack() : false;
    }

    private void doRotate(BlockPos pos, Direction side) {
        this.applyAnticheatFlags();
        SnapRotation.snapAt(Vec3.atCenterOf(pos).add(
                (double) side.getStepX() * 0.5,
                (double) side.getStepY() * 0.5,
                (double) side.getStepZ() * 0.5));
    }

    private void pistonFacingRotation(Direction pushDir, Direction placeSide) {
        this.applyAnticheatFlags();
        // Piston rozszerza się PRZECIWNIE do kierunku patrzenia:
        //   pushDir NORTH -> yaw   0 (patrzysz na południe)
        //   pushDir EAST  -> yaw  90 (patrzysz na zachód)
        //   pushDir SOUTH -> yaw 180 (patrzysz na północ)
        //   pushDir WEST  -> yaw -90 (patrzysz na wschód)
        float yaw;
        switch (pushDir) {
            case NORTH -> yaw = 0.0F;
            case EAST  -> yaw = 90.0F;
            case SOUTH -> yaw = 180.0F;
            case WEST  -> yaw = -90.0F;
            default    -> yaw = 0.0F;
        }
        SnapRotation.snapAt(yaw, 0.0F);
    }

    private void doSwap(int slot) {
        switch (((SwapAction) this.swapAction.getValue()).ordinal()) {
            case 0: // SCREEN
                if (slot < 9) {
                    Managers.INVENTORY.setSlot(slot);
                } else {
                    mc.gameMode.handleInventoryMouseClick(
                            mc.player.containerMenu.containerId,
                            slot,
                            mc.player.getInventory().selected,
                            ClickType.SWAP,
                            mc.player);
                }
                break; // FIX #1
            case 1: // HOTBAR
                if (slot < 9) {
                    Managers.INVENTORY.setSlot(slot);
                }
                break; // FIX #1 (był fall-through do case 2)
            case 2: // NONE
            default:
                break;
        }
    }

    private void restoreSlot(int slot) {
        if (this.swapAction.getValue() == SwapAction.SCREEN && slot >= 9) {
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    slot,
                    mc.player.getInventory().selected,
                    ClickType.SWAP,
                    mc.player);
        }
    }

    private int findPower() {
        int slot = this.findBlock(Blocks.REDSTONE_BLOCK);
        return slot != -1 ? slot : this.findBlock(Blocks.REDSTONE_TORCH);
    }

    private int findBlock(Block block) {
        if (this.swapAction.getValue() != SwapAction.NONE) {
            int end = this.swapAction.getValue() == SwapAction.SCREEN ? 36 : 9;
            for (int i = 0; i < end; ++i) {
                ItemStack s = mc.player.getInventory().getItem(i);
                if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                    return i;
                }
            }
            return -1;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
            return mc.player.getInventory().selected;
        }
        return -1;
    }

    private int findClass(Class<?> clazz) {
        if (this.swapAction.getValue() != SwapAction.NONE) {
            int end = this.swapAction.getValue() == SwapAction.SCREEN ? 36 : 9;
            for (int i = 0; i < end; ++i) {
                ItemStack s = mc.player.getInventory().getItem(i);
                if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && clazz.isInstance(bi.getBlock())) {
                    return i;
                }
            }
            return -1;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof BlockItem bi && clazz.isInstance(bi.getBlock())) {
            return mc.player.getInventory().selected;
        }
        return -1;
    }

    private List<Player> getEnemies() {
        double r = (double) (Float) this.targetRange.getValue();
        return mc.level.players().stream()
                .filter(p -> p != mc.player)
                .filter(p -> !p.isSpectator() && !p.isDeadOrDying() && p.isAlive()) // FIX #3
                .filter(p -> !Managers.SOCIAL.isFriend(p.getName().getString()))
                .filter(p -> mc.player.distanceToSqr(p) <= r * r)
                .sorted(Comparator.comparingDouble(p -> mc.player.distanceToSqr(p)))
                .collect(Collectors.toList());
    }

    public enum SwapAction {
        SCREEN, HOTBAR, NONE
    }
}

package dev.leonetic.features.modules.combat;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.modules.client.TargetsModule;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.manager.RotationRequest;
import dev.leonetic.util.MathUtil;
import dev.leonetic.util.PlaceUtil;
import dev.leonetic.util.player.EatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PistonKickModule extends Module {

    private final Setting<Integer>  delay         = num("Delay", 0, 0, 10).setPage("General");
    private final Setting<Boolean>  oneTwelve     = bool("1.12", true).setPage("General");
    private final Setting<Double>   targetRange   = num("TargetRange", 5.0, 1.0, 6.0).setPage("General");
    private final Setting<Boolean>  inAirTarget   = bool("InAirTarget", true).setPage("General");
    private final Setting<Boolean>  rotate        = bool("Rotate", true).setPage("General");
    private final Setting<Boolean>  breakCrystal  = bool("BreakCrystal", false).setPage("General");
    private final Setting<Double>   placeRange    = num("PlaceRange", 5.0, 1.0, 6.0).setPage("General");
    private final Setting<Double>   breakRange    = num("BreakRange", 4.5, 1.0, 6.0).setPage("General");
    private final Setting<SwapAction> swapAction  = mode("SwapAction", SwapAction.SCREEN).setPage("General");
    private final Setting<Boolean>  airPlace      = bool("AirPlace", false).setPage("General");
    private final Setting<Boolean>  consecutive   = bool("Consecutive", true).setPage("General");
    private final Setting<Boolean>  merge         = bool("Merge", false).setPage("General");

    public enum SwapAction { SCREEN, HOTBAR, NONE }

    private int    tickCount;
    private Player lastTarget;

    public PistonKickModule() {
        super("PistonKick",
              "Pushes players out of bedrock holes using pistons.",
              Category.COMBAT);
        breakRange.setVisibility(v -> breakCrystal.getValue());
    }

    @Override
    public void onEnable() {
        tickCount = 0;
        lastTarget = null;
    }

    @Override
    public void onDisable() {
        lastTarget = null;
    }

    // ─────────────────────────────────────────────────────────────
    //  Tick
    // ─────────────────────────────────────────────────────────────

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (nullCheck() || mc.player.isDeadOrDying()) return;
        if (EatUtil.shouldDefer()) return;

        if (++tickCount < delay.getValue()) return;
        tickCount = 0;

        int pistonSlot = findBlockSlot(Blocks.PISTON);
        if (pistonSlot == -1) pistonSlot = findBlockSlot(Blocks.STICKY_PISTON);
        int powerSlot = findBlockSlot(Blocks.REDSTONE_BLOCK);
        if (powerSlot == -1) powerSlot = findBlockSlot(Blocks.REDSTONE_TORCH);
        if (pistonSlot == -1 || powerSlot == -1) return;

        List<Player> enemies = getEnemies();
        if (enemies.isEmpty()) return;

        if (consecutive.getValue() && lastTarget != null && enemies.contains(lastTarget)) {
            enemies.remove(lastTarget);
            enemies.add(0, lastTarget);
        }

        for (Player target : enemies) {
            if (!inAirTarget.getValue() && !target.onGround()) continue;
            if (breakCrystal.getValue()) breakNearbyCrystals(target);
            if (tryKick(target, pistonSlot, powerSlot)) {
                lastTarget = target;
                return;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Logika kopania
    // ─────────────────────────────────────────────────────────────

    private boolean tryKick(Player target, int pistonSlot, int powerSlot) {
        float[] offsets = {-0.25f, 0.0f, 0.25f};

        // 1) próba: przez ścianę w której siedzi target
        for (float ox : offsets) {
            for (float oz : offsets) {
                BlockPos playerPos = BlockPos.containing(
                        target.getX() + ox, target.getY() + 0.5, target.getZ() + oz);
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = playerPos.relative(dir);
                    if (isTargetHere(wall, target) && mc.level.noCollision(target, new AABB(wall))) {
                        if (tryPush(playerPos.relative(dir.getOpposite()), dir, pistonSlot, powerSlot)) return true;
                        if (tryPush(playerPos.relative(dir.getOpposite()).above(), dir, pistonSlot, powerSlot)) return true;
                    }
                }
            }
        }

        // 2) próba: target częściowo nad blokiem
        if (!mc.level.noCollision(target, new AABB(BlockPos.containing(
                target.getX(), target.getY() + 2.5, target.getZ())))) {
            BlockPos entityPos = target.blockPosition();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos wall = entityPos.relative(dir);
                AABB shifted = target.getBoundingBox().move(
                        dir.getStepX(), dir.getStepY(), dir.getStepZ());
                if (mc.level.getBlockState(wall.above()).getBlock() != Blocks.BEDROCK
                        && !mc.level.noCollision(target, shifted.move(0, 1, 0))
                        && !isTargetHere(wall, target)) {
                    if (tryPush(entityPos.relative(dir.getOpposite()).above(), dir, pistonSlot, powerSlot)) return true;
                    if (tryPush(entityPos.relative(dir.getOpposite()), dir, pistonSlot, powerSlot)) return true;
                }
            }
        }

        // 3) fallback: dowolna ściana
        for (float ox : offsets) {
            for (float oz : offsets) {
                BlockPos playerPos = BlockPos.containing(
                        target.getX() + ox, target.getY() + 0.5, target.getZ() + oz);
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = playerPos.relative(dir);
                    if (isTargetHere(wall, target)) {
                        if (tryPush(playerPos.relative(dir.getOpposite()).above(), dir, pistonSlot, powerSlot)) return true;
                        if (tryPush(playerPos.relative(dir.getOpposite()), dir, pistonSlot, powerSlot)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean tryPush(BlockPos pistonPos, Direction pushDir, int pistonSlot, int powerSlot) {
        if (!mc.level.getBlockState(pistonPos.relative(pushDir)).isAir()) return false;
        if (!PlaceUtil.canPlace(pistonPos)) return false;

        Direction placeSide = getPlaceSide(pistonPos);

        if (placeSide != null) {
            boolean canPower = false;
            for (Direction d : Direction.values()) {
                if (isRedstone(pistonPos.relative(d)) || PlaceUtil.canPlace(pistonPos.relative(d))) {
                    canPower = true;
                    break;
                }
            }
            if (!canPower) return false;

            doSwap(pistonSlot);
            if (rotate.getValue()) rotateAt(pistonPos);
            placeBlockDirect(pistonPos, pistonSlot);
            restoreSlot(pistonSlot);

            if (oneTwelve.getValue() || merge.getValue() || isPiston(mc.level.getBlockState(pistonPos))) {
                for (Direction d : Direction.values()) {
                    if (isRedstone(pistonPos.relative(d))) return true;
                }
                for (Direction d : Direction.values()) {
                    if (d == Direction.UP) continue;
                    BlockPos powerPos = pistonPos.relative(d);
                    if (!PlaceUtil.canPlace(powerPos) || !inPlaceRange(powerPos)) continue;
                    doSwap(powerSlot);
                    if (rotate.getValue()) rotateAt(powerPos);
                    placeBlockDirect(powerPos, powerSlot);
                    restoreSlot(powerSlot);
                    return true;
                }
                return true;
            }
        }

        // Reverse order (piston nie ma miejsca, ale power ma)
        Direction powerDir = null;
        for (Direction d : Direction.values()) {
            if (d == Direction.UP) continue;
            BlockPos powerPos = pistonPos.relative(d);
            if (PlaceUtil.canPlace(powerPos) && inPlaceRange(powerPos)) {
                powerDir = d;
                break;
            }
        }
        if (powerDir == null) return false;

        BlockPos powerPos = pistonPos.relative(powerDir);
        doSwap(powerSlot);
        if (rotate.getValue()) rotateAt(powerPos);
        placeBlockDirect(powerPos, powerSlot);
        restoreSlot(powerSlot);

        Direction newSide = getPlaceSide(pistonPos);
        if (newSide != null) {
            doSwap(pistonSlot);
            if (rotate.getValue()) rotateAt(pistonPos);
            placeBlockDirect(pistonPos, pistonSlot);
            restoreSlot(pistonSlot);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────
    //  Placement / swap
    // ─────────────────────────────────────────────────────────────

    private void placeBlockDirect(BlockPos pos, int sourceSlot) {
        Item item = mc.player.getInventory().getItem(sourceSlot).getItem();
        Homovore.placementManager.placeDirect(pos, null, item);
    }

    private void rotateAt(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition(1.0f);
        Vec3 hit = Vec3.atCenterOf(pos);
        float[] angles = MathUtil.calcAngle(eye, hit);
        Homovore.rotationManager.submit(new RotationRequest(
                "PistonKick_place", 70, angles[0], angles[1], RotationRequest.Mode.SILENT));
    }

    private void doSwap(int slot) {
        SwapAction action = swapAction.getValue();
        if (action == SwapAction.NONE) return;
        if (slot < 9) {
            mc.player.getInventory().selected = slot;
        } else if (action == SwapAction.SCREEN) {
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId, slot,
                    mc.player.getInventory().selected, ClickType.SWAP, mc.player);
        }
    }

    private void restoreSlot(int slot) {
        if (slot < 9) return;
        if (swapAction.getValue() != SwapAction.SCREEN) return;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId, slot,
                mc.player.getInventory().selected, ClickType.SWAP, mc.player);
    }

    // ─────────────────────────────────────────────────────────────
    //  Pomocnicze
    // ─────────────────────────────────────────────────────────────

    private int findBlockSlot(net.minecraft.world.level.block.Block block) {
        SwapAction action = swapAction.getValue();
        if (action == SwapAction.NONE) {
            ItemStack held = mc.player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                return mc.player.getInventory().selected;
            }
            return -1;
        }
        int end = action == SwapAction.SCREEN ? 36 : 9;
        for (int i = 0; i < end; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() == block) return i;
        }
        return -1;
    }

    private List<Player> getEnemies() {
        TargetsModule targets = Homovore.moduleManager.getModuleByClass(TargetsModule.class);
        double r = targetRange.getValue();
        double rSq = r * r;
        List<Player> out = new ArrayList<>();
        for (Player p : mc.level.players()) {
            if (p == mc.player || p.isDeadOrDying() || p.isSpectator()) continue;
            if (targets != null && !targets.isValidPlayerTarget(p)) continue;
            if (mc.player.distanceToSqr(p) > rSq) continue;
            out.add(p);
        }
        out.sort(Comparator.comparingDouble(p -> mc.player.distanceToSqr(p)));
        return out;
    }

    private void breakNearbyCrystals(Player target) {
        double r = breakRange.getValue();
        double rSq = r * r;
        EndCrystal best = null;
        double bestDist = Double.MAX_VALUE;
        AABB area = target.getBoundingBox().inflate(r);
        for (Entity e : mc.level.getEntities(target, area)) {
            if (!(e instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;
            double d = mc.player.distanceToSqr(crystal);
            if (d > rSq) continue;
            if (d < bestDist) { bestDist = d; best = crystal; }
        }
        if (best != null) {
            mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(
                    best, mc.player.isShiftKeyDown()));
            mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }

    private boolean isTargetHere(BlockPos pos, Player target) {
        return new AABB(pos).intersects(target.getBoundingBox());
    }

    private boolean isRedstone(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.REDSTONE_BLOCK)
                || state.is(Blocks.REDSTONE_TORCH)
                || state.is(Blocks.REDSTONE_WALL_TORCH);
    }

    private boolean isPiston(BlockState state) {
        return state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON);
    }

    private boolean inPlaceRange(BlockPos pos) {
        double r = placeRange.getValue();
        return mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= r * r;
    }

    private Direction getPlaceSide(BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (!mc.level.getBlockState(pos.relative(d)).isAir()) return d;
        }
        return airPlace.getValue() ? Direction.DOWN : null;
    }
}

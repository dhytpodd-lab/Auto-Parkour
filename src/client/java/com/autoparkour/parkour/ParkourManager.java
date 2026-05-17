package com.autoparkour.parkour;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ParkourManager {

    private boolean enabled = false;
    private boolean isParkouring = false;
    private BlockPos currentTargetBlock = null;
    private BlockPos lastTargetBlock = null;
    private int tickCounter = 0;
    private int noBlockTicks = 0;
    private int stuckTicks = 0;
    private Vec3d lastPlayerPos = Vec3d.ZERO;
    private int targetSwitchCooldown = 0;

    private BlockPos airTargetLock = null;
    private int airTargetLockTicks = 0;
    private static final int AIR_LOCK_DURATION = 70;

    private BlockPos lastJumpedFromBlock = null;
    private int lastJumpCooldown = 0;
    private static final int JUMP_COOLDOWN_TICKS = 45;

    private boolean jumpJustPerformed = false;

    private final ParkourExecutor executor;
    private final BlockScanner blockScanner;
    private final MovementHelper movementHelper;
    private final ModConfig config;

    private BlockPos lastTargetBeforeJump = null;
    private int lastTargetCooldown = 0;
    private static final int LAST_TARGET_COOLDOWN_TICKS = 35;

    private final Map<BlockPos, Integer> vanishedTargets = new HashMap<>();
    private static final int VANISHED_TARGET_COOLDOWN_TICKS = 90;

    private static final double LONG_JUMP_MIN_DISTANCE = 2.75;
    private static final double LONG_JUMP_IDEAL_DISTANCE = 3.55;
    private static final double LONG_JUMP_MAX_DISTANCE = 4.35;

    public ParkourManager() {
        this.config = AutoParkourMod.getInstance().getConfigManager().getConfig();
        this.executor = new ParkourExecutor();
        this.blockScanner = new BlockScanner();
        this.movementHelper = new MovementHelper();
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        ClientPlayerEntity player = client.player;
        Vec3d currentPos = player.getPos();
        Vec3d previousPos = lastPlayerPos;

        if (!previousPos.equals(Vec3d.ZERO)) {
            double movementSpeed = currentPos.squaredDistanceTo(previousPos);
            if (movementSpeed > 100) {
                executor.onTeleport();
                clearDynamicTargets();
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("Teleport detected, resetting parkour targets");
                }
            }
        }

        tickCounter++;
        if (targetSwitchCooldown > 0) targetSwitchCooldown--;
        if (lastJumpCooldown > 0) lastJumpCooldown--;
        if (lastTargetCooldown > 0) lastTargetCooldown--;
        tickVanishedTargets();

        if (airTargetLock != null) {
            airTargetLockTicks--;
            if (airTargetLockTicks <= 0 || player.isOnGround() || !isTargetValid(player, client.world, airTargetLock)) {
                if (!isTargetValid(player, client.world, airTargetLock)) {
                    markVanishedTarget(airTargetLock);
                }
                airTargetLock = null;
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("Air target lock released");
                }
            }
        }

        if (enabled && currentTargetBlock != null && !previousPos.equals(Vec3d.ZERO)) {
            if (previousPos.squaredDistanceTo(currentPos) < 0.0008) {
                stuckTicks++;
                if (stuckTicks > 8 && player.isOnGround()) {
                    executor.resetCooldowns();
                    targetSwitchCooldown = 0;
                    stuckTicks = 0;
                } else if (stuckTicks > 30) {
                    markVanishedTarget(currentTargetBlock);
                    currentTargetBlock = null;
                    airTargetLock = null;
                    stuckTicks = 0;
                }
            } else {
                stuckTicks = 0;
            }
        }

        if (enabled) {
            performParkour(player, client.world);
        }

        lastPlayerPos = currentPos;
    }

    private void performParkour(ClientPlayerEntity player, World world) {
        if (!isSafe(player, world)) {
            if (config.isSafeFallEnabled()) {
                handleUnsafeSituation(player);
            }
            return;
        }

        if (currentTargetBlock != null && !isTargetValid(player, world, currentTargetBlock)) {
            BlockPos vanishedTarget = currentTargetBlock;
            markVanishedTarget(vanishedTarget);
            if (config.isDebugMode()) {
                AutoParkourMod.getInstance().getLogger().debug("Target vanished, skipping: " + vanishedTarget.toShortString());
            }
            currentTargetBlock = null;
            airTargetLock = null;
            isParkouring = false;
        }

        int scanRange = Math.max(config.getMaxScanRange() + 1, Math.max(5, config.getMaxJumpDistance() + 1));
        List<BlockPos> nearbyBlocks = blockScanner.scanNearbyBlocks(player, world, scanRange);
        filterCandidateBlocks(player, world, nearbyBlocks);

        if (!player.isOnGround() && airTargetLock != null) {
            if (isTargetValid(player, world, airTargetLock)) {
                currentTargetBlock = airTargetLock;
                isParkouring = true;
                executor.executeMovement(player, airTargetLock, config);
                return;
            }

            markVanishedTarget(airTargetLock);
            airTargetLock = null;
            currentTargetBlock = null;
            isParkouring = false;
            return;
        }

        if (nearbyBlocks.isEmpty()) {
            noBlockTicks++;
            isParkouring = false;
            if (noBlockTicks > 15) {
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("No valid parkour blocks nearby");
                }
                noBlockTicks = 0;
            }
            return;
        }
        noBlockTicks = 0;

        BlockPos targetBlock = findOptimalTargetBlock(player, nearbyBlocks, world);

        if (targetBlock == null) {
            isParkouring = false;
            if (player.isOnGround() && currentTargetBlock != null && targetSwitchCooldown <= 0) {
                lastTargetBeforeJump = currentTargetBlock;
                lastTargetCooldown = LAST_TARGET_COOLDOWN_TICKS;
                currentTargetBlock = null;
            }
            return;
        }

        if (player.isOnGround()) {
            if (!targetBlock.equals(currentTargetBlock) && targetSwitchCooldown <= 0) {
                if (currentTargetBlock != null) {
                    lastTargetBeforeJump = currentTargetBlock;
                    lastTargetCooldown = LAST_TARGET_COOLDOWN_TICKS;
                }

                lastTargetBlock = currentTargetBlock;
                currentTargetBlock = targetBlock;
                targetSwitchCooldown = horizontalDistanceToBlockCenter(player.getPos(), targetBlock) >= LONG_JUMP_MIN_DISTANCE ? 2 : 3;

                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("New target: " + targetBlock.toShortString());
                }
            }
        } else if (currentTargetBlock == null) {
            currentTargetBlock = targetBlock;
            airTargetLock = targetBlock;
            airTargetLockTicks = AIR_LOCK_DURATION;
            if (config.isDebugMode()) {
                AutoParkourMod.getInstance().getLogger().debug("Air target locked: " + targetBlock.toShortString());
            }
        }

        if (currentTargetBlock != null && isTargetValid(player, world, currentTargetBlock)) {
            updateJumpSourceTracking(player);
            isParkouring = true;
            executor.executeMovement(player, currentTargetBlock, config);
        } else if (currentTargetBlock != null) {
            markVanishedTarget(currentTargetBlock);
            currentTargetBlock = null;
            airTargetLock = null;
            isParkouring = false;
        }
    }

    private void filterCandidateBlocks(ClientPlayerEntity player, World world, List<BlockPos> blocks) {
        BlockPos standingBlock = player.getBlockPos().down();

        blocks.removeIf(block ->
                block == null ||
                block.equals(standingBlock) ||
                isVanishedTarget(block) ||
                (lastJumpedFromBlock != null && lastJumpCooldown > 0 && block.equals(lastJumpedFromBlock)) ||
                (lastTargetBeforeJump != null && lastTargetCooldown > 0 && block.equals(lastTargetBeforeJump)) ||
                !isBlockReachable(player, block, world) ||
                !blockScanner.isLandingStillValid(player, world, block, config)
        );
    }

    private BlockPos findOptimalTargetBlock(ClientPlayerEntity player, List<BlockPos> blocks, World world) {
        if (blocks.isEmpty()) return null;

        BlockPos bestBlock = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos block : new ArrayList<>(blocks)) {
            if (!isTargetValid(player, world, block)) continue;

            double score = scoreTarget(player, world, block);
            if (score < bestScore) {
                bestScore = score;
                bestBlock = block;
            }
        }

        return bestBlock;
    }

    private double scoreTarget(ClientPlayerEntity player, World world, BlockPos block) {
        Vec3d playerPos = player.getPos();
        BlockPos playerBlockPos = player.getBlockPos();

        double horizontal = horizontalDistanceToBlockCenter(playerPos, block);
        double heightDiff = (block.getY() + 1.0) - playerPos.y;
        boolean diagonal = isDiagonal(playerBlockPos, block);

        double maxJump = Math.max(config.getMaxJumpDistance(), 4);
        double idealDistance = Math.min(Math.max(LONG_JUMP_IDEAL_DISTANCE, config.getMinJumpDistance() + 0.75), maxJump + 0.05);

        double score = Math.abs(horizontal - idealDistance) * 1.45;
        score += Math.abs(heightDiff) * 1.15;

        if (horizontal >= LONG_JUMP_MIN_DISTANCE && horizontal <= LONG_JUMP_MAX_DISTANCE) {
            score -= 2.35;
        } else if (horizontal >= 2.15 && horizontal < LONG_JUMP_MIN_DISTANCE) {
            score -= 0.8;
        } else if (horizontal < Math.max(1.15, config.getMinJumpDistance())) {
            score += 2.0;
        }

        Vec3d toTarget = normalizeFlat(new Vec3d(block.getX() + 0.5 - playerPos.x, 0.0, block.getZ() + 0.5 - playerPos.z));
        Vec3d look = normalizeFlat(player.getRotationVec(1.0F));
        if (toTarget.length() > 0.1 && look.length() > 0.1) {
            double dot = look.dotProduct(toTarget);
            score -= dot * 1.25;
            if (dot < -0.15) score += 2.4;
        }

        if (diagonal) {
            if (!config.isAllowDiagonalMovement()) {
                return Double.MAX_VALUE;
            }
            int absDx = Math.abs(block.getX() - playerBlockPos.getX());
            int absDz = Math.abs(block.getZ() - playerBlockPos.getZ());
            double imbalance = Math.abs(absDx - absDz);
            score += config.isPreferStraightDirections() ? 0.55 : -0.40;
            score += imbalance * 0.24;
            if (horizontal >= 2.40 && horizontal <= 4.25) score -= 0.45;
            if (horizontal > 4.45) score += 1.75;
        } else {
            score += config.isPreferStraightDirections() ? -0.45 : 0.20;
        }

        if (!movementHelper.isPathClear(player, block, world)) {
            score += 4.2;
        }

        Block targetBlock = world.getBlockState(block).getBlock();
        if (blockScanner.isPreferredBlock(targetBlock)) {
            score -= 0.2;
        }

        if (currentTargetBlock != null && currentTargetBlock.equals(block)) {
            score -= 0.65;
        }

        if (lastTargetBlock != null && lastTargetBlock.equals(block)) {
            score += 0.25;
        }

        return score;
    }

    private boolean isBlockReachable(ClientPlayerEntity player, BlockPos block, World world) {
        if (player == null || block == null || world == null) return false;

        if (!config.isAllowDiagonalMovement() && isDiagonal(player.getBlockPos(), block)) {
            return false;
        }

        double horizontal = horizontalDistanceToBlockCenter(player.getPos(), block);
        boolean diagonal = isDiagonal(player.getBlockPos(), block);
        double maxJump = Math.max(config.getMaxJumpDistance(), 4) + (diagonal ? 0.70 : 0.35);
        if (horizontal > maxJump) return false;
        if (horizontal < 0.55) return false;

        double heightDiff = (block.getY() + 1.0) - player.getY();
        if (heightDiff > 1.25 || heightDiff < -3.25) return false;

        if (diagonal && horizontal > 4.70) {
            return false;
        }

        return blockScanner.isLandingStillValid(player, world, block, config);
    }

    private boolean isTargetValid(ClientPlayerEntity player, World world, BlockPos block) {
        return block != null &&
                !isVanishedTarget(block) &&
                isBlockReachable(player, block, world) &&
                blockScanner.isLandingStillValid(player, world, block, config);
    }

    private void updateJumpSourceTracking(ClientPlayerEntity player) {
        if (player.isOnGround()) {
            jumpJustPerformed = false;
            return;
        }

        if (!jumpJustPerformed && !lastPlayerPos.equals(Vec3d.ZERO) && player.getY() > lastPlayerPos.y + 0.015) {
            lastJumpedFromBlock = player.getBlockPos().down().toImmutable();
            lastJumpCooldown = JUMP_COOLDOWN_TICKS;
            jumpJustPerformed = true;

            if (currentTargetBlock != null) {
                lastTargetBeforeJump = currentTargetBlock;
                lastTargetCooldown = LAST_TARGET_COOLDOWN_TICKS;
            }

            if (config.isDebugMode()) {
                AutoParkourMod.getInstance().getLogger().debug("Jump started from: " + lastJumpedFromBlock.toShortString());
            }
        }
    }

    private boolean isSafe(ClientPlayerEntity player, World world) {
        if (config.isAvoidLava()) {
            BlockPos playerPos = player.getBlockPos();
            for (int y = playerPos.getY(); y > playerPos.getY() - 3; y--) {
                BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
                Block block = world.getBlockState(checkPos).getBlock();
                if (block == Blocks.LAVA) {
                    return false;
                }
            }
        }

        return !(config.isAvoidVoid() && player.getY() < -64);
    }

    private void handleUnsafeSituation(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (!isVanishedTarget(checkPos) && blockScanner.isLandingStillValid(player, world, checkPos, config)) {
                        executor.tryReachNearestBlock(player, checkPos, config);
                        return;
                    }
                }
            }
        }

        movementHelper.safeFall(player);
    }

    private void markVanishedTarget(BlockPos block) {
        if (block == null) return;
        vanishedTargets.put(block.toImmutable(), VANISHED_TARGET_COOLDOWN_TICKS);
        if (currentTargetBlock != null && currentTargetBlock.equals(block)) currentTargetBlock = null;
        if (airTargetLock != null && airTargetLock.equals(block)) airTargetLock = null;
    }

    private boolean isVanishedTarget(BlockPos block) {
        return block != null && vanishedTargets.containsKey(block);
    }

    private void tickVanishedTargets() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = vanishedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }

    private void clearDynamicTargets() {
        currentTargetBlock = null;
        airTargetLock = null;
        lastJumpedFromBlock = null;
        lastTargetBeforeJump = null;
        vanishedTargets.clear();
        targetSwitchCooldown = 0;
        lastJumpCooldown = 0;
        lastTargetCooldown = 0;
    }

    private boolean isDiagonal(BlockPos from, BlockPos to) {
        return from != null && to != null && from.getX() != to.getX() && from.getZ() != to.getZ();
    }

    private Vec3d normalizeFlat(Vec3d vector) {
        Vec3d flat = new Vec3d(vector.x, 0.0, vector.z);
        if (flat.length() < 0.0001) return Vec3d.ZERO;
        return flat.normalize();
    }

    private double horizontalDistanceToBlockCenter(Vec3d from, BlockPos block) {
        double dx = block.getX() + 0.5 - from.x;
        double dz = block.getZ() + 0.5 - from.z;
        return Math.hypot(dx, dz);
    }

    private void handleNoBlocksNearby(ClientPlayerEntity player) {}

    public void toggleEnabled() {
        this.enabled = !enabled;
        reset();
    }

    public void reset() {
        this.isParkouring = false;
        this.currentTargetBlock = null;
        this.lastTargetBlock = null;
        this.tickCounter = 0;
        this.noBlockTicks = 0;
        this.stuckTicks = 0;
        this.targetSwitchCooldown = 0;
        this.airTargetLock = null;
        this.airTargetLockTicks = 0;
        this.lastJumpedFromBlock = null;
        this.lastJumpCooldown = 0;
        this.jumpJustPerformed = false;
        this.lastTargetBeforeJump = null;
        this.lastTargetCooldown = 0;
        this.vanishedTargets.clear();
        executor.resetCooldowns();
    }



    public void reloadRuntimeConfig() {
        clearDynamicTargets();
        reset();
        executor.reloadLearning();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isParkouring() {
        return isParkouring;
    }

    public BlockPos getCurrentTargetBlock() {
        return currentTargetBlock;
    }

    public BlockPos getLastTargetBlock() {
        return lastTargetBlock;
    }

    public int getNearbyBlocksCount() {
        return blockScanner.getLastScanCount();
    }
}

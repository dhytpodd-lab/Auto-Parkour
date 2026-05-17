package com.autoparkour.parkour;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockScanner {

    private int lastScanCount = 0;

    private static final List<Block> UNSAFE_BLOCKS = List.of(
            Blocks.LAVA,
            Blocks.WATER,
            Blocks.FIRE, Blocks.SOUL_FIRE,
            Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH,
            Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.POWDER_SNOW, Blocks.COBWEB,
            Blocks.POINTED_DRIPSTONE
    );

    public List<BlockPos> scanNearbyBlocks(ClientPlayerEntity player, World world, int range) {
        List<BlockPos> blocks = new ArrayList<>();
        if (player == null || world == null) {
            lastScanCount = 0;
            return blocks;
        }

        BlockPos playerBlockPos = player.getBlockPos();
        Vec3d playerPos = player.getPos();
        ModConfig config = AutoParkourMod.getInstance().getConfigManager().getConfig();

        int effectiveRange = Math.max(2, range);
        int verticalDown = 3;
        int verticalUp = 3;

        for (int x = -effectiveRange; x <= effectiveRange; x++) {
            for (int z = -effectiveRange; z <= effectiveRange; z++) {
                double horizontalFromPlayerBlock = Math.hypot(x, z);
                if (horizontalFromPlayerBlock > effectiveRange + 0.35) {
                    continue;
                }

                if (!config.isAllowDiagonalMovement() && x != 0 && z != 0) {
                    continue;
                }

                for (int y = -verticalDown; y <= verticalUp; y++) {
                    BlockPos checkPos = playerBlockPos.add(x, y, z);

                    if (checkPos.equals(playerBlockPos) || checkPos.equals(playerBlockPos.down())) {
                        continue;
                    }

                    if (!isInsideWorld(world, checkPos)) {
                        continue;
                    }

                    Block block = world.getBlockState(checkPos).getBlock();
                    if (!isSuitableParkourBlock(block, world, checkPos, config)) {
                        continue;
                    }

                    if (!hasLandingSpace(world, checkPos)) {
                        continue;
                    }

                    double landingDistance = horizontalDistanceToBlockCenter(playerPos, checkPos);
                    if (landingDistance < 0.55 || landingDistance > effectiveRange + 0.65) {
                        continue;
                    }

                    blocks.add(checkPos.toImmutable());
                }
            }
        }

        blocks.sort(Comparator.comparingDouble(pos -> horizontalDistanceToBlockCenter(playerPos, pos)));
        lastScanCount = blocks.size();
        return blocks;
    }

    public boolean isSuitableParkourBlock(Block block, World world, BlockPos pos, ModConfig config) {
        if (world == null || pos == null || config == null || !isInsideWorld(world, pos)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        block = state.getBlock();

        if (state.isAir() || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
            return false;
        }

        if (UNSAFE_BLOCKS.contains(block)) {
            return false;
        }

        if (config.isUseIgnoreList()) {
            String blockId = Registries.BLOCK.getId(block).toString();
            if (config.getIgnoredBlocks().contains(blockId)) {
                return false;
            }
        }

        if (config.isUseBlockWhitelist()) {
            String blockId = Registries.BLOCK.getId(block).toString();
            if (!config.getAllowedBlocks().contains(blockId)) {
                return false;
            }
        }

        return hasLandingSpace(world, pos);
    }

    public boolean isLandingStillValid(ClientPlayerEntity player, World world, BlockPos pos, ModConfig config) {
        if (player == null || world == null || pos == null || config == null) {
            return false;
        }

        if (pos.equals(player.getBlockPos().down())) {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();
        return isSuitableParkourBlock(block, world, pos, config);
    }

    public boolean isSafeBlock(Block block) {
        return block != null && !UNSAFE_BLOCKS.contains(block) && block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR;
    }

    public boolean isPreferredBlock(Block block) {
        if (block == null) return false;
        return block == Blocks.STONE ||
                block == Blocks.COBBLESTONE ||
                block == Blocks.STONE_BRICKS ||
                block == Blocks.DEEPSLATE ||
                block == Blocks.COBBLED_DEEPSLATE ||
                block == Blocks.OAK_PLANKS ||
                block == Blocks.SPRUCE_PLANKS ||
                block == Blocks.WHITE_CONCRETE;
    }

    public int getLastScanCount() {
        return lastScanCount;
    }

    public BlockPos findClosestBlockInDirection(ClientPlayerEntity player, World world, int range, double lookYaw) {
        if (player == null || world == null) return null;

        BlockPos playerPos = player.getBlockPos();
        ModConfig config = AutoParkourMod.getInstance().getConfigManager().getConfig();

        double rad = Math.toRadians(lookYaw);
        int dx = (int) Math.round(-Math.sin(rad));
        int dz = (int) Math.round(Math.cos(rad));

        for (int i = 1; i <= range; i++) {
            BlockPos checkPos = playerPos.add(dx * i, 0, dz * i);

            for (int y = -2; y <= 3; y++) {
                BlockPos heightPos = checkPos.up(y);
                if (isLandingStillValid(player, world, heightPos, config)) {
                    return heightPos.toImmutable();
                }
            }
        }

        return null;
    }

    private boolean hasLandingSpace(World world, BlockPos blockPos) {
        return isPassable(world, blockPos.up()) && isPassable(world, blockPos.up(2));
    }

    private boolean isPassable(World world, BlockPos pos) {
        if (!isInsideWorld(world, pos)) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return state.isAir() ||
                block == Blocks.CAVE_AIR ||
                block == Blocks.VOID_AIR ||
                block == Blocks.TALL_GRASS ||
                block == Blocks.FERN ||
                block == Blocks.VINE;
    }

    private boolean isInsideWorld(World world, BlockPos pos) {
        return pos.getY() >= world.getBottomY() && pos.getY() <= world.getTopYInclusive();
    }

    private double horizontalDistanceToBlockCenter(Vec3d from, BlockPos blockPos) {
        double dx = blockPos.getX() + 0.5 - from.x;
        double dz = blockPos.getZ() + 0.5 - from.z;
        return Math.hypot(dx, dz);
    }
}

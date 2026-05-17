package com.autoparkour.parkour;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MovementHelper {

    public boolean isPathClear(ClientPlayerEntity player, BlockPos targetBlock, World world) {
        if (player == null || targetBlock == null || world == null) return false;

        Vec3d start = player.getPos().add(0.0, 0.15, 0.0);
        Vec3d end = new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 1.0, targetBlock.getZ() + 0.5);

        return hasLineOfSight(world, start, end, targetBlock) &&
                hasLineOfSight(world, start.add(0.0, 1.35, 0.0), end.add(0.0, 0.9, 0.0), targetBlock);
    }

    private boolean hasLineOfSight(World world, Vec3d start, Vec3d end, BlockPos targetBlock) {
        Vec3d delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 0.05) return true;

        Vec3d direction = delta.normalize();
        int steps = Math.max(1, (int) Math.ceil(distance * 4.0));

        for (int i = 1; i <= steps; i++) {
            Vec3d currentPos = start.add(direction.multiply(distance * i / steps));
            BlockPos checkPos = BlockPos.ofFloored(currentPos);

            if (checkPos.equals(targetBlock) || checkPos.equals(targetBlock.up()) || checkPos.equals(targetBlock.up(2))) {
                continue;
            }

            BlockState state = world.getBlockState(checkPos);
            if (!isTransparent(state.getBlock())) {
                return false;
            }
        }

        return true;
    }

    private boolean isTransparent(Block block) {
        return block == Blocks.AIR ||
               block == Blocks.CAVE_AIR ||
               block == Blocks.VOID_AIR ||
               block == Blocks.WATER ||
               block == Blocks.VINE ||
               block == Blocks.TALL_GRASS ||
               block == Blocks.FERN;
    }

    public void safeFall(ClientPlayerEntity player) {
        if (player == null) return;

        Vec3d velocity = player.getVelocity();
        if (velocity.y < -0.5) {
            player.setVelocity(velocity.x * 0.65, -0.5, velocity.z * 0.65);
        }
    }

    public boolean isStandingOnEdge(ClientPlayerEntity player, World world) {
        if (player == null || world == null) return false;

        Box boundingBox = player.getBoundingBox();
        double minX = boundingBox.minX + 0.08;
        double maxX = boundingBox.maxX - 0.08;
        double minZ = boundingBox.minZ + 0.08;
        double maxZ = boundingBox.maxZ - 0.08;
        double y = boundingBox.minY - 0.08;

        int supportedCorners = 0;
        if (!world.getBlockState(BlockPos.ofFloored(minX, y, minZ)).isAir()) supportedCorners++;
        if (!world.getBlockState(BlockPos.ofFloored(maxX, y, minZ)).isAir()) supportedCorners++;
        if (!world.getBlockState(BlockPos.ofFloored(minX, y, maxZ)).isAir()) supportedCorners++;
        if (!world.getBlockState(BlockPos.ofFloored(maxX, y, maxZ)).isAir()) supportedCorners++;

        return supportedCorners > 0 && supportedCorners < 4;
    }

    public double calculateJumpVelocity(double distance) {
        if (distance >= 3.75) return 0.36;
        if (distance >= 3.25) return 0.31;
        if (distance >= 2.75) return 0.27;
        return Math.min(0.24, distance * 0.12);
    }

    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (world == null || pos == null) return false;

        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopYInclusive()) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return state.isAir() || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }
}

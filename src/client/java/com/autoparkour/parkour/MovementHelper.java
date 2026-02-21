package com.autoparkour.parkour;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MovementHelper {

    public boolean isPathClear(ClientPlayerEntity player, BlockPos targetBlock, World world) {
        Vec3d start = player.getPos();
        Vec3d end = new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 1.0, targetBlock.getZ() + 0.5);

        // Проверяем линию видимости
        return hasLineOfSight(world, start, end);
    }

    private boolean hasLineOfSight(World world, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        int steps = (int) (distance * 2);

        Vec3d currentPos = start;
        for (int i = 0; i < steps; i++) {
            currentPos = currentPos.add(direction.multiply(0.5));
            BlockPos checkPos = BlockPos.ofFloored(currentPos);

            Block block = world.getBlockState(checkPos).getBlock();
            if (block != Blocks.AIR && !isTransparent(block)) {
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
               block == Blocks.LAVA ||
               block == Blocks.VINE ||
               block == Blocks.TALL_GRASS ||
               block == Blocks.FERN;
    }

    public void safeFall(ClientPlayerEntity player) {
        // Замедляем падение если оно слишком быстрое
        Vec3d velocity = player.getVelocity();
        if (velocity.y < -0.5) {
            player.setVelocity(velocity.x, -0.5, velocity.z);
        }
    }

    public boolean isStandingOnEdge(ClientPlayerEntity player, World world) {
        Box boundingBox = player.getBoundingBox();
        BlockPos playerPos = player.getBlockPos();

        // Проверяем, есть ли блоки под ногами
        boolean hasBlockBelow = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = playerPos.add(x, -1, z);
                if (world.getBlockState(checkPos).getBlock() != Blocks.AIR) {
                    hasBlockBelow = true;
                    break;
                }
            }
        }

        return !hasBlockBelow;
    }

    public double calculateJumpVelocity(double distance) {
        // Расчет необходимой скорости прыжка для преодоления дистанции
        return Math.min(0.42, distance * 0.15);
    }

    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopYInclusive())
        {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }
}

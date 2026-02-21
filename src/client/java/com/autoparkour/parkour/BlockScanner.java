package com.autoparkour.parkour;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BlockScanner {

    private int lastScanCount = 0;

    private static final List<Block> UNSAFE_BLOCKS = List.of(
            Blocks.LAVA,
            Blocks.FIRE, Blocks.SOUL_FIRE,
            Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH,
            Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE
    );

    public List<BlockPos> scanNearbyBlocks(ClientPlayerEntity player, World world, int range) {
    List<BlockPos> blocks = new ArrayList<>();
    BlockPos playerPos = player.getBlockPos();
    ModConfig config = AutoParkourMod.getInstance().getConfigManager().getConfig();

    for (int x = -range; x <= range; x++) {
        for (int z = -range; z <= range; z++) {
            for (int y = -2; y <= 3; y++) {
                BlockPos checkPos = playerPos.add(x, y, z);

                // Пропускаем блоки слишком далеко
                if (checkPos.getSquaredDistance(playerPos) > range * range) {
                    continue;
                }

                // НОВОЕ: Пропускаем блок под игроком
                if (checkPos.equals(playerPos.down())) {
                    continue; // Игнорируем блок, на котором стоим
                }

                    // НЕ ПРОВЕРЯЕМ направление взгляда! Сканируем всё вокруг
                    // Убрали проверку dot product

                    // Если запрещено диагональное движение, проверяем направление
// Если запрещено диагональное движение, проверяем направление
if (!config.isAllowDiagonalMovement()) {
    // Блок под игроком - всегда разрешаем
    boolean isBelowPlayer = checkPos.equals(playerPos.down());

    if (!isBelowPlayer) {
        // Проверяем, является ли блок диагональным
        boolean isDiagonal = Math.abs(checkPos.getX() - playerPos.getX()) > 0 &&
                             Math.abs(checkPos.getZ() - playerPos.getZ()) > 0;

        if (isDiagonal) {
            continue; // Жёстко отсекаем все диагонали
        }
    }
}

                    Block block = world.getBlockState(checkPos).getBlock();

                    // Проверяем, является ли блок подходящим для паркура
                    if (isSuitableParkourBlock(block, world, checkPos, config)) {
                        blocks.add(checkPos);
                    }
                    // Если запрещено диагональное движение, проверяем направление
if (!config.isAllowDiagonalMovement()) {
    // Проверяем, что блок находится прямо по оси X или Z
    boolean isStraightLine =
        (Math.abs(checkPos.getX() - playerPos.getX()) <= 1 &&
         Math.abs(checkPos.getZ() - playerPos.getZ()) == 0) ||
        (Math.abs(checkPos.getZ() - playerPos.getZ()) <= 1 &&
         Math.abs(checkPos.getX() - playerPos.getX()) == 0);

    // Дополнительно проверяем, что разница по одной оси не больше 1
    // а по другой оси равна 0

    if (!isStraightLine) {
        continue; // Пропускаем диагональные блоки
    }
}
                }
            }
        }

        lastScanCount = blocks.size();
        return blocks;
    }

    public boolean isSuitableParkourBlock(Block block, World world, BlockPos pos, ModConfig config) {
        if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
            return false;
        }

        if (UNSAFE_BLOCKS.contains(block)) {
            return false;
        }

        // Проверка по игнор-листу
        if (config.isUseIgnoreList()) {
            String blockId = Registries.BLOCK.getId(block).toString();
            if (config.getIgnoredBlocks().contains(blockId)) {
                return false;
            }
        }

        // Проверка по белому списку блоков
        if (config.isUseBlockWhitelist()) {
            String blockId = Registries.BLOCK.getId(block).toString();
            if (!config.getAllowedBlocks().contains(blockId)) {
                return false;
            }
        }

        // Проверяем, есть ли место над блоком
        BlockPos abovePos = pos.up();
        Block aboveBlock = world.getBlockState(abovePos).getBlock();

        return aboveBlock == Blocks.AIR || aboveBlock == Blocks.CAVE_AIR;
    }

    public boolean isSafeBlock(Block block) {
        return !UNSAFE_BLOCKS.contains(block) && block != Blocks.AIR;
    }

    public boolean isPreferredBlock(Block block) {
        return false;
    }

    public int getLastScanCount() {
        return lastScanCount;
    }

    public BlockPos findClosestBlockInDirection(ClientPlayerEntity player, World world, int range, double lookYaw) {
        BlockPos playerPos = player.getBlockPos();

        double rad = Math.toRadians(lookYaw);
        int dx = (int) Math.round(Math.sin(rad));
        int dz = (int) Math.round(Math.cos(rad));

        for (int i = 1; i <= range; i++) {
            BlockPos checkPos = playerPos.add(dx * i, 0, dz * i);

            for (int y = -1; y <= 2; y++) {
                BlockPos heightPos = checkPos.up(y);
                Block block = world.getBlockState(heightPos).getBlock();

                if (isSuitableParkourBlock(block, world, heightPos, AutoParkourMod.getInstance().getConfigManager().getConfig())) {
                    return heightPos;
                }
            }
        }

        return null;
    }
}

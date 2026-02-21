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

import java.util.*;

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

    // Блокировка цели в воздухе
    private BlockPos airTargetLock = null;
    private int airTargetLockTicks = 0;
    private static final int AIR_LOCK_DURATION = 140;

    // Защита от пропавших блоков
    private BlockPos lastJumpedFromBlock = null;
    private int lastJumpCooldown = 0;
    private static final int JUMP_COOLDOWN_TICKS = 55;

    // Флаг для отслеживания прыжка
    private boolean jumpJustPerformed = false;

    private final ParkourExecutor executor;
    private final BlockScanner blockScanner;
    private final MovementHelper movementHelper;
    private final ModConfig config;

    // Защита от пропавших блоков
    private BlockPos lastTargetBeforeJump = null;
    private int lastTargetCooldown = 0;
    private static final int LAST_TARGET_COOLDOWN_TICKS = 40; // 2 секунды

    public ParkourManager() {
        this.config = AutoParkourMod.getInstance().getConfigManager().getConfig();
        this.executor = new ParkourExecutor();
        this.blockScanner = new BlockScanner();
        this.movementHelper = new MovementHelper();
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // Детекция резкого перемещения (телепорт)
        if (lastPlayerPos != Vec3d.ZERO) {
            double movementSpeed = client.player.getPos().squaredDistanceTo(lastPlayerPos);
            if (movementSpeed > 100) {
                executor.onTeleport();
                currentTargetBlock = null;
                airTargetLock = null;
                lastJumpedFromBlock = null;
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("Teleport detected, resetting target");
                }
            }
        }
        lastPlayerPos = client.player.getPos();

        tickCounter++;
        if (targetSwitchCooldown > 0) targetSwitchCooldown--;
        if (lastJumpCooldown > 0) lastJumpCooldown--;

        // Обновление блокировки цели в воздухе
        if (airTargetLock != null) {
            airTargetLockTicks--;
            if (airTargetLockTicks <= 0 || client.player.isOnGround()) {
                airTargetLock = null;
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("Air target lock released");
                }
            }
        }
        if (lastTargetCooldown > 0) lastTargetCooldown--;
        if (enabled && currentTargetBlock != null) {
            Vec3d currentPos = client.player.getPos();
            if (lastPlayerPos.squaredDistanceTo(currentPos) < 0.001) {
                stuckTicks++;
                if (stuckTicks > 30) {
                    currentTargetBlock = null;
                    airTargetLock = null;
                    stuckTicks = 0;
                }
            } else {
                stuckTicks = 0;
            }
        }

        if (enabled) {
            performParkour(client.player, client.world);
        }
    }

    private void performParkour(ClientPlayerEntity player, World world) {
        if (!isSafe(player, world)) {
            if (config.isSafeFallEnabled()) {
                handleUnsafeSituation(player);
            }
            return;
        }

        // Сканируем блоки
        List<BlockPos> nearbyBlocks = blockScanner.scanNearbyBlocks(player, world, config.getMaxScanRange() + 1);

        // Фильтруем блоки, с которых только что прыгнули
        if (lastJumpedFromBlock != null && lastJumpCooldown > 0) {
            nearbyBlocks.removeIf(block -> block.equals(lastJumpedFromBlock));
            if (config.isDebugMode()) {
                AutoParkourMod.getInstance().getLogger().debug("Filtered out last jumped block: " + lastJumpedFromBlock.toShortString());
            }
        }

        if (nearbyBlocks.isEmpty()) {
            noBlockTicks++;
            if (noBlockTicks > 15) {
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("No blocks nearby");
                }
                noBlockTicks = 0;
            }
            return;
        } else {
            noBlockTicks = 0;
        }

        // Если мы в воздухе и есть заблокированная цель - используем её
        if (!player.isOnGround() && airTargetLock != null) {
            boolean targetExists = false;
            for (BlockPos block : nearbyBlocks) {
                if (block.equals(airTargetLock)) {
                    targetExists = true;
                    break;
                }
            }

            if (targetExists) {
                currentTargetBlock = airTargetLock;
                isParkouring = true;
                executor.executeMovement(player, airTargetLock, config);
                return;
            } else {
                airTargetLock = null;
            }
        }
// Фильтруем блоки, с которых только что прыгнули
if (lastJumpedFromBlock != null && lastJumpCooldown > 0) {
    nearbyBlocks.removeIf(block -> block.equals(lastJumpedFromBlock));
    if (config.isDebugMode()) {
        AutoParkourMod.getInstance().getLogger().debug("Filtered out last jumped block: " + lastJumpedFromBlock.toShortString());
    }
}

// НОВОЕ: Фильтруем предыдущую цель, если она в кулдауне
if (lastTargetBeforeJump != null && lastTargetCooldown > 0) {
    nearbyBlocks.removeIf(block -> block.equals(lastTargetBeforeJump));
    if (config.isDebugMode()) {
        AutoParkourMod.getInstance().getLogger().debug("Filtered out last target: " + lastTargetBeforeJump.toShortString());
    }
}
        // Находим оптимальную цель с учётом preferStraightDirections
        BlockPos targetBlock = findOptimalTargetBlock(player, nearbyBlocks, world);

        if (targetBlock != null) {
            // Если мы на земле - можем менять цель свободно
            // Если мы на земле - можем менять цель свободно
if (player.isOnGround()) {
    if (!targetBlock.equals(currentTargetBlock) && targetSwitchCooldown <= 0) {
        // Сохраняем предыдущую цель перед сменой
        if (currentTargetBlock != null) {
            lastTargetBeforeJump = currentTargetBlock;
            lastTargetCooldown = LAST_TARGET_COOLDOWN_TICKS;
        }

        lastTargetBlock = currentTargetBlock;
        currentTargetBlock = targetBlock;
        targetSwitchCooldown = 3;

        if (config.isDebugMode()) {
            AutoParkourMod.getInstance().getLogger().debug("New target on ground: " + targetBlock.toShortString());
        }
    }
}
            // Если мы в воздухе - блокируем первую цель
            else if (currentTargetBlock == null) {
                currentTargetBlock = targetBlock;
                airTargetLock = targetBlock;
                airTargetLockTicks = AIR_LOCK_DURATION;
                if (config.isDebugMode()) {
                    AutoParkourMod.getInstance().getLogger().debug("Air target locked: " + targetBlock.toShortString());
                }
            }

            if (currentTargetBlock != null) {
                isParkouring = true;

                // Запоминаем блок при прыжке
                if (jumpJustPerformed && player.isOnGround()) {
                    jumpJustPerformed = false;
                }

                if (!player.isOnGround() && lastPlayerPos.y < player.getY() + 0.1 && !jumpJustPerformed) {
                    lastJumpedFromBlock = player.getBlockPos().down();
                    lastJumpCooldown = JUMP_COOLDOWN_TICKS;
                    jumpJustPerformed = true;
                    if (config.isDebugMode()) {
                        AutoParkourMod.getInstance().getLogger().debug("Jump detected from: " + lastJumpedFromBlock.toShortString());
                    }
                }

                executor.executeMovement(player, currentTargetBlock, config);
            }
        } else {
    isParkouring = false;
    if (currentTargetBlock != null && targetSwitchCooldown <= 0 && player.isOnGround()) {
        // Сохраняем цель перед сбросом
        lastTargetBeforeJump = currentTargetBlock;
        lastTargetCooldown = LAST_TARGET_COOLDOWN_TICKS;
        currentTargetBlock = null;
    }
}
    }

    private BlockPos findOptimalTargetBlock(ClientPlayerEntity player, List<BlockPos> blocks, World world) {
    if (blocks.isEmpty()) return null;

    Vec3d playerPos = player.getPos();
    BlockPos playerBlockPos = player.getBlockPos();

    // Удаляем блок под игроком
    blocks.removeIf(block -> block.equals(playerBlockPos.down()));

    if (blocks.isEmpty()) return null;

    // Разделяем блоки на прямые и диагональные
    List<BlockPos> straightBlocks = new ArrayList<>();
    List<BlockPos> diagonalBlocks = new ArrayList<>();

    for (BlockPos block : blocks) {
        double heightDiff = block.getY() - playerPos.y;
        double distance = Math.sqrt(block.getSquaredDistance(playerPos));

        boolean isValidHeight = heightDiff > -3.0 && heightDiff < 4.0;
        boolean isValidDistance = distance > 0.3 && distance < config.getMaxJumpDistance() + 2.0;

        if (!isValidHeight || !isValidDistance) continue;

        int dx = block.getX() - playerBlockPos.getX();
        int dz = block.getZ() - playerBlockPos.getZ();

        if (dx == 0 || dz == 0) {
            straightBlocks.add(block);
        } else if (dx != 0 && dz != 0) {
            diagonalBlocks.add(block);
        }
    }

        // Выбираем список в зависимости от настройки preferStraightDirections
        List<BlockPos> preferredList;
        List<BlockPos> fallbackList;

        if (config.isPreferStraightDirections()) {
            preferredList = straightBlocks;
            fallbackList = diagonalBlocks;
        } else {
            preferredList = diagonalBlocks;
            fallbackList = straightBlocks;
        }

        // Сортируем предпочтительный список по расстоянию
        // Сортируем предпочтительный список с бонусом для прямых направлений
// Сортируем предпочтительный список по расстоянию с жёстким штрафом за диагональ
// Сортируем предпочтительный список с МАКСИМАЛЬНЫМ штрафом за диагональ
if (!preferredList.isEmpty()) {
    preferredList.sort((a, b) -> {
        double distA = a.getSquaredDistance(playerPos);
        double distB = b.getSquaredDistance(playerPos);

        // Определяем, является ли блок диагональным
        int dxA = a.getX() - playerBlockPos.getX();
        int dzA = a.getZ() - playerBlockPos.getZ();
        int dxB = b.getX() - playerBlockPos.getX();
        int dzB = b.getZ() - playerBlockPos.getZ();

        boolean isDiagonalA = dxA != 0 && dzA != 0;
        boolean isDiagonalB = dxB != 0 && dzB != 0;

        // МАКСИМАЛЬНЫЙ ШТРАФ для диагоналей
        if (isDiagonalA) {
            distA *= 100.0; // Огромный штраф
        }
        if (isDiagonalB) {
            distB *= 100.0;
        }

        return Double.compare(distA, distB);
    });

    // Проверяем доступность
    for (BlockPos block : preferredList) {
        // Пропускаем диагональные блоки полностью
        int dx = block.getX() - playerBlockPos.getX();
        int dz = block.getZ() - playerBlockPos.getZ();
        boolean isDiagonal = dx != 0 && dz != 0;

        if (isDiagonal && config.isPreferStraightDirections()) {
            continue; // Не берём диагонали вообще
        }

        if (isBlockReachable(player, block, world)) {
            return block;
        }
    }

    // Если ничего не нашли, пробуем первый в списке (но без диагоналей)
    for (BlockPos block : preferredList) {
        int dx = block.getX() - playerBlockPos.getX();
        int dz = block.getZ() - playerBlockPos.getZ();
        boolean isDiagonal = dx != 0 && dz != 0;

        if (!isDiagonal) {
            return block;
        }
    }

    // Если остались только диагонали, возвращаем null
    return null;
}

        // Если предпочтительный список пуст, используем запасной
        if (!fallbackList.isEmpty()) {
            fallbackList.sort((a, b) -> {
                double distA = a.getSquaredDistance(playerPos);
                double distB = b.getSquaredDistance(playerPos);
                return Double.compare(distA, distB);
            });

            for (BlockPos block : fallbackList) {
                if (isBlockReachable(player, block, world)) {
                    return block;
                }
            }

            return fallbackList.get(0);
        }

        return null;
    }

    private boolean isBlockReachable(ClientPlayerEntity player, BlockPos block, World world) {
        double distance = Math.sqrt(block.getSquaredDistance(player.getPos()));
        if (distance > config.getMaxJumpDistance() + 0.5) return false;

        double heightDiff = block.getY() - player.getY();
        if (heightDiff > 2.0 || heightDiff < -2.5) return false;

        return true;
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
                    if (blockScanner.isSafeBlock(world.getBlockState(checkPos).getBlock())) {
                        executor.tryReachNearestBlock(player, checkPos, config);
                        return;
                    }
                }
            }
        }

        movementHelper.safeFall(player);
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

package com.autoparkour.parkour;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig;
import com.autoparkour.learning.AutoparkourLearning;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ParkourExecutor {

    // === ОБУЧЕНИЕ ===
    private final AutoparkourLearning learning = AutoparkourLearning.getInstance();

    // === Основные cooldown'ы ===
    private int jumpCooldown = 0;
    private int ticksOnGround = 0;
    private int stuckTicks = 0;
    private Vec3d lastPos = Vec3d.ZERO;

    // === Детекция края ===
    private int edgeTicks = 0;

    // === Спринт ===
    private int sprintCooldown = 0;

    // === Прицеливание ===
    private float cameraResponsiveness = 1.2f;
    private float lastYawError = 0f;
    private int cameraLockTicks = 0;

    // === Прыжки (теперь с обучением) ===
    private float jumpBoost;      // будет загружено из learning
    private float airControl;     // будет загружено из learning

    // === Блокировка цели ===
    private BlockPos lockedTarget = null;
    private int targetLockTicks = 0;
    private int noJumpTicks = 0;
    private int airTicks = 0;

    // === ФЛАГ ТЕЛЕПОРТА ===
    private boolean teleportDetected = false;
    private Vec3d preTeleportPos = Vec3d.ZERO;
    private int teleportGraceTicks = 0;

    // === Для отслеживания результатов прыжков ===
    private double lastJumpDistance = 0;
    private boolean lastJumpOvershoot = false;
    private Vec3d jumpStartPos = Vec3d.ZERO;
    private boolean wasInAir = false;

    public ParkourExecutor() {
        // Загружаем параметры из обучения
        this.jumpBoost = learning.getJumpBoost();
        this.airControl = learning.getAirControl();
    }

    public void executeMovement(ClientPlayerEntity player, BlockPos targetBlock, ModConfig config) {
        if (player == null || targetBlock == null) return;

        // === ТЕЛЕПОРТ-ДЕТЕКЦИЯ ===
        Vec3d currentPos = player.getPos();
        if (preTeleportPos != Vec3d.ZERO && preTeleportPos.distanceTo(currentPos) > 5.0 && player.isOnGround()) {
            teleportDetected = true;
            teleportGraceTicks = 15;
            lockedTarget = null;
            learning.onTeleport(); // Уведомляем обучение о телепорте
        }
        preTeleportPos = currentPos;
        if (teleportGraceTicks > 0) teleportGraceTicks--;

        // === Принудительная смена цели в воздухе ===
        boolean forceRetarget = (!player.isOnGround() && noJumpTicks > 4) || teleportDetected;
        if (lockedTarget == null || !lockedTarget.equals(targetBlock) || forceRetarget) {
            lockedTarget = targetBlock;
            targetLockTicks = 0;
            cameraLockTicks = 0;
        } else {
            targetLockTicks++;
        }

        if (targetLockTicks > (player.isOnGround() ? 35 : 12) && !teleportDetected) {
            lockedTarget = targetBlock;
            targetLockTicks = 0;
        }

        if (teleportGraceTicks > 0) {
            teleportDetected = false;
            return;
        }

        Vec3d targetPos = new Vec3d(lockedTarget.getX() + 0.5, lockedTarget.getY() + 1.0, lockedTarget.getZ() + 0.5);

        double dx = targetPos.x - currentPos.x;
        double dz = targetPos.z - currentPos.z;
        double horizDist = Math.hypot(dx, dz);
        double heightDiff = targetPos.y - currentPos.y;

        // === ОТСЛЕЖИВАНИЕ РЕЗУЛЬТАТОВ ПРЫЖКА ===
        if (!player.isOnGround() && !wasInAir) {
            // Только что взлетели
            jumpStartPos = currentPos;
            lastJumpDistance = horizDist;
        }

        if (player.isOnGround() && wasInAir) {
            // Только что приземлились
            double landingDist = Math.sqrt(player.getBlockPos().getSquaredDistance(lockedTarget));
            boolean overshoot = landingDist > 2.0 && lastJumpDistance < 3.0;

            if (landingDist < 1.5) {
                learning.onSuccessfulJump(lastJumpDistance);
            } else {
                learning.onFailedJump(overshoot);
            }

            // Обновляем параметры из обучения
            jumpBoost = learning.getJumpBoost();
            airControl = learning.getAirControl();
        }
        wasInAir = !player.isOnGround();

        // === Тики на земле / в воздухе ===
        if (player.isOnGround()) {
            ticksOnGround++;
            noJumpTicks = 0;
            airTicks = 0;
        } else {
            ticksOnGround = 0;
            noJumpTicks++;
            airTicks++;
        }

        if (sprintCooldown > 0) sprintCooldown--;

        // === Анти-застревание ===
        if (lastPos.squaredDistanceTo(currentPos) < 0.001 && noJumpTicks > 3) {
            stuckTicks++;
            if (stuckTicks > 5) {
                player.jump();
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }
        lastPos = currentPos;

        // === Детекция края ===
        boolean isOnEdge = detectEdge(player);

        // === КАМЕРА ===
        updateCameraThatActuallyWorks(player, targetPos, config, horizDist, isOnEdge);

        // === Вектора направления ===
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d lookFlat = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
        Vec3d toTargetFlat = new Vec3d(dx, 0, dz).normalize();
        double dot = (toTargetFlat.length() > 0.1) ? lookFlat.dotProduct(toTargetFlat) : 1.0;

        // === Спринт с учётом обучения ===
        if (config.isAutoSprintEnabled() && sprintCooldown <= 0) {
            boolean shouldSprint = (horizDist > 1.8 && dot > 0.5) || (isOnEdge && horizDist > 1.5);
            if (horizDist < 1.4) shouldSprint = false;

            // Учитываем надёжность спринта из обучения
            if (learning.getSprintReliability() < 0.7f && shouldSprint) {
                shouldSprint = false;
            }

            player.setSprinting(shouldSprint);
        }

        // === Прыжки ===
        boolean shouldJump = updateJumpPolicy(player, config, horizDist, heightDiff, isOnEdge, dot);

        if (shouldJump && player.isOnGround() && jumpCooldown <= 0) {
            executeJump(player, lookFlat, horizDist, heightDiff, config);
        }

        // === Движение к цели ===
        if (dot > 0.15 || horizDist < 1.6) {
            moveTowards(player, targetPos, config, horizDist, isOnEdge);
        }

        // === Торможение после успеха ===
        if (horizDist < 0.4 && player.isOnGround() && !shouldJump && !isOnEdge) {
            Vec3d v = player.getVelocity();
            player.setVelocity(v.multiply(0.2, 1.0, 0.2));
        }

        // === Cooldown обновление ===
        if (jumpCooldown > 0) jumpCooldown--;
    }

    private void updateCameraThatActuallyWorks(ClientPlayerEntity player, Vec3d target, ModConfig config,
                                               double horizDist, boolean onEdge) {
        Vec3d eye = player.getEyePos();

        double dx = target.x - eye.x;
        double dz = target.z - eye.z;

        if (Math.hypot(dx, dz) < 0.3) return;

        float targetYaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float currentYaw = player.getYaw();

        float yawError = targetYaw - currentYaw;
        while (yawError > 180f) yawError -= 360f;
        while (yawError < -180f) yawError += 360f;

        lastYawError = yawError;

        if (Math.abs(yawError) > 60f) {
            player.setYaw(targetYaw);
            cameraLockTicks = 0;
            return;
        }

        float baseSpeed = 25f + 45f * (float) config.getLookSpeed();
        float boost = 1.0f;

        if (!player.isOnGround() && horizDist > 2.0) boost = 1.8f;
        if (onEdge) boost = 1.5f;
        if (Math.abs(yawError) > 30f) boost *= 1.5f;

        float step = MathHelper.clamp(yawError,
                -baseSpeed * boost * cameraResponsiveness,
                baseSpeed * boost * cameraResponsiveness);

        player.setYaw(currentYaw + step);

        float remaining = targetYaw - player.getYaw();
        while (remaining > 180f) remaining -= 360f;
        while (remaining < -180f) remaining += 360f;

        if (Math.abs(remaining) > 20f && cameraLockTicks < 3) {
            player.setYaw(player.getYaw() + MathHelper.clamp(remaining, -35f, 35f));
            cameraLockTicks++;
        } else {
            cameraLockTicks = 0;
        }
    }

    private boolean detectEdge(ClientPlayerEntity player) {
        if (!player.isOnGround()) {
            edgeTicks = 0;
            return false;
        }

        BlockPos below = player.getBlockPos().down();
        if (player.getWorld().getBlockState(below).isAir()) return false;

        Vec3d look = player.getRotationVec(1.0F);
        look = new Vec3d(look.x, 0, look.z).normalize();

        BlockPos front = below.add(
                MathHelper.clamp((int) Math.round(look.x), -1, 1),
                0,
                MathHelper.clamp((int) Math.round(look.z), -1, 1)
        );

        boolean edgeNow = player.getWorld().getBlockState(front).isAir();

        if (edgeNow) {
            edgeTicks++;
        } else {
            edgeTicks = 0;
        }

        return edgeNow && edgeTicks > 0;
    }

    private boolean updateJumpPolicy(ClientPlayerEntity player, ModConfig config,
                                     double dist, double height, boolean onEdge, double dot) {
        if (!config.isAutoJumpEnabled() || jumpCooldown > 0 || !player.isOnGround() || ticksOnGround < 1) {
            return false;
        }

        // Используем optimalJumpWindow из обучения
        int window = learning.getOptimalJumpWindow();

        if (onEdge && dist > 0.8) return true;
        if (dist > 1.5 && dot > 0.4 && ticksOnGround > window - 5) return true;
        if (height > 0.5 && dist < 2.0 && dot > 0.3) return true;

        return false;
    }

    private void executeJump(ClientPlayerEntity player, Vec3d lookFlat,
                             double dist, double height, ModConfig config) {
        player.jump();
        jumpCooldown = 7;
        ticksOnGround = 0;
        edgeTicks = 0;
        sprintCooldown = 6;

        Vec3d vel = player.getVelocity();

        // Сила прыжка из обучения
        float boost = jumpBoost;
        if (dist > 2.2) boost += 0.08f;
        if (height > 0.6) boost += 0.05f;

        player.setVelocity(
                vel.x + lookFlat.x * boost,
                vel.y,
                vel.z + lookFlat.z * boost
        );
    }

    private void moveTowards(ClientPlayerEntity player, Vec3d target, ModConfig config,
                             double dist, boolean onEdge) {
        Vec3d pos = player.getPos();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double len = Math.hypot(dx, dz);
        if (len < 0.1) return;

        dx /= len;
        dz /= len;

        if (!config.isAllowDiagonalMovement()) {
            if (Math.abs(dx) > Math.abs(dz)) {
                dz = 0;
                dx = Math.signum(dx);
            } else {
                dx = 0;
                dz = Math.signum(dz);
            }
        }

        double speed = 0.24;
        if (onEdge) speed = 0.28;
        if (player.isSprinting()) speed += 0.05;
        if (dist > 2.0) speed += 0.03;
        if (dist < 1.0) speed = 0.17;

        if (!player.isOnGround()) speed *= airControl;

        player.setVelocity(dx * speed, player.getVelocity().y, dz * speed);
    }

    public void tryReachNearestBlock(ClientPlayerEntity player, BlockPos block, ModConfig config) {
        if (player == null || block == null) return;
        Vec3d pos = new Vec3d(block.getX() + 0.5, block.getY() + 1.0, block.getZ() + 0.5);
        moveTowards(player, pos, config, player.getPos().distanceTo(pos), false);
    }

    public void resetCooldowns() {
        jumpCooldown = 0;
        ticksOnGround = 0;
        stuckTicks = 0;
        edgeTicks = 0;
        sprintCooldown = 0;
        lockedTarget = null;
        targetLockTicks = 0;
        noJumpTicks = 0;
        airTicks = 0;
        teleportDetected = false;
        teleportGraceTicks = 0;
        cameraLockTicks = 0;
    }

    public void onTeleport() {
        teleportDetected = true;
        teleportGraceTicks = 15;
        lockedTarget = null;
        learning.onTeleport();
    }
}

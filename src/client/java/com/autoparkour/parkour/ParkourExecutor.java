package com.autoparkour.parkour;

import com.autoparkour.config.ModConfig;
import com.autoparkour.learning.AutoparkourLearning;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ParkourExecutor {

    private final AutoparkourLearning learning = AutoparkourLearning.getInstance();

    private int jumpCooldown = 0;
    private int ticksOnGround = 0;
    private int stuckTicks = 0;
    private Vec3d lastPos = Vec3d.ZERO;

    private int edgeTicks = 0;
    private int sprintCooldown = 0;

    private float cameraResponsiveness = 1.2f;
    private float lastYawError = 0f;
    private int cameraLockTicks = 0;

    private float jumpBoost;
    private float airControl;

    private BlockPos lockedTarget = null;
    private int targetLockTicks = 0;
    private int noJumpTicks = 0;
    private int airTicks = 0;

    private boolean teleportDetected = false;
    private Vec3d preTeleportPos = Vec3d.ZERO;
    private int teleportGraceTicks = 0;

    private double lastJumpDistance = 0;
    private Vec3d jumpStartPos = Vec3d.ZERO;
    private boolean wasInAir = false;

    public ParkourExecutor() {
        this.jumpBoost = learning.getJumpBoost();
        this.airControl = learning.getAirControl();
    }

    public void executeMovement(ClientPlayerEntity player, BlockPos targetBlock, ModConfig config) {
        if (player == null || targetBlock == null || config == null) return;

        if (!isLandingStillAvailable(player, targetBlock)) {
            releaseVanishedTarget(player, targetBlock);
            return;
        }

        Vec3d currentPos = player.getPos();
        if (!preTeleportPos.equals(Vec3d.ZERO) && preTeleportPos.distanceTo(currentPos) > 5.0 && player.isOnGround()) {
            teleportDetected = true;
            teleportGraceTicks = 15;
            lockedTarget = null;
            learning.onTeleport();
        }
        preTeleportPos = currentPos;
        if (teleportGraceTicks > 0) teleportGraceTicks--;

        boolean forceRetarget = (!player.isOnGround() && noJumpTicks > 4) || teleportDetected;
        if (lockedTarget == null || !lockedTarget.equals(targetBlock) || forceRetarget) {
            lockedTarget = targetBlock;
            targetLockTicks = 0;
            cameraLockTicks = 0;
        } else {
            targetLockTicks++;
        }

        if (lockedTarget != null && !isLandingStillAvailable(player, lockedTarget)) {
            releaseVanishedTarget(player, lockedTarget);
            return;
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

        if (!player.isOnGround() && !wasInAir) {
            jumpStartPos = currentPos;
            lastJumpDistance = horizDist;
        }

        if (player.isOnGround() && wasInAir) {
            double landingDist = horizontalDistanceToBlockCenter(player.getPos(), lockedTarget);
            boolean overshoot = landingDist > 1.8 && lastJumpDistance < 3.0;

            if (landingDist < 1.15) {
                learning.onSuccessfulJump(lastJumpDistance);
            } else {
                learning.onFailedJump(overshoot);
            }

            jumpBoost = learning.getJumpBoost();
            airControl = learning.getAirControl();
        }
        wasInAir = !player.isOnGround();

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

        if (lastPos.squaredDistanceTo(currentPos) < 0.001 && noJumpTicks > 3) {
            stuckTicks++;
            if (stuckTicks > 5 && isLandingStillAvailable(player, lockedTarget)) {
                player.jump();
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }
        lastPos = currentPos;

        boolean diagonalTarget = isDiagonalTarget(player, lockedTarget, targetPos);
        boolean isOnEdge = detectEdge(player) || isNearFrontEdge(player, lockedTarget) || isNearDiagonalTakeoffEdge(player, lockedTarget, diagonalTarget);

        updateCameraThatActuallyWorks(player, targetPos, config, horizDist, isOnEdge);

        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d lookFlat = normalizeFlat(lookVec);
        Vec3d toTargetFlat = normalizeFlat(new Vec3d(dx, 0, dz));
        double dot = (toTargetFlat.length() > 0.1 && lookFlat.length() > 0.1) ? lookFlat.dotProduct(toTargetFlat) : 1.0;

        if (config.isAutoSprintEnabled() && sprintCooldown <= 0) {
            boolean shouldSprint = (horizDist > 1.45 && dot > 0.35) || (isOnEdge && horizDist > 1.15);
            if (horizDist >= 2.8 && dot > 0.25) shouldSprint = true;
            if (horizDist < 1.15) shouldSprint = false;

            if (learning.getSprintReliability() < 0.7f && shouldSprint && horizDist < 2.8) {
                shouldSprint = false;
            }

            player.setSprinting(shouldSprint);
        }

        boolean shouldJump = updateJumpPolicy(player, config, horizDist, heightDiff, isOnEdge, dot, diagonalTarget);

        if (shouldJump && player.isOnGround() && jumpCooldown <= 0 && isLandingStillAvailable(player, lockedTarget)) {
            executeJump(player, toTargetFlat, horizDist, heightDiff, config, diagonalTarget, isOnEdge);
        }

        boolean aimNearlyReady = Math.abs(lastYawError) < (diagonalTarget ? 62.0f : 50.0f);
        boolean mayMoveWhileTurning = !player.isOnGround() || isOnEdge || horizDist < 1.25 || aimNearlyReady;
        boolean enoughAimForInput = dot > (diagonalTarget ? 0.22 : 0.30);
        if (mayMoveWhileTurning && (enoughAimForInput || horizDist < 1.25 || !player.isOnGround() || isOnEdge)) {
            moveTowards(player, targetPos, config, horizDist, isOnEdge, diagonalTarget);
        }

        if (horizDist < 0.35 && player.isOnGround() && !shouldJump && !isOnEdge) {
            Vec3d v = player.getVelocity();
            player.setVelocity(v.multiply(0.15, 1.0, 0.15));
        }

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

        float yawError = MathHelper.wrapDegrees(targetYaw - currentYaw);
        lastYawError = yawError;

        // Full aim, anti-cheat safe movement: yaw rotation itself is fine, but jumping
        // before the yaw has converged makes the player clip the edge or get corrected.
        float baseSpeed = 5.0f + 2.7f * (float) config.getLookSpeed();
        float boost = 1.0f;

        if (player.isOnGround()) boost = Math.max(boost, 1.35f);
        if (!player.isOnGround() && horizDist > 2.0) boost = Math.max(boost, 1.10f);
        if (onEdge) boost = Math.max(boost, 1.55f);
        if (horizDist >= 3.0) boost = Math.max(boost, 1.28f);
        if (Math.abs(yawError) > 35f) boost *= 1.25f;
        if (Math.abs(yawError) > 70f) boost *= 1.15f;

        float maxClamp = player.isOnGround() ? 56.0f : 40.0f;
        if (onEdge) maxClamp = player.isOnGround() ? 68.0f : 46.0f;
        float maxStep = MathHelper.clamp(baseSpeed * boost * cameraResponsiveness, 8.0f, maxClamp);
        float step = MathHelper.clamp(yawError, -maxStep, maxStep);

        player.setYaw(currentYaw + step);

        float remaining = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        if (Math.abs(remaining) > 28f) {
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

        Vec3d look = normalizeFlat(player.getRotationVec(1.0F));
        if (look.length() < 0.1) return false;

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

    private boolean isNearFrontEdge(ClientPlayerEntity player, BlockPos targetBlock) {
        if (!player.isOnGround() || targetBlock == null) return false;

        BlockPos supportBlock = getSupportBlock(player);
        Vec3d playerPos = player.getPos();
        double centerX = supportBlock.getX() + 0.5;
        double centerZ = supportBlock.getZ() + 0.5;
        double targetX = targetBlock.getX() + 0.5;
        double targetZ = targetBlock.getZ() + 0.5;
        Vec3d dir = normalizeFlat(new Vec3d(targetX - centerX, 0.0, targetZ - centerZ));
        if (dir.length() < 0.1) return false;

        double forwardOffset = (playerPos.x - centerX) * dir.x + (playerPos.z - centerZ) * dir.z;
        return forwardOffset > 0.30;
    }

    private boolean isNearDiagonalTakeoffEdge(ClientPlayerEntity player, BlockPos targetBlock, boolean diagonalTarget) {
        if (!diagonalTarget || !player.isOnGround() || targetBlock == null) return false;

        BlockPos supportBlock = getSupportBlock(player);
        Vec3d pos = player.getPos();

        double supportCenterX = supportBlock.getX() + 0.5;
        double supportCenterZ = supportBlock.getZ() + 0.5;
        double targetCenterX = targetBlock.getX() + 0.5;
        double targetCenterZ = targetBlock.getZ() + 0.5;

        double dirX = Math.signum(targetCenterX - supportCenterX);
        double dirZ = Math.signum(targetCenterZ - supportCenterZ);
        if (dirX == 0.0 || dirZ == 0.0) return false;

        double xOffset = (pos.x - supportCenterX) * dirX;
        double zOffset = (pos.z - supportCenterZ) * dirZ;

        return (xOffset > 0.20 && zOffset > 0.20) || (xOffset + zOffset > 0.50);
    }

    private boolean updateJumpPolicy(ClientPlayerEntity player, ModConfig config,
                                     double dist, double height, boolean onEdge, double dot, boolean diagonalTarget) {
        if (!config.isAutoJumpEnabled() || jumpCooldown > 0 || !player.isOnGround() || ticksOnGround < 1) {
            return false;
        }

        int window = learning.getOptimalJumpWindow();

        float absYawError = Math.abs(lastYawError);

        if (diagonalTarget) {
            if (absYawError > 58f) return false;
            if (dot < 0.45) return false;
            if (onEdge && dist > 0.95 && dist <= 4.70 && dot > 0.50) return true;
            if (dist >= 1.10 && dist < 2.10 && dot > 0.48 && absYawError < 55f) return true;
            if (dist >= 2.10 && dist <= 3.35 && ticksOnGround >= Math.max(1, window - 5) && dot > 0.55) return true;
            if (dist >= 3.35 && dist <= 4.70 && ticksOnGround >= Math.max(1, window - 3) && dot > 0.60) return true;
            return height > 0.5 && dist > 0.6 && dot > 0.45;
        }

        if (dist >= 3.0 && dist <= 4.45) {
            if (absYawError > 45f || dot < 0.62) return false;
            if (onEdge) return true;
            return ticksOnGround >= Math.max(1, window - 3) && dist <= 4.20;
        }

        if (onEdge && dist > 0.8 && dot > 0.50 && absYawError < 55f) return true;
        if (dist > 1.20 && dot > 0.52 && absYawError < 55f && ticksOnGround >= Math.max(1, window - 6)) return true;
        if (height > 0.5 && dist < 2.1 && dot > 0.45 && absYawError < 60f) return true;

        return false;
    }

    private void executeJump(ClientPlayerEntity player, Vec3d directionFlat,
                             double dist, double height, ModConfig config, boolean diagonalTarget, boolean onEdge) {
        if (directionFlat.length() < 0.1) return;

        if (config.isAutoSprintEnabled() && dist > 1.6) {
            player.setSprinting(true);
        }

        player.jump();
        jumpCooldown = dist >= 3.0 ? 7 : 6;
        ticksOnGround = 0;
        edgeTicks = 0;
        sprintCooldown = dist >= 3.0 ? 5 : 4;

        Vec3d vel = player.getVelocity();

        // Calibrated takeoff velocities. In Minecraft 1.21 a sprint jump with these
        // initial horizontal speeds reaches approximately the corresponding tile count.
        float targetSpeed;
        if (dist >= 3.80) {
            targetSpeed = 0.460f; // ~4 block jump
        } else if (dist >= 2.80) {
            targetSpeed = 0.395f; // ~3 block jump
        } else if (dist >= 1.85) {
            targetSpeed = 0.305f; // ~2 block jump
        } else {
            targetSpeed = 0.235f; // ~1 block jump
        }

        targetSpeed = Math.max(targetSpeed, jumpBoost);

        if (diagonalTarget) {
            targetSpeed += 0.020f;
            if (onEdge) targetSpeed += 0.012f;
        }

        if (height > 0.55) targetSpeed += 0.022f;
        if (height < -1.2) targetSpeed -= 0.015f;
        if (config.isPreventOvershoot() && dist < 1.65) {
            targetSpeed = Math.min(targetSpeed, 0.245f);
        }

        float maxSpeed = diagonalTarget ? 0.520f : 0.495f;
        targetSpeed = MathHelper.clamp(targetSpeed, 0.18f, maxSpeed);

        double forwardSpeed = vel.x * directionFlat.x + vel.z * directionFlat.z;
        double sideX = vel.x - directionFlat.x * forwardSpeed;
        double sideZ = vel.z - directionFlat.z * forwardSpeed;
        // Set forward velocity directly to targetSpeed (keep existing velocity if it is
        // already larger so we never slow the player down mid-takeoff).
        double newForwardSpeed = Math.max(forwardSpeed, targetSpeed);
        double sideRetention = diagonalTarget ? 0.70 : 0.55;

        player.setVelocity(
                directionFlat.x * newForwardSpeed + sideX * sideRetention,
                vel.y,
                directionFlat.z * newForwardSpeed + sideZ * sideRetention
        );
    }

    private void moveTowards(ClientPlayerEntity player, Vec3d target, ModConfig config,
                             double dist, boolean onEdge, boolean diagonalTarget) {
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

        double speed = 0.145;
        if (onEdge) speed = 0.175;
        if (player.isSprinting()) speed += 0.035;
        if (dist > 2.5) speed += 0.020;
        if (dist > 3.4) speed += 0.012;
        if (diagonalTarget && dist > 1.25) speed += 0.014;
        if (dist < 1.0) speed = diagonalTarget ? 0.135 : 0.120;
        speed = MathHelper.clamp(speed, 0.10, player.isOnGround() ? 0.235 : 0.300);

        if (!player.isOnGround()) {
            speed *= MathHelper.clamp(airControl, 0.80f, 0.93f);
            Vec3d v = player.getVelocity();
            double forwardSpeed = v.x * dx + v.z * dz;
            double desiredForwardSpeed = speed;

            if (dist > 2.7) desiredForwardSpeed = Math.max(desiredForwardSpeed, diagonalTarget ? 0.300 : 0.285);
            if (dist < 1.15) desiredForwardSpeed = Math.min(desiredForwardSpeed, diagonalTarget ? 0.245 : 0.225);
            desiredForwardSpeed = Math.min(desiredForwardSpeed, diagonalTarget ? 0.325 : 0.305);

            double sideX = v.x - dx * forwardSpeed;
            double sideZ = v.z - dz * forwardSpeed;
            double addForward = MathHelper.clamp(desiredForwardSpeed - forwardSpeed, 0.0, diagonalTarget ? 0.026 : 0.022);
            double sideRetention = diagonalTarget ? 0.70 : 0.78;

            player.setVelocity(
                    v.x + dx * addForward - sideX * (1.0 - sideRetention) * 0.35,
                    v.y,
                    v.z + dz * addForward - sideZ * (1.0 - sideRetention) * 0.35
            );
            return;
        }

        Vec3d v = player.getVelocity();
        double desiredX = dx * speed;
        double desiredZ = dz * speed;
        double maxDelta = diagonalTarget ? 0.030 : 0.026;
        double newX = v.x + MathHelper.clamp(desiredX - v.x, -maxDelta, maxDelta);
        double newZ = v.z + MathHelper.clamp(desiredZ - v.z, -maxDelta, maxDelta);
        player.setVelocity(newX, v.y, newZ);
    }

    public void tryReachNearestBlock(ClientPlayerEntity player, BlockPos block, ModConfig config) {
        if (player == null || block == null) return;
        Vec3d pos = new Vec3d(block.getX() + 0.5, block.getY() + 1.0, block.getZ() + 0.5);
        boolean diagonalTarget = isDiagonalTarget(player, block, pos);
        moveTowards(player, pos, config, player.getPos().distanceTo(pos), false, diagonalTarget);
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



    public void reloadLearning() {
        learning.load();
        jumpBoost = learning.getJumpBoost();
        airControl = learning.getAirControl();
        resetCooldowns();
    }

    public void onTeleport() {
        teleportDetected = true;
        teleportGraceTicks = 15;
        lockedTarget = null;
        learning.onTeleport();
    }

    private boolean isLandingStillAvailable(ClientPlayerEntity player, BlockPos blockPos) {
        if (player == null || blockPos == null || player.getWorld() == null) return false;
        World world = player.getWorld();

        if (blockPos.getY() < world.getBottomY() || blockPos.getY() > world.getTopYInclusive()) return false;
        if (blockPos.equals(player.getBlockPos().down())) return false;

        BlockState state = world.getBlockState(blockPos);
        Block block = state.getBlock();
        if (state.isAir() || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) return false;
        if (block == Blocks.WATER || block == Blocks.LAVA || block == Blocks.FIRE || block == Blocks.SOUL_FIRE) return false;
        if (block == Blocks.CACTUS || block == Blocks.MAGMA_BLOCK || block == Blocks.POWDER_SNOW || block == Blocks.COBWEB) return false;

        return isHeadSpaceFree(world, blockPos.up()) && isHeadSpaceFree(world, blockPos.up(2));
    }

    private boolean isHeadSpaceFree(World world, BlockPos pos) {
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopYInclusive()) return false;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return state.isAir() || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR || block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.VINE;
    }

    private void releaseVanishedTarget(ClientPlayerEntity player, BlockPos targetBlock) {
        if (lockedTarget != null && lockedTarget.equals(targetBlock)) {
            lockedTarget = null;
        }
        targetLockTicks = 0;
        cameraLockTicks = 0;

        Vec3d velocity = player.getVelocity();
        double horizontalBrake = player.isOnGround() ? 0.20 : 0.72;
        player.setVelocity(velocity.x * horizontalBrake, velocity.y, velocity.z * horizontalBrake);
    }

    private boolean isDiagonalTarget(ClientPlayerEntity player, BlockPos targetBlock, Vec3d targetPos) {
        if (player == null || targetBlock == null || targetPos == null) return false;

        BlockPos supportBlock = getSupportBlock(player);
        if (supportBlock.getX() != targetBlock.getX() && supportBlock.getZ() != targetBlock.getZ()) {
            return true;
        }

        Vec3d pos = player.getPos();
        return Math.abs(targetPos.x - pos.x) > 0.75 && Math.abs(targetPos.z - pos.z) > 0.75;
    }

    private BlockPos getSupportBlock(ClientPlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        BlockPos below = feet.down();
        if (!player.getWorld().getBlockState(below).isAir()) {
            return below;
        }
        return feet;
    }

    private Vec3d normalizeFlat(Vec3d vector) {
        Vec3d flat = new Vec3d(vector.x, 0.0, vector.z);
        if (flat.length() < 0.0001) return Vec3d.ZERO;
        return flat.normalize();
    }

    private double horizontalDistanceToBlockCenter(Vec3d pos, BlockPos blockPos) {
        double dx = blockPos.getX() + 0.5 - pos.x;
        double dz = blockPos.getZ() + 0.5 - pos.z;
        return Math.hypot(dx, dz);
    }
}

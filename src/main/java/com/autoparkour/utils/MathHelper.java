package com.autoparkour.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class MathHelper {

    public static double distance2D(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double distance2D(Vec3d pos1, Vec3d pos2) {
        double dx = pos1.x - pos2.x;
        double dz = pos1.z - pos2.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double distance3D(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(pos1.getSquaredDistance(pos2));
    }

    public static double distance3D(Vec3d pos1, Vec3d pos2) {
        double dx = pos1.x - pos2.x;
        double dy = pos1.y - pos2.y;
        double dz = pos1.z - pos2.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double getHorizontalAngle(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return Math.toDegrees(Math.atan2(dz, dx)) - 90;
    }

    public static double getVerticalAngle(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        return Math.toDegrees(Math.atan2(dy, horizontalDist));
    }

    public static Vec3d getDirectionVector(float yaw, float pitch) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        double xzLength = Math.cos(pitchRad);

        double x = -xzLength * Math.sin(yawRad);
        double y = -Math.sin(pitchRad);
        double z = xzLength * Math.cos(yawRad);

        return new Vec3d(x, y, z);
    }

    public static boolean isInRange(BlockPos pos1, BlockPos pos2, double range) {
        return pos1.getSquaredDistance(pos2) <= range * range;
    }

    public static boolean isInRange(Vec3d pos1, Vec3d pos2, double range) {
        double dx = pos1.x - pos2.x;
        double dy = pos1.y - pos2.y;
        double dz = pos1.z - pos2.z;
        return (dx * dx + dy * dy + dz * dz) <= range * range;
    }

    public static Vec3d lerp(Vec3d start, Vec3d end, float delta) {
        return new Vec3d(
                start.x + (end.x - start.x) * delta,
                start.y + (end.y - start.y) * delta,
                start.z + (end.z - start.z) * delta
        );
    }

    public static BlockPos getBlockPosFromLook(Vec3d origin, Vec3d direction, double distance) {
        return BlockPos.ofFloored(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance
        );
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double wrapDegrees(double degrees) {
        degrees = degrees % 360.0;
        if (degrees >= 180.0) {
            degrees -= 360.0;
        }
        if (degrees < -180.0) {
            degrees += 360.0;
        }
        return degrees;
    }

    public static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees >= 180.0f) {
            degrees -= 360.0f;
        }
        if (degrees < -180.0f) {
            degrees += 360.0f;
        }
        return degrees;
    }
}

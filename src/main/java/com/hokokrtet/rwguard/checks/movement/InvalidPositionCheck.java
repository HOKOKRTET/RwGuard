package com.hokokrtet.rwguard.checks.movement;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class InvalidPositionCheck extends AbstractCheck {
    public InvalidPositionCheck(RwGuard plugin) {
        super(plugin, "InvalidPosition", PacketCategory.MOVEMENT);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        double x = Double.NaN;
        double y = Double.NaN;
        double z = Double.NaN;
        float yaw = Float.NaN;
        float pitch = Float.NaN;

        try {
            Class<?> clazz = packet.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                if (f.getType() == double.class) {
                    double val = f.getDouble(packet);
                    if (Double.isNaN(x)) x = val;
                    else if (Double.isNaN(y)) y = val;
                    else if (Double.isNaN(z)) z = val;
                } else if (f.getType() == float.class) {
                    float val = f.getFloat(packet);
                    if (Float.isNaN(yaw)) yaw = val;
                    else if (Float.isNaN(pitch)) pitch = val;
                }
            }
        } catch (Throwable ignored) {
            return CheckResult.allow();
        }

        // Check for NaN or Infinite coordinates
        if (!Double.isNaN(x) && (Double.isInfinite(x) || Double.isNaN(x))) {
            return CheckResult.cancel("NaN or Infinite X coordinate", 4);
        }
        if (!Double.isNaN(y) && (Double.isInfinite(y) || Double.isNaN(y))) {
            return CheckResult.cancel("NaN or Infinite Y coordinate", 4);
        }
        if (!Double.isNaN(z) && (Double.isInfinite(z) || Double.isNaN(z))) {
            return CheckResult.cancel("NaN or Infinite Z coordinate", 4);
        }

        // Coordinate bounds check
        double maxCoord = plugin.getConfigManager().getMovementMaxCoord();
        if (!Double.isNaN(x) && Math.abs(x) > maxCoord) {
            return CheckResult.cancel("X coordinate out of bounds (" + x + ")", 4);
        }
        if (!Double.isNaN(y) && (y > 4000.0 || y < -2000.0)) {
            return CheckResult.cancel("Y coordinate out of bounds (" + y + ")", 4);
        }
        if (!Double.isNaN(z) && Math.abs(z) > maxCoord) {
            return CheckResult.cancel("Z coordinate out of bounds (" + z + ")", 4);
        }

        // Pitch sanity check (-90 to +90 degrees)
        if (!Float.isNaN(pitch) && (pitch > 90.5f || pitch < -90.5f || Float.isInfinite(pitch))) {
            return CheckResult.cancel("Illegal pitch value (" + pitch + ")", 2);
        }

        return CheckResult.allow();
    }
}

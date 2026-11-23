package com.hokokrtet.rwguard.checks.world;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;

public class ProjectileVelocityCheck extends AbstractCheck {
    public ProjectileVelocityCheck(RwGuard plugin) {
        super(plugin, "ProjectileVelocity", PacketCategory.OTHER);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        // Intercepted mostly at Bukkit level or velocity packets
        return CheckResult.allow();
    }
}

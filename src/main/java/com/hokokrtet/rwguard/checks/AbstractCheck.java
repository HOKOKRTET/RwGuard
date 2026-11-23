package com.hokokrtet.rwguard.checks;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;

public abstract class AbstractCheck {
    protected final RwGuard plugin;
    private final String name;
    private final PacketCategory category;

    public AbstractCheck(RwGuard plugin, String name, PacketCategory category) {
        this.plugin = plugin;
        this.name = name;
        this.category = category;
    }

    public abstract boolean isEnabled();

    public abstract CheckResult process(CheckContext context);

    public String getName() {
        return name;
    }

    public PacketCategory getCategory() {
        return category;
    }
}

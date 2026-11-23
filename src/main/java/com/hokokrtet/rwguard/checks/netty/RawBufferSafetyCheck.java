package com.hokokrtet.rwguard.checks.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import io.netty.buffer.ByteBuf;

public class RawBufferSafetyCheck extends AbstractCheck {
    public RawBufferSafetyCheck(RwGuard plugin) {
        super(plugin, "RawBufferSafety", PacketCategory.OTHER);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getGlobalByteLimit() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        int byteSize = context.getByteSize();
        int maxBytes = plugin.getConfigManager().getGlobalByteLimit();

        if (byteSize > maxBytes) {
            return CheckResult.drop("Oversized network buffer (" + byteSize + " > " + maxBytes + " bytes)", 5);
        }

        if (context.getRawPacket() instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) context.getRawPacket();
            if (buf.readableBytes() < 0) {
                return CheckResult.drop("Corrupted buffer readable bytes", 5);
            }
        }

        return CheckResult.allow();
    }
}

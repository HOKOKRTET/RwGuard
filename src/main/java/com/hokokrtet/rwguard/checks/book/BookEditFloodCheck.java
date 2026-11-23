package com.hokokrtet.rwguard.checks.book;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

public class BookEditFloodCheck extends AbstractCheck {
    public BookEditFloodCheck(RwGuard plugin) {
        super(plugin, "BookEditFlood", PacketCategory.BOOK);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getBookMaxEditsPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("B_EDIT") && !context.getPacketName().contains("EditBook")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("BOOK_EDIT");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getBookMaxEditsPps();

        if (count > max) {
            return CheckResult.drop("Book edit flood (" + count + "/" + max + " edits/sec)", 3);
        }

        return CheckResult.allow();
    }
}

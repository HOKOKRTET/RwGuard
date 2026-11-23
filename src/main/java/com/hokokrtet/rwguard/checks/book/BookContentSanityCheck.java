package com.hokokrtet.rwguard.checks.book;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;

import java.lang.reflect.Field;
import java.util.List;

public class BookContentSanityCheck extends AbstractCheck {
    public BookContentSanityCheck(RwGuard plugin) {
        super(plugin, "BookContentSanity", PacketCategory.BOOK);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("B_EDIT") && !context.getPacketName().contains("EditBook")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof List) {
                    List<?> pages = (List<?>) val;
                    if (pages.size() > plugin.getConfigManager().getBookMaxPages()) {
                        return CheckResult.drop("Book has too many pages (" + pages.size() + " > " + plugin.getConfigManager().getBookMaxPages() + ")", 4);
                    }
                    for (Object page : pages) {
                        if (page instanceof String) {
                            String pageStr = (String) page;
                            if (pageStr.length() > plugin.getConfigManager().getBookMaxPageLength()) {
                                return CheckResult.drop("Book page length exceeded (" + pageStr.length() + " chars)", 4);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return CheckResult.allow();
    }
}

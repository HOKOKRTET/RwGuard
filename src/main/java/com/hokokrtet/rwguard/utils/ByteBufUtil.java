package com.hokokrtet.rwguard.utils;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public final class ByteBufUtil {
    private ByteBufUtil() {}

    public static int readVarIntSafely(ByteBuf buf) {
        int numRead = 0;
        int result = 0;
        byte read;
        int readerIndex = buf.readerIndex();
        try {
            do {
                if (!buf.isReadable()) {
                    buf.readerIndex(readerIndex);
                    return -1;
                }
                read = buf.readByte();
                int value = (read & 0b01111111);
                result |= (value << (7 * numRead));
                numRead++;
                if (numRead > 5) {
                    buf.readerIndex(readerIndex);
                    return -1; // VarInt is too big
                }
            } while ((read & 0b10000000) != 0);
            return result;
        } finally {
            buf.readerIndex(readerIndex);
        }
    }

    public static String readStringSafely(ByteBuf buf, int maxLength) {
        int readerIndex = buf.readerIndex();
        try {
            int length = readVarIntSafely(buf);
            if (length < 0 || length > maxLength * 4 || length > buf.readableBytes()) {
                return null;
            }
            // Skip VarInt length bytes
            skipVarInt(buf);
            if (buf.readableBytes() < length) {
                return null;
            }
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            String str = new String(bytes, StandardCharsets.UTF_8);
            if (str.length() > maxLength) {
                return null;
            }
            return str;
        } catch (Exception e) {
            return null;
        } finally {
            buf.readerIndex(readerIndex);
        }
    }

    public static void skipVarInt(ByteBuf buf) {
        int numRead = 0;
        byte read;
        do {
            if (!buf.isReadable()) return;
            read = buf.readByte();
            numRead++;
            if (numRead > 5) return;
        } while ((read & 0b10000000) != 0);
    }
}

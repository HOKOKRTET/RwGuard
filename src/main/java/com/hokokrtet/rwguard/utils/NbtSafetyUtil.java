package com.hokokrtet.rwguard.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class NbtSafetyUtil {
    public static final int MAX_UNCOMPRESSED_BYTES = 524288; // 512 KB
    public static final int MAX_NBT_DEPTH = 32;
    public static final int MAX_NBT_TAGS = 10000;
    public static final int MAX_ARRAY_LENGTH = 16384;
    public static final int MAX_STRING_LENGTH = 32767;

    private NbtSafetyUtil() {}

    public static boolean isPotentiallyCompressed(ByteBuf buf) {
        if (buf.readableBytes() < 2) return false;
        int readerIndex = buf.readerIndex();
        byte b0 = buf.getByte(readerIndex);
        byte b1 = buf.getByte(readerIndex + 1);

        // GZIP: 0x1F, 0x8B
        if ((b0 == 0x1F || b0 == 31) && (b1 == (byte) 0x8B || b1 == -117)) {
            return true;
        }
        // ZLIB: 0x78 and valid check
        if (b0 == 0x78 && (b1 == 0x01 || b1 == 0x5E || b1 == (byte) 0x9C || b1 == (byte) 0xDA)) {
            return true;
        }
        return false;
    }

    public static boolean isSafeNbt(ByteBuf buf, int maxBytes, int maxDepth) {
        if (buf == null || buf.readableBytes() == 0) return true;

        ByteBuf slice = buf.slice();
        InputStream rawIn = new ByteBufInputStream(slice);
        InputStream targetIn = rawIn;

        try {
            if (isPotentiallyCompressed(slice)) {
                byte b0 = slice.getByte(slice.readerIndex());
                if (b0 == 0x1F || b0 == 31) {
                    targetIn = new GZIPInputStream(rawIn, 1024);
                } else {
                    targetIn = new InflaterInputStream(rawIn, new Inflater(), 1024);
                }
            }

            BoundedInputStream boundedIn = new BoundedInputStream(targetIn, maxBytes > 0 ? maxBytes : MAX_UNCOMPRESSED_BYTES);
            DataInputStream dataIn = new DataInputStream(boundedIn);

            return validateNbtStructure(dataIn, maxDepth > 0 ? maxDepth : MAX_NBT_DEPTH);
        } catch (IOException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean validateNbtStructure(DataInputStream in, int maxDepth) throws IOException {
        int tagType = in.read();
        if (tagType == -1) return true; // empty
        if (tagType == 0) return true; // TAG_End

        if (tagType != 10) {
            // Root NBT must be TAG_Compound (10) in standard Minecraft NBT
            return false;
        }

        // Read root compound name
        int nameLen = in.readUnsignedShort();
        if (nameLen > 256) return false;
        in.skipBytes(nameLen);

        Counter tagCounter = new Counter();
        return checkCompound(in, 1, maxDepth, tagCounter);
    }

    private static boolean checkTag(DataInputStream in, int type, int currentDepth, int maxDepth, Counter tagCounter) throws IOException {
        tagCounter.count++;
        if (tagCounter.count > MAX_NBT_TAGS) {
            throw new NbtBombException("Too many NBT tags: " + tagCounter.count);
        }
        if (currentDepth > maxDepth) {
            throw new NbtBombException("NBT depth exceeded: " + currentDepth);
        }

        switch (type) {
            case 0: // TAG_End
                return true;
            case 1: // TAG_Byte
                in.skipBytes(1);
                return true;
            case 2: // TAG_Short
                in.skipBytes(2);
                return true;
            case 3: // TAG_Int
                in.skipBytes(4);
                return true;
            case 4: // TAG_Long
                in.skipBytes(8);
                return true;
            case 5: // TAG_Float
                in.skipBytes(4);
                return true;
            case 6: // TAG_Double
                in.skipBytes(8);
                return true;
            case 7: { // TAG_Byte_Array
                int len = in.readInt();
                if (len < 0 || len > MAX_ARRAY_LENGTH) throw new NbtBombException("Illegal byte array length: " + len);
                in.skipBytes(len);
                return true;
            }
            case 8: { // TAG_String
                int len = in.readUnsignedShort();
                if (len > MAX_STRING_LENGTH) throw new NbtBombException("Illegal string length: " + len);
                in.skipBytes(len);
                return true;
            }
            case 9: { // TAG_List
                int elemType = in.read();
                int len = in.readInt();
                if (len < 0 || len > MAX_ARRAY_LENGTH) throw new NbtBombException("Illegal list length: " + len);
                if (len == 0 || elemType == 0) return true;
                for (int i = 0; i < len; i++) {
                    checkTag(in, elemType, currentDepth + 1, maxDepth, tagCounter);
                }
                return true;
            }
            case 10: // TAG_Compound
                return checkCompound(in, currentDepth + 1, maxDepth, tagCounter);
            case 11: { // TAG_Int_Array
                int len = in.readInt();
                if (len < 0 || len > MAX_ARRAY_LENGTH) throw new NbtBombException("Illegal int array length: " + len);
                in.skipBytes(len * 4);
                return true;
            }
            case 12: { // TAG_Long_Array
                int len = in.readInt();
                if (len < 0 || len > MAX_ARRAY_LENGTH) throw new NbtBombException("Illegal long array length: " + len);
                in.skipBytes(len * 8);
                return true;
            }
            default:
                throw new NbtBombException("Unknown NBT tag type: " + type);
        }
    }

    private static boolean checkCompound(DataInputStream in, int currentDepth, int maxDepth, Counter tagCounter) throws IOException {
        while (true) {
            int tagType = in.read();
            if (tagType == -1) break;
            if (tagType == 0) break; // TAG_End terminates compound

            int nameLen = in.readUnsignedShort();
            if (nameLen > 256) throw new NbtBombException("Oversized tag name length: " + nameLen);
            in.skipBytes(nameLen);

            checkTag(in, tagType, currentDepth, maxDepth, tagCounter);
        }
        return true;
    }

    private static class Counter {
        int count = 0;
    }

    public static class NbtBombException extends IOException {
        public NbtBombException(String message) {
            super(message);
        }
    }

    private static class BoundedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long bytesRead = 0;

        public BoundedInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                checkLimit();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                bytesRead += read;
                checkLimit();
            }
            return read;
        }

        private void checkLimit() throws NbtBombException {
            if (bytesRead > maxBytes) {
                throw new NbtBombException("Decompressed NBT size limit exceeded (" + bytesRead + " > " + maxBytes + " bytes)");
            }
        }
    }
}

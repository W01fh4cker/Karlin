package com.karlin.decompiler.core;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class ClassFileInfoReader {
    private static final int ACC_INTERFACE = 0x0200;
    private static final int ACC_ENUM = 0x4000;

    private ClassFileInfoReader() {
    }

    public static int readAccessFlags(byte[] classBytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IOException("Invalid class file");
            }
            in.readUnsignedShort();
            in.readUnsignedShort();
            int cpCount = in.readUnsignedShort();
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> {
                        int len = in.readUnsignedShort();
                        in.skipBytes(len);
                    }
                    case 3, 4 -> in.readInt();
                    case 5, 6 -> {
                        in.readLong();
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> in.readUnsignedShort();
                    case 9, 10, 11, 12, 18, 17 -> {
                        in.readUnsignedShort();
                        in.readUnsignedShort();
                    }
                    case 15 -> {
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                    }
                    default -> throw new IOException("Unknown constant pool tag: " + tag);
                }
            }
            return in.readUnsignedShort();
        }
    }

    public static boolean isInterface(int accessFlags) {
        return (accessFlags & ACC_INTERFACE) != 0;
    }

    public static boolean isEnum(int accessFlags) {
        return (accessFlags & ACC_ENUM) != 0;
    }
}

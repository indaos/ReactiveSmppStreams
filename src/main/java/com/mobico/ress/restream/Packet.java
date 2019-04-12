package com.mobico.ress.restream;

import java.nio.ByteBuffer;

public class Packet {

    private String value;

    public Packet() { }


    public static Packet newPacket(ByteBuffer buff) {
        int length = buff.get();
        if (length <= 0) return null;

        Packet packet = new Packet();
        byte[] arr = new byte[length];
        buff.get(arr, 0, length);
        packet.addValue(new String(arr));
        return packet;
    }

    public static Packet newPacket(byte[] buff) {
        if (buff == null || buff.length == 0) return null;

        Packet packet = new Packet();
        packet.addValue(new String(buff));
        return packet;
    }

    public Packet addValue(String str) {
        value = value == null ? str : value + "," + str;
        return this;
    }

    public String[] getValues() {
        return value.split(",");
    }

    public byte getLength() {
        return (byte) (value.length() + 1);
    }

    public ByteBuffer getBytes() {
        ByteBuffer buff = ByteBuffer.allocate(value.length() + 1);
        buff.put((byte) value.length());
        buff.put(value.getBytes());
        buff.flip();

        return buff;
    }

    public String toString() {
        return value;
    }

}

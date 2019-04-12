package com.mobico.ress.resmpp.pdu;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Address implements BaseOps {

    private byte ton = 0;
    private byte npi = 0;
    private String address = "";

    private Address() { }

    public static Address allocate() {
        Address addr = new Address();
        addr.setTon((byte) 0);
        addr.setNpi((byte) 0);
        addr.setAddress("");
        return addr;
    }

    public int getLength() {
        return getAddress().length() + 3;
    }

    public boolean setBytes(ByteBuffer buff) {
        try {
            byte ton = buff.get();
            byte npi = buff.get();
            String address = getString(buff);
            setTon(ton);
            setNpi(npi);
            setAddress(address);

            return true;
        } catch (BufferUnderflowException e) {
        }
        return false;
    }

    public ByteBuffer getBytes() {

        ByteBuffer buff = ByteBuffer.allocate(2 + address.length() + 1);
        buff.put(ton);
        buff.put(npi);
        putString(buff, address);

        return buff.flip();
    }

    public byte getTon() {
        return ton;
    }

    public void setTon(byte ton) {
        this.ton = ton;
    }

    public byte getNpi() {
        return npi;
    }

    public void setNpi(byte npi) {
        this.npi = npi;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null) {
            return;
        }
        this.address = address;
    }

    public String toString() {
        return new StringBuilder()
                .append("address: {")
                .append(ton).append(",")
                .append(npi).append(",")
                .append(address)
                .append("}")
                .toString();
    }
}

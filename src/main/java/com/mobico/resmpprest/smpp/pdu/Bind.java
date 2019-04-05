package com.mobico.resmpprest.smpp.pdu;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Bind extends BasePDU {

    private String systemId = "";
    private String password = "";
    private String sysType = "";
    private byte iVersion = 0x34;
    private byte ton = 0;
    private byte npi = 0;
    private String addressRange = "";

    public Bind(){
        super(BIND_TRANSCEIVER);
    }

    protected ByteBuffer getBody() {

        int length=systemId.length()+1
                +password.length()+1
                +sysType.length()+1
                +Byte.BYTES*3
                +addressRange.length()+1;

        ByteBuffer  buffer=ByteBuffer.allocate(length);
        putString(buffer,systemId)
        .putString(buffer,password)
        .putString(buffer,sysType);
        buffer.put(iVersion);
        buffer.put(ton);
        buffer.put(npi);
        putString(buffer,addressRange);

        buffer.flip();

        return buffer;
    }

    protected boolean setBody(ByteBuffer buff) {
        try {
            systemId = getString(buff);
            password = getString(buff);
            sysType = getString(buff);
            iVersion = buff.get();
            ton = buff.get();
            npi = buff.get();
            addressRange = getString(buff);
            return true;
        }catch(BufferUnderflowException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getSystemId() {
        return systemId;
    }

    public Bind setSystemId(String systemId) {
        this.systemId = systemId;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Bind setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getSysType() {
        return sysType;
    }

    public Bind setSysType(String sysType) {
        this.sysType = sysType;
        return this;
    }

    public byte getiVersion() {
        return iVersion;
    }

    public Bind setiVersion(byte iVersion) {
        this.iVersion = iVersion;
        return this;
    }

    public byte getTon() {
        return ton;
    }

    public Bind setTon(byte ton) {
        this.ton = ton;
        return this;
    }

    public byte getNpi() {
        return npi;
    }

    public Bind setNpi(byte npi) {
        this.npi = npi;
        return this;
    }

    public String toString() {
        return new StringBuilder()
                .append("bind: {")
                .append(super.toString())
                .append(systemId).append(",")
                .append(password).append(",")
                .append(sysType).append(",")
                .append(Integer.toHexString(ton)).append(",")
                .append(Integer.toHexString(npi)).append(",")
                .append(addressRange).append("}")
                .toString();
    }
}

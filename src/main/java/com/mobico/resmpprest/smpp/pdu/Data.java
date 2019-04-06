package com.mobico.resmpprest.smpp.pdu;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Data extends BasePDU {

    private String serviceType  = "";
    private Address sourceAddr  = Address.allocate();
    private Address destAddr  = Address.allocate();;
    private byte esmClass  = 0;
    private byte registeredDelivery  = 0;
    private byte dataCoding = 0;

    public Data(){
        super(DATA);
    }

    @Override
    public ByteBuffer getBody() {
        return pack(ByteBuffer.allocate(getObjectSize()));
    }

    @Override
    public boolean setBody(ByteBuffer buff) {
        return unpack(buff,this);
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Address getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(Address sourceAddr) {
        Objects.requireNonNull(this.sourceAddr = sourceAddr);
    }

    public Address getDestAddr() {
        return destAddr;
    }

    public void setDestAddr(Address destAddr) {
        Objects.requireNonNull(this.destAddr = destAddr);
    }

    public byte getEsmClass() {
        return esmClass;
    }

    public void setEsmClass(byte esmClass) {
        this.esmClass = esmClass;
    }

    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }

    public void setRegisteredDelivery(byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }

    public byte getDataCoding() {
        return dataCoding;
    }

    public void setDataCoding(byte dataCoding) {
        this.dataCoding = dataCoding;
    }

    public String toString() {
        return new StringBuilder()
                .append("data: {")
                .append(super.toString())
                .append(sourceAddr.toString()).append(",")
                .append(destAddr.toString()).append(",")
                .append(Integer.toHexString(esmClass&0xff)).append(",")
                .append(Integer.toHexString(registeredDelivery&0xff)).append(",")
                .append(Integer.toHexString(dataCoding&0xff)).append(",")
                .append("}")
                .toString();
    }
}

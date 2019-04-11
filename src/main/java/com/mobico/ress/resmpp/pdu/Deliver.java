package com.mobico.ress.resmpp.pdu;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class Deliver extends BasePDU {

    private String serviceType    = "";
    private Address sourceAddr    =  Address.allocate();
    private Address destAddr    = Address.allocate();
    private byte esmClass      = 0;
    private byte protocolId     = 0;
    private byte priorityFlag     = 0;
    private String scheduleDeliveryTime   = "";
    private String validityPeriod    = "";
    private byte registeredDelivery   = 0;
    private byte replaceIfPresentFlag  = 0;
    private byte dataCoding     = 0;
    private byte defaultMsgId  = 0;
    private byte length    = 0;
    private byte[] message       = null;

    public Deliver() {
        super(DELIVER);
    }

    @Override
    public ByteBuffer getBody() {
        return pack(ByteBuffer.allocate(getObjectSize()));
    }

    @Override
    public boolean setBody(ByteBuffer buff) {
        if (unpack(buff,this)) {
            message = new byte[length];
            try {
                buff.get(message);
                return true;
            }catch(BufferUnderflowException e){
            }
        }
        return false;
    }

    public Deliver message(String str) {
        setMessage(str.getBytes());
        return this;
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

    public byte getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(byte protocolId) {
        this.protocolId = protocolId;
    }

    public byte getPriorityFlag() {
        return priorityFlag;
    }

    public void setPriorityFlag(byte priorityFlag) {
        this.priorityFlag = priorityFlag;
    }

    public String getScheduleDeliveryTime() {
        return scheduleDeliveryTime;
    }

    public void setScheduleDeliveryTime(String scheduleDeliveryTime) {
        this.scheduleDeliveryTime = scheduleDeliveryTime;
    }

    public String getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(String validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }

    public void setRegisteredDelivery(byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }

    public byte getReplaceIfPresentFlag() {
        return replaceIfPresentFlag;
    }

    public void setReplaceIfPresentFlag(byte replaceIfPresentFlag) {
        this.replaceIfPresentFlag = replaceIfPresentFlag;
    }

    public byte getDataCoding() {
        return dataCoding;
    }

    public void setDataCoding(byte dataCoding) {
        this.dataCoding = dataCoding;
    }

    public byte getSmDefaultMsgId() {
        return defaultMsgId;
    }

    public void setSmDefaultMsgId(byte defaultMsgId) {
        this.defaultMsgId = defaultMsgId;
    }

    public byte getSmLength() {
        return length;
    }

    public void setSmLength(byte length) {
        this.length = length;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {

        Objects.requireNonNull(this.message = message);
        setSmLength((byte)message.length);
    }

    public String toString() {
        return new StringBuilder()
                .append("delivery: {")
                .append(super.toString())
                .append(serviceType).append(",")
                .append(sourceAddr.toString()).append(",")
                .append(destAddr.toString()).append(",")
                .append(Integer.toHexString(esmClass&0xff)).append(",")
                .append(Integer.toHexString(protocolId&0xff)).append(",")
                .append(Integer.toHexString(priorityFlag&0xff)).append(",")
                .append(scheduleDeliveryTime).append(",")
                .append(validityPeriod).append(",")
                .append(Integer.toHexString(registeredDelivery&0xff)).append(",")
                .append(Integer.toHexString(replaceIfPresentFlag&0xff)).append(",")
                .append(Integer.toHexString(dataCoding&0xff)).append(",")
                .append(Integer.toHexString(defaultMsgId&0xff)).append(",")
                .append(Integer.toHexString(length&0xff)).append(",")
                .append("}")
                .toString();
    }
}

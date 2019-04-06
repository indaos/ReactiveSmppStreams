package com.mobico.resmpprest.smpp.pdu;

import java.nio.ByteBuffer;

public class TLV {
    private short tag =0;
    private ByteBuffer value=null;

    public TLV(){

    }

    public void setTag(short tag) {
        this.tag=tag;
    }

    public void setValue(ByteBuffer value){
        this.value=value;
    }

    public short getTag() {
        return tag;
    }

    public ByteBuffer getValue() {
        return value;
    }

    public int getLength(){
        return (getValueLength()+4);
    }

    public int getValueLength() {
        return value!=null?value.capacity():0;
    }

    public ByteBuffer getEncoded(){
        short length=(short)getLength();
        ByteBuffer tlv_bytes=ByteBuffer.allocate(length);
        tlv_bytes.putShort(tag);
        tlv_bytes.putShort(length);
        tlv_bytes.put(getValue());
        tlv_bytes.flip();
        return tlv_bytes;
    }

    public String toString() {
        return "{"+tag+"}";
    }

}

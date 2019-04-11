package com.mobico.ress.resmpp.pdu;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class DataResp extends BasePDU {

    private String messageId = "";

    public DataResp() {
        super(DATA_RESP);
    }

    @Override
    public ByteBuffer getBody() {
        ByteBuffer buff = ByteBuffer.allocate(getObjectSize());

        putString(buff,getMessageId());
        return buff;
    }

    @Override
    public boolean setBody(ByteBuffer buff){
        try {
            String messageId = getString(buff);
            setMessageId(messageId);
            return true;
        }catch(BufferUnderflowException e){

        }
        return false;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        Objects.requireNonNull(this.messageId = messageId);
    }

    public String toString() {
        return new StringBuilder()
                .append("data_resp: {")
                .append(super.toString())
                .append(messageId).append("}")
                .toString();
    }
}

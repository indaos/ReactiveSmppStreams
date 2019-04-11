package com.mobico.ress.resmpp.pdu;

import java.nio.ByteBuffer;

public class SubmitResp extends BasePDU {

    private String messageId = "";

    public SubmitResp() {
        super(SUBMIT_RESP);
    }

    @Override
    public ByteBuffer getBody() {
        return pack(ByteBuffer.allocate(getObjectSize()));
    }

    @Override
    public boolean setBody(ByteBuffer buff) {
        return unpack(buff,this);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String toString() {
        return new StringBuilder()
                .append("submit_resp: {")
                .append(super.toString())
                .append(messageId).append("}")
                .toString();
    }
}

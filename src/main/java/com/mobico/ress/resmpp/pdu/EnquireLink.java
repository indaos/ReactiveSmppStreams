package com.mobico.ress.resmpp.pdu;

import java.nio.ByteBuffer;

public class EnquireLink extends BasePDU {

    public EnquireLink() {
        super(ENQUIRE_LINK);
    }

    public ByteBuffer getBody() {
        return null;
    }

    public String toString() {
        return new StringBuilder()
                .append("enquire_link: {")
                .append(super.toString()).append("}")
                .toString();
    }

}

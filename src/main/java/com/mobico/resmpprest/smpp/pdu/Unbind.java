package com.mobico.resmpprest.smpp.pdu;

public class Unbind extends BasePDU {
    public Unbind(){
        super(UNBIND);
    }

    public String toString() {
        return new StringBuilder()
                .append("unbind: {")
                .append(super.toString()).append("}")
                .toString();
    }
}

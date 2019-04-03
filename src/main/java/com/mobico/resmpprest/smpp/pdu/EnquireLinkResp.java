package com.mobico.resmpprest.smpp.pdu;

public class EnquireLinkResp extends BasePDU {

    public EnquireLinkResp() {
        super(ENQUIRE_LINK_RESP);
    }


    public String toString() {
        return new StringBuilder()
                .append("enquire_link_resp: {")
                .append(super.toString()).append("}")
                .toString();
    }
}

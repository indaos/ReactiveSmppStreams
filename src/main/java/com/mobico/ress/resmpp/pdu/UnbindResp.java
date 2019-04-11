package com.mobico.ress.resmpp.pdu;

public class UnbindResp extends BasePDU {
    public UnbindResp() {
        super(UNBIND_RESP);
    }

    public String toString() {
        return new StringBuilder()
                .append("unbind_resp: {")
                .append(super.toString()).append("}")
                .toString();
    }
}

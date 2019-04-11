package com.mobico.ress.resmpp.pdu;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class BindResp extends BasePDU {

    private String systemId = "";

    public BindResp() {
        super(BIND_TRANSCEIVER_RESP);
    }

    @Override
    public ByteBuffer getBody() {
        ByteBuffer buff=null;
        if (getCommandStatus()==0) {
            buff = ByteBuffer.allocate(getObjectSize());
            putString(buff,systemId);
        }
        buff.flip();
        return buff;
    }

    @Override
    public boolean setBody(ByteBuffer buff) {
        if (getCommandStatus() == 0) {
            try {
                systemId = getString(buff);
                return true;
            }catch (BufferUnderflowException e) {
              //  e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String toString() {
        return new StringBuilder()
                .append("bind_resp: {")
                .append(super.toString())
                .append(systemId).append("}")
                .toString();
    }

}

package com.mobico.resmpprest.smpp.pdu;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class BaseOps {
    public static final int SUBMIT_SM_RESP = 0x80000004;
    public static final int DELIVER_SM_RESP = 0x80000005;
    public static final int BIND_TRANSCEIVER_RESP = 0x80000009;
    public static final int UNBIND_RESP = 0x80000006;
    public static final int ENQUIRE_LINK_RESP = 0x80000015;
    public static final int DATA_SM_RESP = 0x80000103;

    public static final int GENERIC_NACK = 0x80000000;
    public static final int SUBMIT_SM = 0x00000004;
    public static final int DELIVER_SM = 0x00000005;
    public static final int UNBIND = 0x00000006;
    public static final int BIND_TRANSCEIVER = 0x00000009;
    public static final int ENQUIRE_LINK = 0x00000015;
    public static final int DATA_SM = 0x00000103;

    public BaseOps() { }

    public  BaseOps putString(ByteBuffer buff,String str)  throws BufferOverflowException {

         byte[] arr = str.getBytes();
         buff.put(arr);
         buff.put((byte)0);

        return this;
    }

    public  String getString(ByteBuffer buff) throws BufferUnderflowException {

        StringBuilder sb=new StringBuilder();
        byte b;
        while((b=buff.get())!=0) {
            sb.append((char)b);
        }
        return sb.toString();
    }
}

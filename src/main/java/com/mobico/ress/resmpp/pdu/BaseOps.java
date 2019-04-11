package com.mobico.ress.resmpp.pdu;

import java.lang.reflect.Field;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public interface BaseOps {
    public static final int SUBMIT_RESP = 0x80000004;
    public static final int DELIVER_RESP = 0x80000005;
    public static final int BIND_TRANSCEIVER_RESP = 0x80000009; //
    public static final int UNBIND_RESP = 0x80000006;
    public static final int ENQUIRE_LINK_RESP = 0x80000015;//
    public static final int DATA_RESP = 0x80000103;

    public static final int GENERIC_NACK = 0x80000000;
    public static final int SUBMIT = 0x00000004;
    public static final int DELIVER = 0x00000005;
    public static final int UNBIND = 0x00000006;
    public static final int BIND_TRANSCEIVER = 0x00000009;//
    public static final int ENQUIRE_LINK = 0x00000015;//
    public static final int DATA = 0x00000103;


    public boolean setBytes(ByteBuffer b);
    public ByteBuffer  getBytes();

    default ByteBuffer pack(ByteBuffer buff) {
        try {
            Objects.requireNonNull(buff);
            for (Field field : this.getClass().getDeclaredFields()) {

                field.setAccessible(true);

                Object e = field.get(this);

                if (e==null) continue;
                if (e instanceof Byte)  buff.put((byte)e);
                else if (e instanceof Short)  buff.putShort((short)e);
                else if (e instanceof Integer)  buff.putInt((int)e);
                else if (e instanceof String)   putString(buff,(String)e);
                else if (e instanceof Address)  buff.put(((Address) e).getBytes());
                else if (e instanceof byte[])   buff.put(ByteBuffer.wrap((byte[])e));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        buff.flip();

        return buff;
    }

    default boolean unpack(ByteBuffer buff,Object obj) {
        try {
            Objects.requireNonNull(buff);
            for (Field field : this.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object e = field.get(this);
                if (e==null) continue;
                if (e instanceof Byte)  field.setByte(obj,buff.get());
                else if (e instanceof Short)  field.setShort(obj,buff.getShort());
                else if (e instanceof Integer)  field.setInt(obj,buff.getInt());
                else if (e instanceof String)   field.set(obj,getString(buff));
                else if (e instanceof Address)  ((Address)e).setBytes(buff);
                else if (e instanceof byte[])   continue;
            }
        }catch(IllegalAccessException
                | BufferUnderflowException e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    default int getObjectSize() {
        ArrayList list=new ArrayList();
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(this);
                if (value != null) list.add(value);
            }
        }catch(IllegalAccessException e){
            e.printStackTrace();
        }
        return getSizeOfElements(list);
    }

    default int  getSizeOfElements(ArrayList list) {
        int summ=(Integer)list.stream().map(e->{
            if (e instanceof Byte)          return 1;
            else if (e instanceof Short)    return Short.BYTES;
            else if (e instanceof Integer)  return Integer.BYTES;
            else if (e instanceof String)   return ((String) e).length()+1;
            else if (e instanceof Address)  return ((Address) e).getLength();
            else if (e instanceof byte[])   return ((byte[]) e).length;
            else return 0;
        }).collect(Collectors.summingInt(Integer::intValue));
        return summ;
    }


    default  BaseOps putString(ByteBuffer buff,String str)  throws BufferOverflowException {
         byte[] arr = str.getBytes();
         buff.put(arr);
         buff.put((byte)0);

        return this;
    }

    default String getString(ByteBuffer buff) throws BufferUnderflowException {

        StringBuilder sb=new StringBuilder();
        byte b;
        while((b=buff.get())!=0) {
            sb.append((char)b);
        }
        return sb.toString();
    }
}

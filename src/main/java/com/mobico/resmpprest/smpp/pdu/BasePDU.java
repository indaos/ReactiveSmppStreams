package com.mobico.resmpprest.smpp.pdu;

import com.mobico.resmpprest.smpp.SmppClient;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;


public class BasePDU implements BaseOps{

    private final int HEADER_SIZE=16;

    private int commandLength = 0;
    private int commandId = 0;

    private int commandStatus = 0;
    private int sequenceNumber = 0;

    LinkedList<TLV>  optional=new LinkedList();

    private int start_position=0;

    protected BasePDU(int cid) {

        commandId = cid;
    }


    public static BasePDU newPDU(ByteBuffer buff) {

        int commandLength = 0;
        int commandId =0;
        int start_pos=buff.mark().position();
        try {
         commandLength = buff.getInt();
         commandId = buff.getInt();
        }catch(BufferUnderflowException e){
            buff.position(buff.limit());
            return null;
        }
        buff.position(start_pos);
        final BasePDU pdu=switch(commandId) {
            case ENQUIRE_LINK -> new EnquireLink();
            case ENQUIRE_LINK_RESP -> new EnquireLinkResp();
            case BIND_TRANSCEIVER -> new Bind();
            case BIND_TRANSCEIVER_RESP -> new BindResp();
            case SUBMIT -> new Submit();
            case SUBMIT_RESP -> new SubmitResp();
            case DATA -> new DataResp();
            case DATA_RESP -> new DataResp();
            case DELIVER -> new Deliver();
            case DELIVER_RESP -> new DataResp();
            case UNBIND -> new Unbind();
            case UNBIND_RESP -> new UnbindResp();
            case GENERIC_NACK -> new Generic_Nack();
            default -> null;
        };

        if (pdu!=null && pdu.setBytes(buff)) {
            return pdu;
        }
        else {
            buff.position(buff.limit());
            return null;
        }
    }

    protected ByteBuffer getBody() {
        return null;
    }


    protected boolean setBody(ByteBuffer buff) {
            return true;
    }

    public static BasePDU of(BasePDU pdu) {
        final BasePDU resp=switch(pdu.getCommandId()) {
            case ENQUIRE_LINK -> new EnquireLinkResp();
            case BIND_TRANSCEIVER -> new BindResp();
            case SUBMIT -> new SubmitResp();
            case DATA -> new DataResp();
            case DATA_RESP -> new DataResp();
            case DELIVER -> new DeliverResp();
            case UNBIND -> new UnbindResp();
            default -> null;
        };
        if (resp!=null) resp.setSequenceNumber(pdu.getSequenceNumber());
        return resp;
    }

    public BasePDU setseqId() {
        setSequenceNumber(SmppClient.getNextSeqNumber());
        return this;
    }


    public ByteBuffer getBytes() {

        ByteBuffer header=getHeader();
        ByteBuffer body=getBody();
        ByteBuffer tlv_opt=getOptional();

        int pdu_len=header.capacity();
        pdu_len+=body!=null?body.capacity():0;
        pdu_len+=tlv_opt!=null?tlv_opt.capacity():0;

        ByteBuffer pduBuff=ByteBuffer.allocate(pdu_len);
        pduBuff.put(header);
        Optional.ofNullable(body).ifPresent(b->pduBuff.put(b));
        Optional.ofNullable(tlv_opt).ifPresent(b->pduBuff.put(b));
        pduBuff.flip();

        return pduBuff;
    }

    public boolean setBytes(ByteBuffer buff) {

        start_position=buff.position();
        if (!setHeader(buff)) {

            return false;
        }
        if (!setBody(buff)) {
            return false;
        }
        while (buff.position()-start_position<commandLength) {
            if (!setOptional(buff)) {
                return false;
            }
        }
        return true;
    }

    public boolean setOptional(ByteBuffer buff) {
        try {
            short tag = buff.getShort();
            short length = buff.getShort();
            byte[] value = new byte[length];
            buff.get(value);
            ByteBuffer buffValue = ByteBuffer.allocate(length);
            buffValue.put(value);
            TLV tlv = new TLV();
            tlv.setTag(tag);
            tlv.setValue(buffValue);
            optional.add(tlv);

            return true;
        }catch(BufferOverflowException | BufferUnderflowException e){
            e.printStackTrace();
        }
        return false;
    }

    private boolean setHeader(ByteBuffer buff) {
        try {
            commandLength = buff.getInt();
            commandId = buff.getInt();
            commandStatus = buff.getInt();
            sequenceNumber = buff.getInt();
            return true;
        }catch(BufferUnderflowException e){
           // e.printStackTrace();
        }
        commandLength = 0;
        commandId = 0;
        commandStatus = 0;
        sequenceNumber = 0;
        return false;
    }

    private ByteBuffer getHeader() {
        ByteBuffer header=ByteBuffer.allocate(4*4);
        header.putInt(commandLength);
        header.putInt(commandId);
        header.putInt(commandStatus);
        header.putInt(sequenceNumber);
        header.flip();
        return header;
    }

    private ByteBuffer getOptional() {
        int all_len=0;
        for(TLV t:optional)
            all_len+=t.getLength();
        ByteBuffer opt_buff=ByteBuffer.allocate(all_len);
        for(TLV t:optional)
            opt_buff.put(t.getEncoded());
        opt_buff.flip();
        return opt_buff;
    }

    public int getCommandLength() {
        return commandLength;
    }

    public void setCommandLength(int commandLength) {
        this.commandLength = commandLength;
    }

    public int getCommandId() {
        return commandId;
    }

    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }

    public int getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(int commandStatus) {
        this.commandStatus = commandStatus;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setOptional(LinkedList<TLV> optional) {
        this.optional = optional;
    }

    public String toString() {
        return new StringBuilder()
                .append("pdu: {")
                .append(sequenceNumber).append(",")
                .append(Integer.toHexString(commandId)).append(",")
                .append(Integer.toHexString(commandStatus)).append("}")
                .toString();
    }

    public String tlvToString() {

        return optional.stream()
                .map(e->e.toString())
                .collect(Collectors.joining(","));
    }
}

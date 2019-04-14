package com.mobico.ress.util;

import com.mobico.ress.resmpp.pdu.BasePDU;
import com.mobico.ress.restream.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class AsyncProcessing<T> extends SocketClient {

    private ConcurrentLinkedQueue<PDU> output_queue = new ConcurrentLinkedQueue();
    private ConcurrentLinkedQueue<PDU> input_queue = new ConcurrentLinkedQueue();
    private Function<ByteBuffer,T> parser;

    public AsyncProcessing() { }

    public AsyncProcessing(Builder conf) {
        super(conf);
    }

    private ConcurrentLinkedQueue<PDU> getInputQueue(){
        return input_queue;
    }
    private ConcurrentLinkedQueue<PDU> getOutputQueue(){
        return output_queue;
    }

    protected void addOutputPDU(int channedlId,T pdu) {
        getOutputQueue().add(new PDU(channedlId,pdu));
        signalToWrite(channedlId);
    }

    protected T getNextInputPDU() {
        AsyncProcessing.PDU pdu=getInputQueue().poll();
        return pdu==null?null:(T)pdu.getPDU();
    }

    protected T getNextInputPDU(long timeout_ms) {
        T res;
        long start = System.currentTimeMillis();
        while ((res = getNextInputPDU()) == null) {
            try {
               if (timeout_ms>0)
                   Thread.sleep(5);
               else break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() >= start + timeout_ms)
                break;
        }
        return res;
    }


    class PDU<T> {
        private int channel;
        private T pdu;
        private boolean hasRemains=false;

        PDU(int c,T p) {
            channel=c; pdu=p;
        }

        T getPDU() {
            return pdu;
        }

        ByteBuffer getBytes(){
            if (pdu instanceof BasePDU)
                return ((BasePDU)pdu).getBytes();
            else if (pdu instanceof Packet) {
                return ((Packet)pdu).getBytes();
            } else
                return null;
        }
    }

    public void setParser(Function<ByteBuffer,T> parser) {
        this.parser=parser;
    }

    private boolean getPduWillBeNext(ChannelDesc desc) {
        T pdu;
        while((pdu= parser.apply(desc.getByteBuffer()))!=null) {
            getInputQueue().add(new PDU(desc.getIndex(),pdu));
            if (desc.getByteBuffer().position()==desc.getByteBuffer().limit()) {
                desc.getByteBuffer().clear();
                return false;
            } else {
                if (desc.getByteBuffer().position()>desc.getByteBuffer().capacity()*0.75)
                    desc.getByteBuffer().compact();
            }
        }
        return true;
    }

    public void multiChannelProcessing() {
        int remaining=0;
        while(!isShutdown) {
                try {

                    for (ChannelDesc channel : channels) {

                        loadNextPduFromPublisher();

                        if (channel.getSelector()==null) continue;

                        channel.getSelector().select(2);
                        Set<SelectionKey> selectedKeys = channel.getSelector().selectedKeys();
                        Iterator<SelectionKey> i = selectedKeys.iterator();

                        while (i.hasNext()) {
                            SelectionKey key = i.next();
                            if (!key.isValid())
                                continue;
                            ChannelDesc chdesc=(ChannelDesc)key.attachment();
                            if(key.isConnectable()) {
                                int p=0;
                            }else if (key.isReadable()) {
                                SocketChannel socket = (SocketChannel) key.channel();
                                int num_read=socket.read(chdesc.getByteBuffer());
                                if (num_read> 0) {
                                    chdesc.getByteBuffer().limit(chdesc.getByteBuffer().position());
                                    if (chdesc.hasRemains()) {
                                        chdesc.getByteBuffer().reset();
                                        chdesc.setHasRemains(false);
                                    } else {
                                        chdesc.getByteBuffer().flip();
                                    }
                                    if (getPduWillBeNext(chdesc)) {
                                        chdesc.setHasRemains(true);
                                    }
                                    chdesc.getByteBuffer().limit(DEFAULT_READ_BUFFER_SIZE);
                                } else if (num_read<0) {
                                    close(chdesc.getIndex());
                                    continue;
                                }
                            } else if (key.isWritable()) {
                                SocketChannel socket = (SocketChannel) key.channel();
                                if (chdesc.getWriteBuffer() == null
                                        || !chdesc.getWriteBuffer().hasRemaining()) {
                                    PDU pdu = getOutputQueue().poll();
                                    if (pdu != null) {
                                        chdesc.setWriteBuff(pdu.getBytes());
                                        remaining++;
                                    }
                                }
                                if (chdesc.getWriteBuffer()!=null){
                                    if (chdesc.getWriteBuffer().hasRemaining()) {
                                        int res=socket.write(chdesc.getWriteBuffer());
                                        if (res<0) {
                                            close(chdesc.getIndex());
                                            continue;
                                        }
                                        if (!chdesc.getWriteBuffer().hasRemaining()
                                                && remaining>0) {
                                            remaining--;
                                        }
                                    } else if (remaining>0) remaining--;
                                    if (getOutputQueue().size()==0 && remaining==0)
                                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                }
                            }

                            i.remove();
                        }

                    }

                }catch(IOException | NullPointerException e) { }
            }
        isShutdown=false;
        unsubcribeAll();
    }

    protected void loadNextPduFromPublisher() { }
    protected void unsubcribeAll() { }

}

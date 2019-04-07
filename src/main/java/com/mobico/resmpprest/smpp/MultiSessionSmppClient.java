package com.mobico.resmpprest.smpp;

import com.mobico.resmpprest.smpp.pdu.BasePDU;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiSessionSmppClient {
    private final int DEFAULT_READ_BUFFER_SIZE = 4096;

    private static ArrayList<BuilderImpl> builders=new ArrayList();
    private static ArrayList<ChannelDesc> channels=new ArrayList();
    private ConcurrentLinkedQueue<PDU> output_queue = new ConcurrentLinkedQueue();
    private ConcurrentLinkedQueue<PDU> input_queue = new ConcurrentLinkedQueue();
    private boolean isClosingState=false;
    private static MultiSessionSmppClient client;
    private int hasRemaining=0;

    public class PDU {

        public int channel;
        public BasePDU pdu;
        private boolean hasRemains=false;

        PDU(int c,BasePDU p) {
            channel=c; pdu=p;
        }
    }

    class ChannelDesc{

        int index;
        Selector selector;
        SocketChannel socket;
        ByteBuffer read_buff;
        boolean hasRemains=false;
        ByteBuffer write_buff;

        ChannelDesc(int i,SocketChannel s){
            index=i;
            socket=s;
            read_buff=ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        }
        void clear() {
            index=-1;
            selector=null;
            socket=null;
            read_buff.clear();
            write_buff.clear();
            hasRemains=false;
        }
    }

    private MultiSessionSmppClient () {

    }

    public static Builder newBuilder() {
        if (client==null) client=new MultiSessionSmppClient();
        BuilderImpl builder=new BuilderImpl(client,builders.size());
        builders.add(builder);
        return  builder;
    }

    public boolean connect(int index) {
        try {
            if (index<0) return false;
            if (index<channels.size()
                    && channels.get(index).socket!=null)
                return false;
            BuilderImpl conf=builders.get(index);
            if (conf==null) return false;
            SocketChannel socket = SocketChannel.open();

            if (socket.connect(new InetSocketAddress(conf.host, conf.port))) {
                SocketAddress raddr = socket.getRemoteAddress();
                if (raddr == null)
                    return false;
                socket.configureBlocking(false);
                boolean result = socket.isOpen();
                if (result) {
                    Selector selector = Selector.open();
                    ChannelDesc desc;
                    if (index<channels.size()) {
                        desc=channels.get(index);
                        desc.selector=selector;
                        desc.socket=socket;
                        desc.index=index;
                    } else {
                        desc = new ChannelDesc(channels.size(), socket);
                        desc.selector=selector;
                        channels.add(desc);
                    }
                    socket.register(selector, SelectionKey.OP_READ,desc);
                }
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean getPduWillBeMore(ChannelDesc desc) {
        BasePDU pdu;
        while((pdu=BasePDU.newPDU(desc.read_buff))!=null) {
            input_queue.add(new PDU(desc.index,pdu));
            if (desc.read_buff.position()==desc.read_buff.limit()) {
                desc.read_buff.clear();
                return false;
            } else {
                if (desc.read_buff.position()>desc.read_buff.capacity()*0.75)
                    desc.read_buff.compact();
            }
        }
        return true;
    }

    public void processing() {

        ExecutorService read_executor = Executors.newSingleThreadExecutor();
        read_executor.execute(() -> {
                while(!isClosingState) {
                    try {
                        for (ChannelDesc channel : channels) {
                            if (channel.selector==null)
                                continue;
                            channel.selector.select(10);
                            Set<SelectionKey> selectedKeys = channel.selector.selectedKeys();
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
                                    int num_read=socket.read(chdesc.read_buff);
                                    if (num_read> 0) {
                                        chdesc.read_buff.limit(chdesc.read_buff.position());
                                        if (chdesc.hasRemains) {
                                            chdesc.read_buff.reset();
                                            chdesc.hasRemains=false;
                                        } else {
                                            chdesc.read_buff.flip();
                                        }
                                        if (getPduWillBeMore(chdesc)) {
                                            chdesc.hasRemains = true;
                                        }
                                        chdesc.read_buff.limit(DEFAULT_READ_BUFFER_SIZE);
                                    } else if (num_read<0) {
                                        close(chdesc.index);
                                        continue;
                                    }
                                } else if (key.isWritable()) {
                                    SocketChannel socket = (SocketChannel) key.channel();
                                    if (chdesc.write_buff == null
                                            || !chdesc.write_buff.hasRemaining()) {
                                        PDU pdu = output_queue.poll();
                                        if (pdu != null) {
                                            chdesc.write_buff = pdu.pdu.getBytes();
                                            hasRemaining++;
                                        }
                                    }
                                    if (chdesc.write_buff!=null){
                                        if (chdesc.write_buff.hasRemaining()) {
                                            int res=socket.write(chdesc.write_buff);
                                            if (res<0) {
                                                close(chdesc.index);
                                                continue;
                                            }
                                            if (!chdesc.write_buff.hasRemaining()
                                            && hasRemaining>0) {
                                                hasRemaining--;
                                            }
                                            if (output_queue.size()==0 && hasRemaining==0)
                                             key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                        }
                                    }
                                }
                                i.remove();
                            }
                        }
                    }catch(IOException | NullPointerException e) {}
                }
        });
    }

    private SelectionKey getKeyByIndex(int index) {
        try {
            ChannelDesc desc = channels.get(index);
            SelectionKey key = desc.socket.keyFor(desc.selector);
            return key;
        }catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    public boolean send(BasePDU pdu,int channel) {

        SelectionKey key=getKeyByIndex(channel);
        if (key==null) return false;
        output_queue.add(new PDU(channel,pdu));
        if ((key.interestOps()
                & SelectionKey.OP_WRITE)==0)
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        return true;
    }

    public MultiSessionSmppClient.PDU getNextPdu() {
        return input_queue.poll();
    }

    public MultiSessionSmppClient.PDU getNextPdu(int timeout_ms) {
        PDU res;
        long start = System.currentTimeMillis();
        while ((res = input_queue.poll()) == null) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() >= start + timeout_ms)
                break;
        }
        return res;
    }

    public void close(int channel) {
        try {
            channels.get(channel).socket.close();
            channels.get(channel).clear();
        }catch(IOException e) {}
    }

    public void closeAll() {
        isClosingState=true;
        for (ChannelDesc channel : channels) {
            try {
                channel.socket.finishConnect();
            }catch(IOException e) {}
        }
        channels.clear();
    }


}

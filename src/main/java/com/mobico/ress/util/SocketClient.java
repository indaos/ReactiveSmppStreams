package com.mobico.ress.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketClient {
    public final long DEFAULT_CONNECTION_TIMEOUT=60000;
    public final int DEFAULT_READ_BUFFER_SIZE = 4096;
    private Builder conf=null;
    private AsynchronousSocketChannel channel;
    private List<Builder> builders=Collections.synchronizedList(new ArrayList());
    protected List<ChannelDesc> channels= Collections.synchronizedList(new ArrayList());
    protected boolean isShutdown =false;

    class ChannelDesc{

        private int index;
        private Selector selector;
        private SocketChannel socket;
        private ByteBuffer readBuff;
        private boolean hasRemains=false;
        private ByteBuffer writeBuff;

        ChannelDesc(int i,SocketChannel s){
            index=i;
            socket=s;
            readBuff=ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        }
        void clear() {
            index=-1;
            selector=null;
            socket=null;
            readBuff.clear();
            writeBuff.clear();
            hasRemains=false;
        }

        void setHasRemains(boolean b){
            hasRemains=b;
        }

        boolean hasRemains(){
            return hasRemains;
        }

        Selector getSelector() {
            return selector;
        }

        int getIndex() {
            return index;
        }

        void setWriteBuff(ByteBuffer buff) {
            writeBuff=buff;
        }

        ByteBuffer getWriteBuffer() {
            return writeBuff;
        }

        ByteBuffer getByteBuffer() {
            return readBuff;
        }
    }

    public SocketClient() { }

    public SocketClient(Builder conf) {
        this.conf=conf;
    }

    public  void addBuilder(Builder builder) {
        builders.add(builder);
    }

    public boolean connect(int index) {
        try {
            if (index<0) return false;
            if (index<channels.size()
                    && channels.get(index).socket!=null)
                return false;
            Builder conf=builders.get(index);
            if (conf==null) return false;
            SocketChannel socket = SocketChannel.open();

            if (socket.connect(new InetSocketAddress(conf.getHost(), conf.getPort()))) {
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
                        channels.add(desc); // ?? concurent modification
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

    public int getNumChannels(){
        return channels.size();
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

    public boolean signalToWrite(int channel) {

        SelectionKey key=getKeyByIndex(channel);
        if (key==null) return false;
        if ((key.interestOps()
                & SelectionKey.OP_WRITE)==0)
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        return true;
    }

    private void reset() {
        channel=null;
        channels.clear();
        builders.clear();
    }

    public void close(int index) {
        try {
            channels.get(index).socket.close();
            channels.get(index).clear();
            int num_open=0;
            for (ChannelDesc channel : channels)
                if (channel.index!=-1) num_open++;
            if (num_open==0) {
                isShutdown=true;
                reset();
            }
        }catch(IOException e) {}
    }

    public void closeAll() {
        isShutdown =true;
        for (ChannelDesc channel : channels) {
            try {
                channel.socket.close();
            }catch(IOException e) {}
        }
        reset();
    }
}

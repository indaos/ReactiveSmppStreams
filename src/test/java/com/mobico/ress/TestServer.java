package com.mobico.ress;

import com.mobico.ress.resmpp.pdu.BasePDU;
import com.mobico.ress.restream.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class TestServer<T> {
    private final int DEFAULT_READ_BUFFER_SIZE = 4096;

    private int port = 8080;
    private ServerSocketChannel server = null;
    private ConcurrentLinkedQueue<T> input_queue = new ConcurrentLinkedQueue();
    private ConcurrentLinkedQueue<PDU> output_queue = new ConcurrentLinkedQueue();
    protected List<ChannelDesc> channels= Collections.synchronizedList(new ArrayList());
    private Function<T,byte[]> handler;
    private Function<ByteBuffer,T> parser;
    private AtomicBoolean writeFlag=new AtomicBoolean(false);

    class ChannelDesc{

        private Selector selector;
        private SocketChannel socket;
        private ServerSocketChannel serverSocket;
        private ByteBuffer readBuff;
        private boolean hasRemains=false;
        private ByteBuffer writeBuff;

        ChannelDesc(Selector sel,ServerSocketChannel s){
            selector=sel;
            serverSocket=s;
            readBuff=ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        }
        ChannelDesc(Selector sel,SocketChannel s){
            selector=sel;
            socket=s;
            readBuff=ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        }
        void clear() {
            selector=null;
            socket=null;
            readBuff.clear();
            writeBuff.clear();
            hasRemains=false;
        }

        ServerSocketChannel getServerSocket() { return serverSocket; }

        SocketChannel getSocket() { return socket; }

        void setHasRemains(boolean b){
            hasRemains=b;
        }

        boolean hasRemains(){
            return hasRemains;
        }

        Selector getSelector() {
            return selector;
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

    class PDU<T> {
        private T pdu;
        private boolean hasRemains=false;

        PDU(T p) {
             pdu=p;
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


    public TestServer() { }

    TestServer withPort(int port) {
        this.port = port;
        return this;
    }

    TestServer start() throws IOException {
        Selector selector = Selector.open();
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        ChannelDesc desc = new ChannelDesc(selector,server);
        channels.add(desc);

        return this;
    }

    public boolean isOpen(){
        return server.isOpen();
    }

    public int getPort() {
        return port;
    }

    public TestServer setHandler(Function<T,byte[]> hadnler) {
        this.handler=hadnler;
        return this;
    }
    public TestServer setPackerParser(Function<ByteBuffer,T> parser) {
        this.parser=parser;
        return this;
    }

    private boolean getPduWillBeMore(ChannelDesc desc) {
        T pdu;
        while((pdu=parser.apply(desc.getByteBuffer()))!=null) {
            if (handler!=null) {
                byte[] response = handler.apply(pdu);
                if (response != null) {
                    try {
                        desc.socket.write(ByteBuffer.wrap(response));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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

    public void run() {
        ExecutorService main_loop = Executors.newSingleThreadExecutor();
        main_loop.execute(() -> {
            int remaining=0;
            while (server.isOpen()) {
                try {
                    for (int i_ch=0;i_ch<channels.size();i_ch++) {

                        ChannelDesc channel=channels.get(i_ch);

                        if (channel.getSelector() == null)
                            continue;
                        channel.selector.select(1);
                        Set<SelectionKey> selectedKeys = channel.selector.selectedKeys();
                        Iterator<SelectionKey> i = selectedKeys.iterator();
                        while (i.hasNext()) {
                            SelectionKey key = i.next();
                            if (!key.isValid()) {
                                continue;
                            }
                            ChannelDesc chdesc = (ChannelDesc) key.attachment();
                            if (key.isAcceptable()) {
                                try {
                                    SocketChannel ch = server.accept();
                                    ch.configureBlocking(false);
                                    Selector selector_accept = Selector.open();
                                    ChannelDesc desc = new ChannelDesc(selector_accept,ch);
                                    desc.selector = selector_accept;
                                    ch.register(selector_accept, SelectionKey.OP_READ, desc);
                                    channels.add(desc);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else if (key.isReadable()) {
                                ChannelDesc desc = (ChannelDesc) key.attachment();
                                SocketChannel socket = (SocketChannel) key.channel();
                                int num_read = socket.read(desc.getByteBuffer());

                                if (num_read > 0) {
                                    desc.getByteBuffer().limit(desc.getByteBuffer().position());
                                    if (desc.hasRemains) {
                                        desc.getByteBuffer().reset();
                                        desc.hasRemains = false;
                                    } else {
                                        desc.getByteBuffer().flip();
                                    }
                                    if (getPduWillBeMore(desc)) {
                                        desc.hasRemains = true;
                                    }
                                    desc.getByteBuffer().limit(DEFAULT_READ_BUFFER_SIZE);
                                }
                            } else if (key.isWritable()) {
                                SocketChannel socket = (SocketChannel) key.channel();
                                if (chdesc.getWriteBuffer() == null
                                        || !chdesc.getWriteBuffer().hasRemaining()) {
                                    PDU pdu = output_queue.poll();
                                    if (pdu != null) {
                                        chdesc.setWriteBuff(pdu.getBytes());
                                        remaining++;
                                    }
                                }
                                if (chdesc.getWriteBuffer() != null) {
                                    if (chdesc.getWriteBuffer().hasRemaining()) {
                                        int res = socket.write(chdesc.getWriteBuffer());
                                        if (res < 0) {
                                            chdesc.getSocket().close();
                                            continue;
                                        }
                                        if (!chdesc.getWriteBuffer().hasRemaining()
                                                && remaining > 0) {
                                            remaining--;
                                        }
                                    } else if (remaining>0) remaining--;

                                    if (output_queue.size() == 0 && remaining == 0) {
                                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                        if (writeFlag.get()) writeFlag.set(false);
                                    }
                                }
                            }
                            i.remove();
                        }
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    public void send(T pdu) {
        output_queue.add(new PDU(pdu));
        if (!writeFlag.get()) {
            writeFlag.set(true);
            for(ChannelDesc ch:channels) {
                if (ch.getSocket()!=null) {
                    SelectionKey key = ch.getSocket().keyFor(ch.getSelector());
                    if ((key.interestOps()
                            & SelectionKey.OP_WRITE) == 0)
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
            }
        }
    }

    public void close() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.mobico.resmpprest;

import com.mobico.resmpprest.smpp.pdu.BasePDU;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class TestSmppServer {
    private final int DEFAULT_READ_BUFFER_SIZE = 4096;

    private int port = 8080;
    private ServerSocketChannel server = null;
    private Selector selector = null;
    private ConcurrentLinkedQueue<BasePDU> input_queue = new ConcurrentLinkedQueue();
    private Function<BasePDU,byte[]> handler;

    class ChannelDesc{
        SocketChannel socket;
        ByteBuffer buff;
        boolean hasRemains=false;
        ChannelDesc(SocketChannel s){
            socket=s;
            buff=ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        }
    }

    public TestSmppServer() {

    }

    TestSmppServer withPort(int port) {
        this.port = port;
        return this;
    }

    TestSmppServer start() throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        return this;
    }

    public boolean isOpen(){
        return server.isOpen();
    }

    public int getPort() {
        return port;
    }


    private boolean getPduWillBeMore(ChannelDesc desc) {
        BasePDU pdu;
        while((pdu=BasePDU.newPDU(desc.buff))!=null) {
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
            if (desc.buff.position()==desc.buff.limit()) {
                desc.buff.clear();
                return false;
            } else {
                if (desc.buff.position()>desc.buff.capacity()*0.75)
                    desc.buff.compact();
            }
         }
        return true;
    }

    public void run() {
        ExecutorService main_loop = Executors.newSingleThreadExecutor();
        main_loop.execute(() -> {
            while (server.isOpen()) {
                try {
                    selector.select(1000);
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();
                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isAcceptable()) {
                            SocketChannel channel = server.accept();
                            channel.configureBlocking(false);
                            channel.register(selector, SelectionKey.OP_READ,new ChannelDesc(channel));

                        } else
                        if (key.isReadable()) {
                            ChannelDesc desc = (ChannelDesc)key.attachment();
                            SocketChannel channel = (SocketChannel) key.channel();
                            int num_read=channel.read(desc.buff);

                            if (num_read> 0) {
                                desc.buff.limit(desc.buff.position());
                                if (desc.hasRemains) {
                                    desc.buff.reset();
                                    desc.hasRemains=false;
                                } else {
                                    desc.buff.flip();
                                }
                                if (getPduWillBeMore(desc)) {
                                    desc.hasRemains = true;
                                }
                                desc.buff.limit(DEFAULT_READ_BUFFER_SIZE);
                            }

                        }
                        i.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    public void setHandler(Function<BasePDU,byte[]> hadnler) {
        this.handler=hadnler;
    }

    public void close() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

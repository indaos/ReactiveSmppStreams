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


    private boolean getPduWillBeMore(ByteBuffer buffer,SocketChannel channel) {
        BasePDU pdu;
        while((pdu=BasePDU.newPDU(buffer))!=null) {
            if (handler!=null) {
                byte[] response = handler.apply(pdu);
                if (response != null) {
                    try {
                        channel.write(ByteBuffer.wrap(response));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (buffer.position()==buffer.limit()) {
                buffer.clear();
                return false;
            } else {
                if (buffer.position()>buffer.capacity()*0.75)
                    buffer.compact();
            }
         }
        return true;
    }

    public void run() {
        final ByteBuffer buff = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        ExecutorService main_loop = Executors.newSingleThreadExecutor();
        main_loop.execute(() -> {
            boolean hasRemains=false;
            while (server.isOpen()) {
                try {
                    selector.select();
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
                            channel.register(selector, SelectionKey.OP_READ);

                        } else
                        if (key.isReadable()) {

                            SocketChannel channel = (SocketChannel) key.channel();
                            int num_read=channel.read(buff);

                            if (num_read> 0) {
                                buff.limit(buff.position());
                                if (hasRemains) {
                                    buff.reset();
                                    hasRemains=false;
                                } else {
                                    buff.flip();
                                }
                                if (getPduWillBeMore(buff,channel)) {
                                    hasRemains = true;
                                }
                                buff.limit(DEFAULT_READ_BUFFER_SIZE);
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

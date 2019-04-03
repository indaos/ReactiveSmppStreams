package com.mobico.resmpprest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestSmppServer {

    private final int MAX_PENDING_BUFFER_SIZE = 64 * 1024;
    private int port = 8080;
    private ServerSocketChannel server = null;
    private Selector selector = null;

    public TestSmppServer() {

    }

    TestSmppServer withPort(int port) {
        this.port = port;
        return this;
    }

    TestSmppServer start() throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", 5454));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        return this;
    }

    private void try_pdu(ByteBuffer buffer) {

    }

    public void run() {
        final ByteBuffer pending = ByteBuffer.allocate(MAX_PENDING_BUFFER_SIZE);
        final ByteBuffer buff = ByteBuffer.allocate(512);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            while (server.isOpen()) {
                try {
                    if (pending.position() > 0) {
                        try_pdu(pending);
                    }
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();
                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        if (key.isAcceptable()) {
                            SocketChannel channel = server.accept();
                            channel.register(selector, SelectionKey.OP_READ);
                            channel.configureBlocking(false);
                        }
                        if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            channel.read(buff);
                            try_pdu(buff);
                            if (buff.hasRemaining()) {
                                byte[] bytes = new byte[buff.remaining()];
                                buff.get(bytes, 0, bytes.length);
                                pending.put(bytes);
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

    public void close() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

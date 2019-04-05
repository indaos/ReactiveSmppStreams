package com.mobico.resmpprest;

import com.mobico.resmpprest.smpp.SmppClient;
import com.mobico.resmpprest.smpp.pdu.BaseOps;
import com.mobico.resmpprest.smpp.pdu.BasePDU;
import com.mobico.resmpprest.smpp.pdu.BindResp;

import java.io.ByteArrayOutputStream;
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

public class TestSmppServer {

    private final int MAX_PENDING_BUFFER_SIZE = 64 * 1024;
    private int port = 8080;
    private ServerSocketChannel server = null;
    private Selector selector = null;
    private ConcurrentLinkedQueue<BasePDU> input_queue = new ConcurrentLinkedQueue();


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


    private void sendResponse (BasePDU pdu,SocketChannel channel,ByteArrayOutputStream bo){
        if (pdu!=null) {
            BasePDU resp=switch(pdu.getCommandId()){
                case BaseOps.BIND_TRANSCEIVER -> new BindResp();
                default->null;
            };
            if (resp!=null) {
                try {
                    ByteBuffer b = resp.getBytes();
                    /*
                    int m = b.capacity() / 2;
                    byte[] a1 = new byte[m];
                    byte[] a2 = new byte[b.capacity() - m];
                    b.get(a1, 0, m);
                    b.get(a2, 0, a2.length);
                    channel.write(ByteBuffer.wrap(a1));
                  //  SmppClient.LOG.info("*** 1write " + a1.length + " bytes");
                    try {
                        Thread.sleep(new Random().nextInt(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    channel.write(ByteBuffer.wrap(a2));
                   // SmppClient.LOG.info("*** 2write " + a2.length + " bytes");

                    */
                   // channel.write(b);
                    bo.write(b.array());
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }


    private boolean getPduWillBeMore(ByteBuffer buffer,SocketChannel channel) {
        BasePDU pdu;
        while((pdu=BasePDU.newPDU(buffer))!=null) {
            input_queue.add(pdu);
            if(input_queue.size()%10==0) {
                ByteArrayOutputStream bo=new ByteArrayOutputStream();
                for(int i=0;i<10;i++) {
                    BasePDU pdu1=input_queue.poll();
                    if (pdu1==null) continue;
                    SmppClient.LOG.info("S<received " + pdu.toString() + "," + buffer.position() + "," + buffer.limit());
                    sendResponse(pdu1, channel,bo);
                }
                try {
                    channel.write(ByteBuffer.wrap(bo.toByteArray()));
                }catch(IOException e){
                    e.printStackTrace();
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
        final ByteBuffer buff = ByteBuffer.allocate(512);
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
                                if (hasRemains) {
                                    buff.reset();
                                    hasRemains=false;
                                } else {
                                    buff.flip();
                                }
                                if (getPduWillBeMore(buff,channel)) {
                                    hasRemains = true;
                                }
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

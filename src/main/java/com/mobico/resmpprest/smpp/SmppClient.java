package com.mobico.resmpprest.smpp;

import com.mobico.resmpprest.smpp.pdu.BasePDU;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.*;

public class SmppClient {
    private final long DEFAULT_READ_TIMEOUT = 60000L;
    private final int DEFAULT_READ_BUFFER_SIZE = 4096;
    private final int MAX_PENDING_BUFFER_SIZE = 64 * 1024;

    private BuilderImpl conf;
    private AsynchronousSocketChannel channel;
    private ConcurrentLinkedQueue<BasePDU> output_queue = new ConcurrentLinkedQueue();
    private ConcurrentLinkedQueue<BasePDU> input_queue = new ConcurrentLinkedQueue();

    private SmppClient() {
    }

    protected SmppClient(BuilderImpl conf) {
        this.conf = conf;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public boolean connect() {
        try {
            channel = AsynchronousSocketChannel.open();
            channel.connect(new InetSocketAddress(conf.host, conf.port)).get(conf.time, TimeUnit.MILLISECONDS);
            SocketAddress raddr = channel.getRemoteAddress();
            if (raddr == null)
                return false;
            return channel.isOpen();
        } catch (IOException
                | InterruptedException
                | ExecutionException
                | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tryPDU(ByteBuffer buffer) {
        BasePDU pdu;
        while((pdu=BasePDU.newPDU(buffer))!=null)
          input_queue.add(pdu);
    }

    public void processing() {
        final ByteBuffer pending = ByteBuffer.allocate(MAX_PENDING_BUFFER_SIZE);
        final ByteBuffer buff = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
        ExecutorService read_executor = Executors.newSingleThreadExecutor();
        read_executor.execute(() -> {
            int num_read=0;
            while(num_read>=0) {
                Future<Integer> result = channel.read(buff);
                try {
                    num_read = result.get(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS);
                }catch(InterruptedException
                        | ExecutionException
                        | TimeoutException e) {
                    e.printStackTrace();
                }
                if (num_read == -1) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (num_read>0) {
                    buff.limit(num_read);
                    if (pending.position() > 0) {
                        pending.flip();
                        tryPDU(pending);
                        pending.limit(MAX_PENDING_BUFFER_SIZE);
                    }
                    buff.flip();
                    tryPDU(buff);
                    if (buff.hasRemaining()) {
                        byte[] bytes = new byte[buff.remaining()];
                        buff.get(bytes, 0, bytes.length);
                        pending.put(bytes);
                        pending.limit(pending.position());
                    }
                    buff.limit(DEFAULT_READ_BUFFER_SIZE);
                }
            }
        });

        ScheduledExecutorService write_executor = Executors.newScheduledThreadPool(1);

        Runnable write_task = () -> {
            if (!channel.isOpen()) {
                return;
            }
            BasePDU pdu = output_queue.poll();
            if (pdu != null) {
                ByteBuffer pduBytes = pdu.getBytes();
                while(pduBytes.hasRemaining()) {
                    Future<Integer> result = channel.write(pduBytes);
                    int num_written=0;
                    try {
                      num_written = result.get(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS);
                    }catch(InterruptedException
                            | ExecutionException
                            | TimeoutException e) {
                        e.printStackTrace();
                    }
                    if (num_written == -1) {
                        try {
                            channel.close();
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                }
            }
        };

        int period = 1;
        write_executor.scheduleWithFixedDelay(write_task, 0, 1000 / conf.mps, TimeUnit.MILLISECONDS);

    }

    public void send(BasePDU pdu) {
        output_queue.add(pdu);
    }

    public BasePDU getNextPdu() {
        return input_queue.poll();
    }

    public BasePDU getNextPdu(int timeout_ms) {
        BasePDU res;
        long start = System.currentTimeMillis();
        while ((res = input_queue.poll()) == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() >= start + timeout_ms)
                break;
        }
        return res;
    }

    public interface Builder {
         Builder bindIp(String ip);

         Builder host(String host);

         Builder port(int port);

         Builder username(String name);

         Builder password(String pswd);

         Builder systype(String type);

         Builder timeout(int timeout);

         Builder maxmps(int mps);

         SmppClient newClient();
    }

}

package com.mobico.resmpprest.smpp;

import com.mobico.resmpprest.smpp.pdu.BasePDU;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadPendingException;
import java.util.concurrent.*;


public class SmppClient {
    private final long DEFAULT_READ_TIMEOUT = 10000L;
    private final int DEFAULT_READ_BUFFER_SIZE = 4096;
    private final int MAX_PENDING_BUFFER_SIZE = 64 * 1024;

    private static  int  seq_id;

    private BuilderImpl conf;
    private AsynchronousSocketChannel channel;
    private ConcurrentLinkedQueue<BasePDU> output_queue = new ConcurrentLinkedQueue();
    private ConcurrentLinkedQueue<BasePDU> input_queue = new ConcurrentLinkedQueue();
    private Semaphore exitSem = new Semaphore(2);
    private boolean isClosingState=false;


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
            boolean result=channel.isOpen();
            return result;
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
            isClosingState=true;
            int cycle=0;
            while(exitSem.availablePermits()<2 && ++cycle<120)
                Thread.sleep(100);
            channel.close();
        } catch (InterruptedException
                | IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized  int getNextSeqNumber() {
        return ++seq_id;
    }
    public static synchronized  void resetSeqNumber() {
         seq_id=0;
    }


    private boolean getPduWillBeMore(ByteBuffer buffer) {
        BasePDU pdu;
        while((pdu=BasePDU.newPDU(buffer))!=null) {
            input_queue.add(pdu);
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

    void print_buff(ByteBuffer b) {
        byte[] a=b.array();
        int max=a.length>100?100:a.length;
        String s="";
        int c=0;
        for(int i=0;i<max;i++) {
            s+=Integer.toHexString(a[i]&0xff);
            if (c==16) {
                s+="*";
                c=0;
            }
            else {
                s+=",";
                c++;
            }
        }
        System.out.println(s);
    }

    public void processing() {
        final ByteBuffer buff = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);

        ExecutorService read_executor = Executors.newSingleThreadExecutor();
        read_executor.execute(() -> {
            boolean hasRemains=false;

            try {
                exitSem.acquire();
            } catch(InterruptedException e){}
            int num_read=0;
            while(num_read>=0 && !isClosingState) {
                Future<Integer> result;
                try {
                    result = channel.read(buff);
                }catch (ReadPendingException e) {
                    e.printStackTrace();
                        continue;
                }
                try{
                    while(!result.isDone())
                       Thread.sleep(5);
                    num_read = result.get(DEFAULT_READ_TIMEOUT,TimeUnit.MILLISECONDS);
                }catch(InterruptedException
                        | ExecutionException e){
                  //  e.printStackTrace();
                } catch ( TimeoutException e) {
                    continue;
                } catch (Exception e) {
                    continue;
                }
                if (num_read == -1) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (num_read>0) {
                    buff.limit(buff.position());
                    if (hasRemains) {
                        buff.reset();
                        hasRemains=false;
                    } else {
                        buff.flip();
                    }
                    if (getPduWillBeMore(buff))
                       hasRemains=true;
                    buff.limit(DEFAULT_READ_BUFFER_SIZE);
                }
            }
            exitSem.release();
        });

        ScheduledExecutorService write_executor = Executors.newScheduledThreadPool(1);

        Runnable write_task = () -> {
            if (!channel.isOpen() || isClosingState) {
                return;
            }
            try {
                exitSem.acquire();
            } catch(InterruptedException e){}
            BasePDU pdu = output_queue.poll();
            if (pdu != null) {
            //    LOG.info("prepare to write "+pdu.toString());
                ByteBuffer pduBytes = pdu.getBytes();
                while(pduBytes.hasRemaining() && !isClosingState) {
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
                            exitSem.release();
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        exitSem.release();
                        return;
                    }
                }
            }
            exitSem.release();
        };

        int period = 1;
        write_executor.scheduleWithFixedDelay(write_task, 0, 1000 / conf.mps, TimeUnit.MILLISECONDS);

    }

    private void writeBytes() {
        BasePDU pdu = output_queue.poll();
        if (pdu != null) {
            ByteBuffer pduBytes = pdu.getBytes();

        }
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
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() >= start + timeout_ms)
                break;
        }
        return res;
    }

}

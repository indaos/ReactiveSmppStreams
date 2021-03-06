package com.mobico.ress;

import com.mobico.ress.resmpp.SmppClient;
import com.mobico.ress.resmpp.pdu.BasePDU;
import com.mobico.ress.resmpp.pdu.Deliver;
import com.mobico.ress.restream.MessagesProcessor;
import com.mobico.ress.restream.Packet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.*;

import static org.junit.Assert.assertTrue;

public class StreamsTest {
    private final int BASE_APP_PORT = 9000;
    private final int BASE_SMSC_PORT = 8000;

    private final int MAX_MSG_PER_THREAD = 1000;
    private final int MAX_ACCEPT_DUR = 10000;

    private TestServer<Packet> appServer;
    private TestServer<BasePDU> smscServer;

    public final static Logger LOG = Logger.getLogger(SmppClient.class.getName());
    private static final ConsoleHandler handler = new ConsoleHandler();

    static {
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";
            @Override
            public synchronized String format(LogRecord l) {
                return String.format(format,new Date(l.getMillis()),l.getLoggerName(),l.getMessage()
                ); }
        });
        LOG.addHandler(handler);
        LOG.setLevel(Level.INFO);
        LOG.setUseParentHandlers(false);
    }


    public StreamsTest(){

    }

    @Before
    public void startServers() throws IOException {
        int port_app = BASE_APP_PORT + new Random().nextInt(1000);
        appServer = new TestServer()
                .withPort(port_app)
                .setPackerParser(new Function<ByteBuffer, Packet>() {
                    @Override
                    public Packet apply(ByteBuffer buffer) {
                        return Packet.newPacket(buffer);
                    }
                })
                .start();

        assertTrue(appServer.isOpen());

        LOG.info("Local App Server started: "+appServer.getPort());

        int port_smsc = BASE_SMSC_PORT + new Random().nextInt(1000);
        smscServer = new TestServer()
                .withPort(port_smsc)
                .setPackerParser(new Function<ByteBuffer, BasePDU>() {
                    @Override
                    public BasePDU apply(ByteBuffer buffer) {
                        return BasePDU.newPDU(buffer);
                    }
                })
                .start();

        assertTrue(smscServer.isOpen());

        LOG.info("Local SMSC  started: "+smscServer.getPort());

    }

    @After
    public void stopServers()  {
        appServer.close();
        LOG.info("Local App Server stopped: "+appServer.getPort());

       smscServer.close();
        LOG.info("Local Smsc Server stopped: "+smscServer.getPort());

    }



    @Test
    public void simpleBidirectionalTest() throws InterruptedException {

        SmppClient.resetSeqNumber();
        AtomicInteger tmpVar1=new AtomicInteger(0);
        AtomicInteger tmpVar2=new AtomicInteger(0);

        smscServer.setHandler((pdu) -> {
            int n=tmpVar1.incrementAndGet();
            LOG.info("smsc ["+n+"]: " + pdu.toString());
            return  BasePDU.of(pdu).getBytes().array();
        });

        appServer.setHandler((pdu) -> {
            int n=tmpVar2.incrementAndGet();
            LOG.info("app server ["+n+"]: " + pdu.toString());
            return null;
        });


        SmppClient<BasePDU> smppClient =(SmppClient) SmppClient.builder()
                .bindIp("localhost").host("localhost").port(smscServer.getPort())
                .username("guest").password("secret")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(2)
                .newSession();

        MessagesProcessor<Packet> appClient = (MessagesProcessor)MessagesProcessor.builder()
                .bindIp("localhost").host("localhost").port(appServer.getPort())
                .username("guest").password("secret")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(2)
                .newSession();


        smppClient.subscribe(appClient);
        appClient.subscribe(smppClient);

        assertTrue(appClient.connect(0));
        assertTrue(smppClient.connect(0));

        appClient.processing();
        smppClient.processing();

        Thread thr_app=new Thread() {
            public void run() {
                    for(int i=0;i<MAX_MSG_PER_THREAD;i++) {
                        appServer.send(new Packet()
                            .addValue("originated from app#1"));
                    }
                    LOG.info("app server finished");
                }
        };

        Thread thr_smsc=new Thread() {
            public void run() {
                for(int i=0;i<MAX_MSG_PER_THREAD;i++) {
                    smscServer.send(new Deliver()
                            .message("Hello!")
                            .setseqId());
                }
                LOG.info("smsc finished");
            }
        };

        thr_smsc.start();
        thr_app.start();

        thr_app.join();
        thr_smsc.join();


        long start=System.currentTimeMillis();

        while(tmpVar1.get()!=MAX_MSG_PER_THREAD || tmpVar1.get()!=tmpVar2.get()) {
            Thread.sleep(1);
           if (System.currentTimeMillis()-start> MAX_ACCEPT_DUR)
               assertTrue(false);
        }
        long end=System.currentTimeMillis();

        long duration=end-start;

        LOG.info("finished: "+duration+" ms ("+(float)MAX_MSG_PER_THREAD*2/duration*1000+" msg/sec)");

        appClient.closeAll();
        smppClient.closeAll();


    }

}

package com.mobico.ress;

import com.mobico.ress.resmpp.SmppClient;
import com.mobico.ress.resmpp.pdu.BasePDU;
import com.mobico.ress.resmpp.pdu.BindResp;
import com.mobico.ress.restream.MessagesProcessor;
import com.mobico.ress.restream.Packet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.*;

import static org.junit.Assert.assertTrue;

public class StreamsTest {
    private final int BASE_APP_PORT = 9000;
    private final int BASE_SMSC_PORT = 8000;

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
        appServer.run();

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
        smscServer.run();

        assertTrue(smscServer.isOpen());

        LOG.info("Local SMSC  started: "+smscServer.getPort());

    }

    @After
    public void stopServers()  {
        appServer.close();
        LOG.info("Local App Server stopped: "+appServer.getPort());

    //    smscServer.close();
    //    LOG.info("Local Smsc Server stopped: "+smscServer.getPort());

    }


    @Test
    public void simpleTest() throws InterruptedException {

        SmppClient.resetSeqNumber();

        smscServer.setHandler((pdu) -> {
            LOG.info("smsc got: " + pdu.toString());
            return new BindResp().of(pdu).getBytes().array();
        });

        appServer.setHandler((pdu) -> {
            LOG.info("app server got: " + pdu.toString());
            return null;
        });


        SmppClient<BasePDU> smppClient =(SmppClient) SmppClient.builder()
                .bindIp("localhost").host("localhost").port(smscServer.getPort())
                .username("guest").password("guest")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(2)
                .newSession();

        MessagesProcessor<Packet> appClient = (MessagesProcessor)MessagesProcessor.builder()
                .bindIp("localhost").host("localhost").port(appServer.getPort())
                .username("guest").password("guest")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(2)
                .newSession();


        smppClient.subscribe(appClient);
        appClient.subscribe(smppClient);

        assertTrue(appClient.connect(0));
        assertTrue(smppClient.connect(0));

        appClient.processing();
        smppClient.processing();

        appServer.send(new Packet()
                        .addValue("originated from app#1")
                        .addValue("originated from app#2"));

      //  Packet pdu;
        //assertNotNull(pdu=appClient.getNextPdu(20000));
       // LOG.info("got: "+pdu.toString());

       Thread.sleep(5000);

      //  smscServer.send(new Deliver()
      //          .message("originated from smsc")
       //         .setseqId());


       // appClient.closeAll();
       // smppClient.closeAll();

    }

}

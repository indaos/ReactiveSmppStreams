package com.mobico.ress;

import com.mobico.ress.resmpp.SmppClient;
import com.mobico.ress.resmpp.pdu.*;
import com.mobico.ress.util.ProtocolClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class SmppTest {
    private final int BASE_PORT = 8000;
    private final String OUTSIDE_SMSC_HOST="localhost";
    private final int OUTSIDE_SMSC_PORT=5000;
    private final int DEFAUL_READ_TIMEOUT=10000;

    private byte[] sample_pdu={ 0x0,0x0,0x0,0x56,0x0,0x0,0x0,0x5,0x0,0x0,
                                0x0,0x0,0x0,0x0,0x0,0x1,0x0,0x0,0x0,0x39,
                                0x30,0x30,0x37,0x34,0x32,0x38,0x36,0x31,
                                0x38,0x0,0x0,0x0,0x39,0x30,0x30,0x0,0x0,
                                0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x17,0x54,
                                0x68,0x69,0x73,0x20,0x69,0x73,0x20,0x61,
                                0x20,0x74,0x65,0x73,0x74,0x20,0x6d,0x65,
                                0x73,0x73,0x61,0x67,0x65,0x21,0x2,0xa,
                                0x0,0x2,0x0,0x14,0x2,0xb,0x0,0x2,0x0,0x14,
                                0x4,0x27,0x0,0x1,0x1};

    private TestServer<BasePDU> server;
    private ByteArrayOutputStream tempStream;

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

    @Before
    public void startLocalSmsc() throws IOException {
        int port = BASE_PORT + new Random().nextInt(1000);
        server = new TestServer()
                .withPort(port)
                .setPackerParser(new Function<ByteBuffer, BasePDU>() {
                    @Override
                    public BasePDU apply(ByteBuffer buffer) {
                        return BasePDU.newPDU(buffer);
                    }
                })
                .start();

        assertTrue(server.isOpen());

        LOG.info("Local SMSC  started: "+server.getPort());
    }

    @After
    public void stopLocalSmsc() {
        server.close();

        LOG.info("Local SMSC  stopped: "+server.getPort());
    }

    @Test
    public void simplePduTest() {
        LOG.info("***********************");
        BasePDU pdu=BasePDU.newPDU(ByteBuffer.wrap(sample_pdu));
        assertNotNull(pdu);
        LOG.info(pdu.toString());
        LOG.info("***********************");

    }

    @Test
    public void pduWithTLVTest() {
        LOG.info("***********************");
        BasePDU pdu=BasePDU.newPDU(ByteBuffer.wrap(sample_pdu));
        assertNotNull(pdu);
        LOG.info(pdu.tlvToString());
        LOG.info("***********************");

    }


    @Test
    public void seqConversationTest() throws IOException, InterruptedException {
        LOG.info("***********************");

        SmppClient.resetSeqNumber();

        server.setHandler((pdu)->{
                 LOG.info("server: "+pdu.toString());
                BasePDU resp=switch(pdu.getCommandId()){
                    case BaseOps.BIND_TRANSCEIVER -> new BindResp().of(pdu);
                    default->null;
                };
                try {
                    if (pdu.getSequenceNumber()==1) tempStream=new ByteArrayOutputStream();
                    tempStream.write(resp.getBytes().array());

                    if (pdu.getSequenceNumber()%10==0)  {
                        byte[] res=tempStream.toByteArray();
                        tempStream.reset();
                        return res;
                    }
                }catch(IOException|NullPointerException e){ e.printStackTrace(); }
                return null;
        });

        ProtocolClient<BasePDU> client = SmppClient.builder()
                .bindIp("localhost").host("localhost").port(server.getPort())
                .username("guest").password("secret")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(2)
                .newSession();

        assertTrue(client.connect(0));
        client.processing();

        for(int i=0;i<100;i++) {
            BasePDU pdu;
            client.send(0,pdu=new Bind().setSystemId("sys")
                    .setPassword("secret")
                    .setSysType("mock")
                    .setTon((byte) 1)
                    .setNpi((byte) 1)
                    .setseqId());
            LOG.info("sent: "+pdu.toString());
            Thread.sleep(new Random().nextInt(200));
        }

        Thread.sleep(new Random().nextInt(5000));

        for(int i=0;i<100;i++) {
            BasePDU pdu;
            assertNotNull(pdu=client.getNextPdu(DEFAUL_READ_TIMEOUT));
            assertTrue(pdu.getSequenceNumber()==(i+1));
            LOG.info("received: "+pdu.toString());
        }

        client.close(0);
        LOG.info("***********************");

    }

    @Test
    public void sophisticatedConversationTest() {
        LOG.info("***********************");

        SmppClient.resetSeqNumber();

        server.setHandler((pdu)->{
                LOG.info("server: "+pdu.toString());

                BasePDU resp=BasePDU.of(pdu);
                if (resp==null) return null;
                if (resp instanceof  SubmitResp) {
                    tempStream=null;
                    try {
                        tempStream = new ByteArrayOutputStream();
                        tempStream.write(resp.getBytes().array());
                        tempStream.write(new Deliver()
                                .message("Hello!")
                                .setseqId().getBytes().array());
                    }catch(IOException e) { e.printStackTrace(); }
                    return tempStream.toByteArray();
                } else
                 return resp.getBytes().array();
            });

        ProtocolClient<BasePDU> client = SmppClient.builder()
                    .bindIp("localhost").host("localhost").port(server.getPort())
                    .username("guest").password("secret")
                    .systype("ReSmpp").timeout(30 * 1000)
                    .maxmps(2)
                    .newSession();

        assertTrue(client.connect(0));
        client.processing();

        BasePDU pdu;
        BasePDU resp;

        client.send(0,pdu=new Bind().setSystemId("sys")
                .setPassword("secret")
                .setSysType("mock")
                .setTon((byte) 1)
                .setNpi((byte) 1)
                .setseqId());
        LOG.info("sent: "+pdu.toString());

        assertNotNull(resp=client.getNextPdu(DEFAUL_READ_TIMEOUT));
        assertTrue(resp instanceof BindResp);
        assertTrue(resp.getSequenceNumber()==pdu.getSequenceNumber());
        LOG.info("received: "+resp.toString());

        client.send(0,pdu=new Submit()
                .message("Hello!")
                .setseqId());
        LOG.info("sent: "+pdu.toString());


        assertNotNull(resp=client.getNextPdu(DEFAUL_READ_TIMEOUT));
        assertTrue(resp instanceof SubmitResp);
        assertTrue(resp.getSequenceNumber()==pdu.getSequenceNumber());
        LOG.info("received: "+resp.toString());

        assertNotNull(resp=client.getNextPdu(DEFAUL_READ_TIMEOUT));
        assertTrue(resp instanceof Deliver);
        LOG.info("received: "+resp.toString());

        client.send(0,resp=BasePDU.of(resp));
        LOG.info("sent: "+resp.toString());

        client.send(0,pdu=new Unbind().setseqId());
        LOG.info("sent: "+pdu.toString());

        assertNotNull(resp=client.getNextPdu(DEFAUL_READ_TIMEOUT));

        assertTrue(resp instanceof UnbindResp);
        assertTrue(resp.getSequenceNumber()==pdu.getSequenceNumber());

        LOG.info("received: "+resp.toString());

        client.close(0);
        LOG.info("***********************");
    }

    @Test
    public void multisessionClientTest() throws InterruptedException {

        LOG.info("***********************");

        BiPredicate<ProtocolClient<BasePDU>,Integer> sendSMS=(ProtocolClient<BasePDU> client,Integer channel)->{
            for(int i=0;i<20;i++) {
                client.send(channel==-1?(i % 2 == 0 ? 1 : 0):channel,new Bind().setSystemId("sys")
                        .setPassword("secret")
                        .setSysType("mock")
                        .setTon((byte) 1)
                        .setNpi((byte) 1));
                BasePDU pdu;
                assertNotNull(pdu = client.getNextPdu(DEFAUL_READ_TIMEOUT));
                LOG.info("client: " + pdu.toString());
            }
            return true;
        };

        server.setHandler((pdu) -> {
            LOG.info("server: " + pdu.toString());
            return new BindResp().of(pdu).getBytes().array();
        });

        ProtocolClient<BasePDU> client = SmppClient.builder()
                .bindIp("localhost").host("localhost").port(server.getPort())
                .username("guest").password("secret")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(1)
                .newSession();
        LOG.info("session started: 0");

        SmppClient.builder()
                .bindIp("localhost").host("localhost").port(server.getPort())
                .username("guest").password("secret")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(1)
                .newSession();
        LOG.info("session started: 1");

        assertTrue(client.connect(0));
        assertTrue(client.connect(1));

        LOG.info("processing");

        client.processing();

        sendSMS.test(client,-1);

        client.close(0);
        LOG.info("********* close : 0**************");

        sendSMS.test(client,1);

        assertTrue(client.connect(0));

        LOG.info("********* connect : 0**************");

        sendSMS.test(client,-1);

        LOG.info("***********************");

    }


}

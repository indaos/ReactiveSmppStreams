package com.mobico.resmpprest;

import com.mobico.resmpprest.smpp.SmppClient;
import com.mobico.resmpprest.smpp.pdu.BasePDU;
import com.mobico.resmpprest.smpp.pdu.Bind;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest {
    private final int BASE_PORT = 8000;

    private byte[] sample_pdu={0x0,0x0,0x0,0x56,0x0,0x0,0x0,0x5,0x0,0x0,
            0x0,0x0,0x0,0x0,0x0,0x1,0x0,0x0,0x0,0x39,
            0x30,0x30,0x37,0x34,0x32,0x38,0x36,0x31,0x38,0x0,
            0x0,0x0,0x39,0x30,0x30,0x0,0x0,0x0,0x0,0x0,
            0x0,0x0,0x0,0x0,0x0,0x17,0x54,0x68,0x69,0x73,
            0x20,0x69,0x73,0x20,0x61,0x20,0x74,0x65,0x73,0x74,
            0x20,0x6d,0x65,0x73,0x73,0x61,0x67,0x65,0x21,0x2,
            0xa,0x0,0x2,0x0,0x14,0x2,0xb,0x0,0x2,0x0,
            0x14,0x4,0x27,0x0,0x1,0x1};

    @Test
    public void pduTest() {

        BasePDU pdu=BasePDU.newPDU(ByteBuffer.wrap(sample_pdu));
        assertNotNull(pdu);
        System.out.println(pdu.toString());
    }

    @Test
    public void tempTest() {
        SmppClient client = SmppClient.builder()
                .bindIp("localhost").host("localhost").port(5000)
                .username("guest").password("guest")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(1)
                .newClient();

        assertTrue(client.connect());
        client.processing();

        client.send(new Bind().setSystemId("sys")
                    .setPassword("secret")
                    .setSysType("mock")
                    .setTon((byte)1)
                    .setNpi((byte)1));

        BasePDU pdu;
        assertNotNull(pdu=client.getNextPdu(10000));
        System.out.println(pdu.toString());
    }




    public void simpleConnectionTest() throws IOException, InterruptedException {
        int port = BASE_PORT + new Random().nextInt(1000);
        TestSmppServer server = new TestSmppServer()
                .withPort(port)
                .start();
        server.run();

        SmppClient client = SmppClient.builder()
                .bindIp("localhost").port(port)
                .username("guest").password("guest")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(1)
                .newClient();

        assertTrue(client.connect());

        client.processing();

       // client.send(new BasePDU(0));

        assertNotNull(client.getNextPdu(10000));

        client.close();
        server.close();
    }
}

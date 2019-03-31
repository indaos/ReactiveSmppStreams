package com.mobico.resmpprest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import com.mobico.resmpprest.smpp.SmppClient;
import com.mobico.resmpprest.smpp.pdu.BasePDU;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	private final int BASE_PORT=8000;
	
    @Test
    public void simpleConnectionTest() throws IOException,InterruptedException
    {
    	int  port=BASE_PORT+new Random().nextInt(1000);
    	TestSmppServer server = new TestSmppServer()
    			.withPort(port)
    			.start();
    	server.run();
    	
    	SmppClient client=SmppClient.builder()
		    	.bindIp("localhost").port(port)
		    	.username("guest").password("guest")
		    	.systype("ReSmpp").timeout(30*1000)
		    	.maxmps(1)
		    	.newClient();
    	
    	assertTrue(client.connect());
    		
    	client.processing();
    	
    	client.send(new BasePDU());
    	    	
    	assertNotNull(client.get_next_pdu(10000));
    	
    	client.close();
    	server.close();
    }
}

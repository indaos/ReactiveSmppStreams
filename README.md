# ReactiveSmppStreams
<p>
This is an example of a server-to-server chain implementation where a SMPP (Short Message Peer-to-Peer) client using NIO in a single thread can provide many TCP sessions . 
</p>
<p>

The other asynchronous client  using reactive streams (java.util.concurrent.Flow) to communicate with the SMPP client sends messages to the application server.
</p>
<p>

Thus, communication between many SMSC (Short Message Service Center) and many application servers can be served by only two threads.
</p>

### connecting to SMSC
<pre>
ProtocolClient<BasePDU> client = SmppClient.builder()
                .bindIp("localhost").host("localhost").port(server.getPort())
                .username("guest").password("secret")
                .systype("ReSmpp").timeout(30 * 1000)
                .maxmps(2)
                .newSession();
client.connect(0)
</pre>  

###  processing of PDUs received from all SMSCs.
<pre>
 client.processing();
 BasePDU pdu;
 while (pdu!= null )  {
    pdu=client.getNextPdu(DEFAUL_READ_TIMEOUT));
 }
</pre>  

### sending PDU
<pre>
 client.send(0,pdu=new Submit()
                .message("Hello!")
                .setseqId());
 client.close(0)
</pre>    

### SMSC simulator
<pre>
 server = new TestServer()
          .withPort(port)
          .setPackerParser(new Function<ByteBuffer, BasePDU>() {
              @Override
              public BasePDU apply(ByteBuffer buffer) {
                  return BasePDU.newPDU(buffer);
              }
          }).start();
</pre>                

### processing PDUs received from clients
<pre>
server.setHandler((pdu)->{
              BasePDU resp=switch(pdu.getCommandId()){
                  case BaseOps.BIND_TRANSCEIVER -> new BindResp().of(pdu);
                  default->null;
              };
              return null;
        });
</pre>

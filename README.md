# ReactiveSmppStreams
<p>
This is an example of a server-to-server chain implementation where a SMPP (Short Message Peer-to-Peer) client using NIO in a single thread can provide many TCP sessions . 
</p>
<p>

The other asynchronous client  using reactive streams (java.util.concurrent.Flow) to communicate with the SMPP client sends messages to the application server.
</p>
<p>

Thus, communication between several SMSC (Short Message Service Center) and several application servers can be served by only two threads.
</p>

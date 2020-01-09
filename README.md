# ReactiveSmppStreams

This is an example of a server-to-server chain implementation where a SMPP (Short Message Peer-to-Peer) client using nio in a single thread can provide many tcp sessions . 
The other asynchronous client  using reactive streams (java.util.concurrent.Flow) to communicate with the SMPP client sends messages to the application server. 
Thus, communication between several SMSC (Short Message Service Center) and several application servers can be served by only two threads.

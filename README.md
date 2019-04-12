# ReactiveSmppStreams

This is an example of a server-to-server chain implementation where a smpp client using nio in a single thread can support many tcp sessions . The other asynchronous client  using reactive streams (java.util.concurrent.Flow) to communicate with the smpp client sends messages to the application server. Therefore, communication between multiple smsc and multiple application servers can be served by two threads.


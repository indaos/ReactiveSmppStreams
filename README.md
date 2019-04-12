# ReactiveSmppRest

This is an example of a server-to-server chain implementation where a smpp client using nio2 in a single thread can support many tcp sessions . The same other client sends messages to the application server using reactive threads to communicate with the smpp client.Therefore, communication between multiple smsc and multiple application servers can be served by two threads

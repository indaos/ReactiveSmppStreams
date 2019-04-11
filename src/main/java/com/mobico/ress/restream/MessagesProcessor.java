package com.mobico.ress.restream;

import com.mobico.ress.resmpp.pdu.Deliver;
import com.mobico.ress.util.AsyncProcessing;
import com.mobico.ress.util.Builder;
import com.mobico.ress.util.ProtocolClient;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.function.Function;

public class MessagesProcessor<T extends Packet> extends AsyncProcessing<T> implements Flow.Processor, ProtocolClient<T> {

    private int c_cptr=0;
    private Flow.Subscription subscription;

    public MessagesProcessor(){

    }

    public MessagesProcessor(BuilderMsgImpl conf) {
        super(conf);
        setUpParser();
    }


    public static Builder builder() {
        return new BuilderMsgImpl();
    }

    public void send(Packet pdu){
        if (c_cptr == getNumChannels()) c_cptr=0;
        send(c_cptr++,pdu);
    }

    private void setUpParser() {
        super.setParser(new Function<ByteBuffer,T>() {
            @Override
            public T apply(ByteBuffer buffer) {
                return (T) Packet.newPacket(buffer);
            }
        });
    }

    /*PUBLISHER*/
    @Override
    public void subscribe(Flow.Subscriber subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long count) {
                int received=0;
                while(received!=count) {
                    T  pdu=getNextPdu(0);
                    if (pdu!=null) {
                        subscriber.onNext(pdu);
                        received++;
                    } else break;
                }
            }
            @Override
            public void cancel() {
            }
        });
    }

    protected void loadNextPduFromPublisher() {
       if(subscription!=null) subscription.request(1);
    }

    /*SUBSCRIBER*/
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription=subscription;
    }

    @Override
    public void onNext(Object item) {
        if (item instanceof Deliver) {
            Deliver pdu=(Deliver)item;
            byte[] msg=pdu.getMessage();
            send(Packet.newPacket(ByteBuffer.wrap(msg)));
        }
    }


    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

    /*PROTOCOL*/
    @Override
    public void processing() {
        ExecutorService read_executor = Executors.newSingleThreadExecutor();
        read_executor.execute(() -> super.multiChannelProcessing());
    }

    @Override
    public void send(int channeld,Packet pdu) {
        addOutputPDU(channeld,(T)pdu);
    }
    @Override
    public T getNextPdu() {
        return getNextInputPDU();
    }
    @Override
    public T getNextPdu(long timeout_ms) {
        return getNextInputPDU(timeout_ms);
    }

}

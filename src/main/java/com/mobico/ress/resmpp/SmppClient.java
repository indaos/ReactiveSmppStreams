package com.mobico.ress.resmpp;

import com.mobico.ress.resmpp.pdu.BasePDU;
import com.mobico.ress.resmpp.pdu.Submit;
import com.mobico.ress.restream.Packet;
import com.mobico.ress.util.AsyncProcessing;
import com.mobico.ress.util.Builder;
import com.mobico.ress.util.ProtocolClient;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;


public class SmppClient<T extends BasePDU>  extends AsyncProcessing<T>  implements Flow.Processor,ProtocolClient<T> {

    private static  int  seq_id;
    private int c_cptr=0;
    private Flow.Subscription subscription;

    private SmppClient() {
    }

    public SmppClient(BuilderSmppImpl conf) {
        super(conf);
        setUpParser();
    }


    public static Builder builder() {
        return new BuilderSmppImpl();
    }


    public void processing() {
        ExecutorService read_executor = Executors.newSingleThreadExecutor();
        read_executor.execute(() -> super.multiChannelProcessing());
    }


    public void send(int channeld, BasePDU pdu) {
         addOutputPDU(channeld,(T)pdu);
    }

    public T getNextPdu() {
        return getNextInputPDU();
    }

    public T getNextPdu(long timeout_ms) {
        return getNextInputPDU(timeout_ms);
    }

    public static synchronized  int getNextSeqNumber() {
        return ++seq_id;
    }
    public static synchronized  void resetSeqNumber() {
        seq_id=0;
    }

    private void setUpParser() {
        super.setParser(new Function<ByteBuffer,T>() {
            @Override
            public T apply(ByteBuffer buffer) {
                return (T)BasePDU.newPDU(buffer);
            }
        });
    }


    public void send(BasePDU pdu){
       if (c_cptr == getNumChannels()) c_cptr=0;
       send(c_cptr++,pdu);
    }

    protected void loadNextPduFromPublisher() {
        if(subscription!=null) subscription.request(1);
    }

    /*PUBLISHER*/
    @Override
    public void subscribe(Flow.Subscriber  subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long count) {
                    int received=0;
                    while(received!=count) {
                        T pdu=getNextPdu(100);
                        if (pdu!=null) {
                            subscriber.onNext(pdu);
                            received++;
                        }
                        else break;
                    }
            }
            @Override
            public void cancel() {
            }
        });
    }

    /*SUBSCRIBER*/
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
       this.subscription=subscription;
    }

    @Override
    public void onNext(Object item) {
        if (item instanceof Packet) {
            Packet packet=(Packet)item;
            String[] values=packet.getValues();
            if (values==null) return;
            for(int i=0;i<values.length;i++)
            send(new Submit()
                    .message(values[i])
                    .setseqId());
        }
    }

    @Override
    public void onError(Throwable throwable) {
      //  subscribtionReady=false;
    }

    @Override
    public void onComplete() {
     //   subscribtionReady=false;
    }


}

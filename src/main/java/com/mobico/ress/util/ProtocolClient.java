package com.mobico.ress.util;

public interface ProtocolClient<T> {
    public void send(int channeld, T pdu);

    public T getNextPdu();

    public T getNextPdu(long timeout_ms);

    public boolean connect(int index);

    public void close(int index);

    public void processing();

}

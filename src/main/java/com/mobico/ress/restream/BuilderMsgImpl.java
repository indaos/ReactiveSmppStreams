package com.mobico.ress.restream;


import com.mobico.ress.util.Builder;
import com.mobico.ress.util.ProtocolClient;

public class BuilderMsgImpl implements Builder {

    private String bindip;
    private String host;
    private int port;
    private String username;
    private String password;
    private String systype;
    private int time;
    private int mps;
    private int index=-1;
    private static MessagesProcessor client=null;

    public BuilderMsgImpl(){
    }


    @Override
    public Builder bindIp(String bindip) {
        this.bindip = bindip;
        return this;
    }

    @Override
    public Builder host(String host) {
        this.host = host;
        return this;
    }

    @Override
    public Builder port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public Builder username(String username) {
        this.username = username;
        return this;
    }

    @Override
    public Builder password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public Builder systype(String systype) {
        this.systype = systype;
        return this;
    }

    @Override
    public Builder timeout(int time) {
        this.time = time;
        return this;
    }

    @Override
    public Builder maxmps(int mps) {
        this.mps = mps;
        return this;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public ProtocolClient<String> newSession() {
        if (client==null)
            client=new MessagesProcessor<Packet>(this);
        client.addBuilder(this);
        return client;
    }

}

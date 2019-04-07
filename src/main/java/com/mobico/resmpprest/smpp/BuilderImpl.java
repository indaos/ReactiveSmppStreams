package com.mobico.resmpprest.smpp;


public class BuilderImpl implements Builder {

    String bindip;
    String host;
    int port;
    String username;
    String password;
    String systype;
    int time;
    int mps;
    MultiSessionSmppClient client;
    int index=-1;

    public BuilderImpl(){
    }

    public BuilderImpl(MultiSessionSmppClient client,int index){
        this.client=client;
        this.index=index;
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
    public SmppClient newClient() {
        return new SmppClient(this);
    }
    @Override
    public MultiSessionSmppClient client() {
        return client;
    }

}

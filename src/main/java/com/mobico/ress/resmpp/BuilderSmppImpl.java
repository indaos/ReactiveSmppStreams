package com.mobico.ress.resmpp;


import com.mobico.ress.resmpp.pdu.BasePDU;
import com.mobico.ress.util.Builder;
import com.mobico.ress.util.ProtocolClient;

public class BuilderSmppImpl implements Builder {

    private static SmppClient client = null;
    private String bindip;
    private String host;
    private int port;
    private String username;
    private String password;
    private String systype;
    private int time;
    private int mps;
    private int index = -1;

    public BuilderSmppImpl() { }

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

    public String getBindip() { return bindip; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public String getSystype() { return systype; }

    public int getTime() { return time; }

    public int getMps() { return mps; }


    @Override
    public ProtocolClient<BasePDU> newSession() {
        if (client == null) client = new SmppClient<BasePDU>(this);
        client.addBuilder(this);
        return client;
    }

}

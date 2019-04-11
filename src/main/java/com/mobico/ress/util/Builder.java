package com.mobico.ress.util;

public interface Builder {
    Builder bindIp(String ip);

    Builder host(String host);

    Builder port(int port);

    Builder username(String name);

    Builder password(String pswd);

    Builder systype(String type);

    Builder timeout(int timeout);

    Builder maxmps(int mps);

    String getHost();

    int getPort();

    ProtocolClient newSession();
}
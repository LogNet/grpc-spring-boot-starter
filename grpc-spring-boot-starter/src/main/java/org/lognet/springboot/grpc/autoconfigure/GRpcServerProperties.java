package org.lognet.springboot.grpc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by alexf on 26-Jan-16.
 */
@ConfigurationProperties("grpc")
public class GRpcServerProperties {
    /**
     * enable/disable
     */
    private boolean enable = true;
    /**
     * gRPC server port
     */
    private int port = 6565;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}

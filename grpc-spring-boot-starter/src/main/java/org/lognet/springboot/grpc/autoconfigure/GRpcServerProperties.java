package org.lognet.springboot.grpc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by alexf on 26-Jan-16.
 */
@ConfigurationProperties("grpc")
public class GRpcServerProperties {
    /**
     * gRPC server port
     */
    private int port = 6565;

    private String inProcessServerName = "gRPC-InProcess";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getInProcessServerName() {
        return inProcessServerName;
    }

    public void setInProcessServerName(String inProcessServerName) {
        this.inProcessServerName = inProcessServerName;
    }
}

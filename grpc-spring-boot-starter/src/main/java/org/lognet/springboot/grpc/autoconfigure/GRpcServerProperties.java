package org.lognet.springboot.grpc.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Created by alexf on 26-Jan-16.
 */

@ConfigurationProperties("grpc")
@Getter @Setter
public class GRpcServerProperties {
    /**
     * gRPC server port
     *
     */
    private int port = 6565;


    /**
     * Enables the embedded grpc server.
     */
    private boolean enabled = true;


    /**
     * In process server name.
     * If  the value is not empty, the embedded in-process server will be created and started.
     *
     */
    private String inProcessServerName;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInProcessServerName() {
        return inProcessServerName;
    }

    public void setInProcessServerName(String inProcessServerName) {
        this.inProcessServerName = inProcessServerName;
    }
}

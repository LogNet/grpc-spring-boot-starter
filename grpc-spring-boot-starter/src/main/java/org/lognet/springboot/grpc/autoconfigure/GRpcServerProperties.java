package org.lognet.springboot.grpc.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

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

    public int getPort() {
        if (port == 0) {
            port = SocketUtils.findAvailableTcpPort(40000, 50000);
        }
        return port;
    }

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


}

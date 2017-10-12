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

    /**
     * Transport security options for the gRPC server. Transport security is disabled by default.
     *
     * Additional dependencies are needed to start a server with TLS enabled.
     * See https://github.com/grpc/grpc-java/blob/master/SECURITY.md#transport-security-tls
     */
    private final TransportSecurity transportSecurity = new TransportSecurity();

    @Getter @Setter
    public static class TransportSecurity {

        /**
         * TLS will only be enabled if this is set to true.
         */
        private boolean enabled = false;

        /**
         * Path to the file containing the complete certificateChain. Path should be the absolute path to that file.
         */
        private String certificateChainFilePath = "";

        /**
         * Path to the file containing the private key in PKCS8 format. Path should be the absolute path to that file.
         */
        private String privateKeyFilePath = "";

    }

}

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

    /**
     * consul host
     */
    private String consulHost;

    /**
     * local ip address
     */
    private String localIp;

    /**
     * Health check Url
     */
    private String healthUrl;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConsulHost() {
        return consulHost;
    }

    public void setConsulHost(String consulHost) {
        this.consulHost = consulHost;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getHealthUrl() {
        return healthUrl;
    }

    public void setHealthUrl(String healthUrl) {
        this.healthUrl = healthUrl;
    }
}

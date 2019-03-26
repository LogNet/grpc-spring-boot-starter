package org.lognet.springboot.grpc.autoconfigure;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.SocketUtils;

import java.util.Optional;

/**
 * Created by alexf on 26-Jan-16.
 */

@ConfigurationProperties("grpc")
@Getter @Setter
public class GRpcServerProperties {
    public static final int DEFAULT_GRPC_PORT = 6565;
    /**
     * gRPC server port
     *
     */
    private Integer port = null;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private Integer runningPort= null;

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
     * Enables server reflection using <a href="https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md">ProtoReflectionService</a>.
     * Available only from gRPC 1.3 or higher.
     */
    private boolean enableReflection = false;

    public Integer getRunningPort(){
        if ( null == runningPort) {
            synchronized (this) {
                if (null==runningPort) {
                    runningPort = Optional.ofNullable(port)
                            .map(p -> 0 == p ? SocketUtils.findAvailableTcpPort() : p)
                            .orElse(DEFAULT_GRPC_PORT);
                }
            }
        }
        return runningPort;

    }

}

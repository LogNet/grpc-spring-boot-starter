package org.lognet.springboot.grpc.health;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

@Slf4j
public class GrpcHealthProbeContainer extends GenericContainer<GrpcHealthProbeContainer> {
    public GrpcHealthProbeContainer() {
        super("slyncio/grpc-health-probe");
        withLogConsumer(f -> log.info(f.getUtf8String()))
        .withCreateContainerCmdModifier(cmd ->
                cmd.withStdinOpen(true)
                    .withEntrypoint("sh")
        );
    }


}

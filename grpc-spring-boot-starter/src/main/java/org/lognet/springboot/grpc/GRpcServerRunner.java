package org.lognet.springboot.grpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 *  Hosts embedded gRPC server.
 */
@Slf4j
public class GRpcServerRunner implements CommandLineRunner,DisposableBean {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GRpcServerProperties gRpcServerProperties;

    private Server server;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(gRpcServerProperties.getPort());

        // find and register all GRpcService-enabled beans
        for(Object grpcService : applicationContext.getBeansWithAnnotation(GRpcService.class).values()) {

            // register service
            if (BindableService.class.isAssignableFrom(grpcService.getClass())) {

                BindableService bindableService = BindableService.class.cast(grpcService);

                ServerServiceDefinition definition = bindInterceptors(bindableService);
                serverBuilder.addService(definition);
                log.info("'{}' service has been registered.", bindableService.getClass().getName());
            } else {
                throw new IllegalArgumentException(String.format("%s should be of type %s" ,grpcService.getClass().getName(), BindableService.class.getName()));
            }
        }

        server = serverBuilder.build().start();
        log.info("gRPC Server started, listening on port {}.", gRpcServerProperties.getPort());
        startDaemonAwaitThread();

    }

    private ServerServiceDefinition bindInterceptors(BindableService bindableService) {
        Class<? extends ServerInterceptor>[] interceptorClasses = bindableService.getClass().getAnnotation(GRpcService.class).interceptors();

        if(interceptorClasses.length == 0) {
            bindableService.bindService();
        }

        List<ServerInterceptor> interceptors = buildInterceptors(interceptorClasses);
        return ServerInterceptors.intercept(bindableService, interceptors);
    }

    private List<ServerInterceptor> buildInterceptors(Class<? extends ServerInterceptor>[] classes) {
        List<ServerInterceptor> interceptors = new ArrayList<>();

        for(Class<? extends ServerInterceptor> clazz : classes) {
            try {
                interceptors.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Unable to instantiate ServerInterceptor from class {}", clazz, e);
            }
        }

        return interceptors;
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread() {
            @Override
            public void run() {
                try {
                    GRpcServerRunner.this.server.awaitTermination();
                } catch (InterruptedException e) {
                    log.error("gRPC server stopped.",e);
                }
            }

        };
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
    @Override
    public void destroy() throws Exception {
        log.info("Shutting down gRPC server ...");
        Optional.ofNullable(server).ifPresent(Server::shutdown);
        log.info("gRPC server stopped.");
    }
}

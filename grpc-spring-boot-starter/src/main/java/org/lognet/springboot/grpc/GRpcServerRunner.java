package org.lognet.springboot.grpc;

import com.google.common.base.Supplier;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 *  Hosts embedded gRPC server.
 */
@Slf4j
public class GRpcServerRunner implements CommandLineRunner,DisposableBean {

    /**
     * Name of static function of gRPC service-outer class that creates {@link io.grpc.ServerServiceDefinition}.
     */
    final private  static String bindServiceMethodName = "bindService";

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
            final Class<?> grpcServiceOuterClass = AnnotationUtils.findAnnotation(grpcService.getClass(), GRpcService.class).grpcServiceOuterClass();

            // find 'bindService' method on outer class.
            final Optional<Method> bindServiceMethod = Arrays.asList(ReflectionUtils.getAllDeclaredMethods(grpcServiceOuterClass)).stream().filter(
                    method ->  bindServiceMethodName.equals(method.getName()) && 1 == method.getParameterCount() && method.getParameterTypes()[0].isAssignableFrom(grpcService.getClass())
            ).findFirst();

            // register service
            if (bindServiceMethod.isPresent()) {
                ServerServiceDefinition serviceDefinition = (ServerServiceDefinition) bindServiceMethod.get().invoke(null, grpcService);
                serverBuilder.addService(serviceDefinition);
                log.info("'{}' service has been registered.", serviceDefinition.getName());
            } else {
                throw new IllegalArgumentException(String.format("Failed to find '%s' method on class %s.\r\n" +
                                "Please make sure you've provided correct 'grpcServiceOuterClass' attribute for '%s' annotation.\r\n" +
                                "It should be the protoc-generated outer class of your service."
                        , bindServiceMethodName,grpcServiceOuterClass.getName(), GRpcService.class.getName()));
            }
        }

        server = serverBuilder.build().start();
        log.info("gRPC Server started, listening on port {}.", gRpcServerProperties.getPort());
        startDaemonAwaitThread();

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

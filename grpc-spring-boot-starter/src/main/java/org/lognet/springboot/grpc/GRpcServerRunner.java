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
 * Created by alexf on 25-Jan-16.
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
        for(Object grpcService : applicationContext.getBeansWithAnnotation(GRpcService.class).values()) {
            final Class<?> grpcClass = AnnotationUtils.findAnnotation(grpcService.getClass(), GRpcService.class).grpcClass();

            final String bindServiceMethodName = "bindService";
            final Optional<Method> bindServiceMethod = Arrays.asList(ReflectionUtils.getAllDeclaredMethods(grpcClass)).stream().filter(
                    method ->  bindServiceMethodName.equals(method.getName()) && 1 == method.getParameterCount() && method.getParameterTypes()[0].isAssignableFrom(grpcService.getClass())
            ).findFirst();
            if (bindServiceMethod.isPresent()) {
                ServerServiceDefinition serviceDefinition = (ServerServiceDefinition) bindServiceMethod.get().invoke(null, grpcService);
                serverBuilder.addService(serviceDefinition);
                log.info("'{}' service has been registered.", serviceDefinition.getName());
            } else {
                throw new IllegalArgumentException(String.format("Failed to find '%s' method on class %s.\r\n" +
                                "Please make sure you've provided correct 'grpcClass' attribute for %s annotation.\r\n" +
                                "It should be the gRPC generated outer class of your service."
                        , grpcClass.getName(), bindServiceMethodName,GRpcService.class.getName()));
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
                    log.error("GRpc server stopped.",e);
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

package org.lognet.springboot.grpc;

import io.grpc.*;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hosts embedded gRPC server.
 */
@Slf4j
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

    @Autowired
    private HealthStatusManager healthStatusManager;

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private GRpcServerProperties gRpcServerProperties;

    private final Consumer<ServerBuilder<?>> configurator;

    private Server server;

    private final ServerBuilder<?> serverBuilder;

    private final CountDownLatch latch;

    public GRpcServerRunner(Consumer<ServerBuilder<?>> configurator, ServerBuilder<?> serverBuilder) {
        this.configurator = configurator;
        this.serverBuilder = serverBuilder;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        Collection<ServerInterceptor> globalInterceptors = getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class, ServerInterceptor.class)
                .map(name -> applicationContext.getBeanFactory().getBean(name, ServerInterceptor.class))
                .collect(Collectors.toList());

        // Adding health service
        serverBuilder.addService(healthStatusManager.getHealthService());

        // find and register all GRpcService-enabled beans
        getBeanNamesByTypeWithAnnotation(GRpcService.class, BindableService.class)
                .forEach(name -> {
                    BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = srv.bindService();
                    GRpcService gRpcServiceAnn = applicationContext.findAnnotationOnBean(name, GRpcService.class);
                    serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);
                    serverBuilder.addService(serviceDefinition);
                    String serviceName = serviceDefinition.getServiceDescriptor().getName();
                    healthStatusManager.setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING);

                    log.info("'{}' service has been registered.", srv.getClass().getName());

                });

        if (gRpcServerProperties.isEnableReflection()) {
            serverBuilder.addService(ProtoReflectionService.newInstance());
            log.info("'{}' service has been registered.", ProtoReflectionService.class.getName());
        }

        configurator.accept(serverBuilder);
        server = serverBuilder.build().start();
        applicationContext.publishEvent(new GRpcServerInitializedEvent(applicationContext,server));

        log.info("gRPC Server started, listening on port {}.", server.getPort());
        startDaemonAwaitThread();

    }

    private ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, GRpcService gRpcService, Collection<ServerInterceptor> globalInterceptors) {

        Stream<? extends ServerInterceptor> privateInterceptors = Stream.of(gRpcService.interceptors())
                .map(interceptorClass -> {
                    try {
                        return 0 < applicationContext.getBeanNamesForType(interceptorClass).length ?
                                applicationContext.getBean(interceptorClass) :
                                interceptorClass.newInstance();
                    } catch (Exception e) {
                        throw new BeanCreationException("Failed to create interceptor instance.", e);
                    }
                });

        List<ServerInterceptor> interceptors = Stream.concat(
                gRpcService.applyGlobalInterceptors() ? globalInterceptors.stream() : Stream.empty(),
                privateInterceptors)
                .distinct()
                .sorted(serverInterceptorOrderComparator())
                .collect(Collectors.toList());
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
    }

    private Comparator<Object> serverInterceptorOrderComparator() {
        Function<Object,Boolean> isOrderAnnotated = obj->{
            Order ann = obj instanceof Method ? AnnotationUtils.findAnnotation((Method) obj, Order.class) :
                    AnnotationUtils.findAnnotation(obj.getClass(), Order.class);
            return ann != null;
        };
        return AnnotationAwareOrderComparator.INSTANCE.thenComparing((o1, o2) -> {
            boolean p1 = isOrderAnnotated.apply(o1);
            boolean p2 = isOrderAnnotated.apply(o2);
            return p1 && !p2 ? -1 : p2 && !p1 ? 1 : 0;
        }).reversed();
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(()->{
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    log.error("gRPC server awaiter interrupted.", e);
                }
            });
        awaitThread.setName("grpc-server-awaiter");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void destroy() throws Exception {

        Optional.ofNullable(server).ifPresent(s->{
            log.info("Shutting down gRPC server ...");
            s.getServices().forEach(def->healthStatusManager.clearStatus(def.getServiceDescriptor().getName()));
            s.shutdown();
            int shutdownGrace = gRpcServerProperties.getShutdownGrace();
            try {
                // If shutdownGrace is 0, then don't call awaitTermination
                if (shutdownGrace < 0) {
                    s.awaitTermination();
                } else if (shutdownGrace > 0) {
                    s.awaitTermination(shutdownGrace, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                log.error("gRPC server interrupted during destroy.", e);
            } finally {
                latch.countDown();
            }
            log.info("gRPC server stopped.");
        });

    }

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) throws Exception {

        return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name -> {
                    final BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);

                    if (beansWithAnnotation.containsKey(name)) {
                        return true;
                    } else if (beanDefinition.getSource() instanceof AnnotatedTypeMetadata) {
                        return AnnotatedTypeMetadata.class.cast(beanDefinition.getSource()).isAnnotated(annotationType.getName());

                    }

                    return false;
                });
    }


}

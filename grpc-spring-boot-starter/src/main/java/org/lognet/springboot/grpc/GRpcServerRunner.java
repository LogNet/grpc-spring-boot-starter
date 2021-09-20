package org.lognet.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.lognet.springboot.grpc.health.ManagedHealthStatusService;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hosts embedded gRPC server.
 */
@Slf4j
public class GRpcServerRunner implements SmartLifecycle {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private Optional<ManagedHealthStatusService> healthStatusManager = Optional.empty();

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private GRpcServerProperties gRpcServerProperties;

    private final Consumer<ServerBuilder<?>> configurator;

    private Server server;

    private final ServerBuilder<?> serverBuilder;

    private CountDownLatch latch;

    public GRpcServerRunner(Consumer<ServerBuilder<?>> configurator, ServerBuilder<?> serverBuilder) {
        this.configurator = configurator;
        this.serverBuilder = serverBuilder;

    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        log.info("Starting gRPC Server ...");
        latch = new CountDownLatch(1);
        try {
            Collection<ServerInterceptor> globalInterceptors = getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class, ServerInterceptor.class)
                    .map(name -> applicationContext.getBeanFactory().getBean(name, ServerInterceptor.class))
                    .collect(Collectors.toList());


            // find and register all GRpcService-enabled beans
            List<String> serviceNames = new ArrayList<>();
            getBeanNamesByTypeWithAnnotation(GRpcService.class, BindableService.class)
                    .forEach(name -> {
                        BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
                        ServerServiceDefinition serviceDefinition = srv.bindService();
                        GRpcService gRpcServiceAnn = applicationContext.findAnnotationOnBean(name, GRpcService.class);
                        serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);
                        serverBuilder.addService(serviceDefinition);

                        if (srv instanceof HealthGrpc.HealthImplBase) {
                            if(!(srv instanceof ManagedHealthStatusService)){
                                throw new FatalBeanException(String.format("Please inherit %s from %s rather than directly from %s",
                                        srv.getClass().getName(),
                                        ManagedHealthStatusService.class.getName(),
                                        HealthGrpc.HealthImplBase.class.getName()
                                        ));
                            }
                            if(healthStatusManager.isPresent()){
                                throw new FatalBeanException(String.format("Only 1 single %s service instance is allowed",ManagedHealthStatusService.class.getName()));
                            }else {
                                healthStatusManager = Optional.of((ManagedHealthStatusService) srv);
                            }
                        } else {
                            serviceNames.add(serviceDefinition.getServiceDescriptor().getName());
                        }


                        log.info("'{}' service has been registered.", srv.getClass().getName());

                    });

            healthStatusManager.ifPresent(h ->
                    serviceNames.forEach(serviceName -> h.setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING))
            );

            if (gRpcServerProperties.isEnableReflection()) {
                serverBuilder.addService(ProtoReflectionService.newInstance());
                log.info("'{}' service has been registered.", ProtoReflectionService.class.getName());
            }

            configurator.accept(serverBuilder);
            server = serverBuilder.build().start();
            isRunning.set(true);
            startDaemonAwaitThread();
            log.info("gRPC Server started, listening on port {}.", server.getPort());

            applicationContext.publishEvent(new GRpcServerInitializedEvent(applicationContext, server));
        } catch (Exception e) {
            throw new RuntimeException("Failed to start GRPC server", e);
        }

    }

    private ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, GRpcService gRpcService, Collection<ServerInterceptor> globalInterceptors) {

        Stream<? extends ServerInterceptor> privateInterceptors = Stream.of(gRpcService.interceptors())
                .map(interceptorClass -> {
                    try {
                        return 0 < applicationContext.getBeanNamesForType(interceptorClass).length ?
                                applicationContext.getBean(interceptorClass) : interceptorClass.newInstance();
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
        return new AnnotationAwareOrderComparator()
                .withSourceProvider(o -> {
                    List<Object> sources = new ArrayList<>(2);
                    final Optional<RootBeanDefinition> rootBeanDefinition = Stream.of(applicationContext.getBeanNamesForType(o.getClass()))
                            .findFirst()
                            .map(name -> applicationContext.getBeanFactory().getBeanDefinition(name))
                            .filter(RootBeanDefinition.class::isInstance)
                            .map(RootBeanDefinition.class::cast);

                    rootBeanDefinition.map(RootBeanDefinition::getResolvedFactoryMethod)
                            .ifPresent(sources::add);

                    rootBeanDefinition.map(RootBeanDefinition::getTargetType)
                            .filter(t -> t != o.getClass())
                            .ifPresent(sources::add);

                    return sources.toArray();
                })
                .reversed();
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {

                latch.await();
            } catch (InterruptedException e) {
                log.error("gRPC server awaiter interrupted.", e);
            } finally {
                isRunning.set(false);
            }
        });
        awaitThread.setName("grpc-server-awaiter");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void stop() {
        Optional.ofNullable(server).ifPresent(s -> {
            log.info("Shutting down gRPC server ...");
            healthStatusManager.ifPresent(ManagedHealthStatusService::onShutdown);

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


    @Override
    public int getPhase() {
        return gRpcServerProperties.getStartUpPhase();
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }
}

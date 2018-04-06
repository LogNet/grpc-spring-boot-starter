package org.lognet.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.services.HealthStatusManager;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.core.type.MethodMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final GRpcServerBuilderConfigurer configurer;

    private final ServerBuilder<?> serverBuilder;

    private Server server;

    public GRpcServerRunner(GRpcServerBuilderConfigurer configurer, ServerBuilder<?> serverBuilder) {
        this.configurer = configurer;
        this.serverBuilder = serverBuilder;
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

        configurer.configure(serverBuilder);
        server = serverBuilder.build().start();
        applicationContext.publishEvent(new GRpcServerInitializedEvent(server));

        log.info("gRPC Server started, listening on port {}.", server.getPort());
        startDaemonAwaitThread();
    }


    private ServerServiceDefinition bindInterceptors(
            ServerServiceDefinition serviceDefinition,
            GRpcService gRpcService,
            Collection<ServerInterceptor> globalInterceptors
    ) {
        Stream<ServerInterceptor> privateInterceptors = Stream.of(gRpcService.interceptors())
                .map(this::getBeanOrCreateInterceptor);

        Stream<ServerInterceptor> unorderedInterceptors = Stream.concat(
                gRpcService.applyGlobalInterceptors() ? globalInterceptors.stream() : Stream.empty(),
                privateInterceptors
        );

        List<ServerInterceptor> orderedInterceptors = unorderedInterceptors
                .sorted(serverInterceptorOrderComparator())
                .collect(Collectors.toList());

        return ServerInterceptors.intercept(serviceDefinition, orderedInterceptors);
    }

    private ServerInterceptor getBeanOrCreateInterceptor(Class<? extends ServerInterceptor> interceptorClass) {
        try {
            return isBeanAvailable(interceptorClass)
                    ? applicationContext.getBean(interceptorClass)
                    : interceptorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new BeanCreationException("Failed to create interceptor instance.", e);
        }
    }

    private boolean isBeanAvailable(Class<? extends ServerInterceptor> interceptorClass) {
        return applicationContext.getBeanNamesForType(interceptorClass).length > 0;
    }

    private Comparator<Object> serverInterceptorOrderComparator() {
        Function<Object, Boolean> isOrderAnnotated = obj -> {
            Order ann = obj instanceof Method
                    ? AnnotationUtils.findAnnotation((Method) obj, Order.class)
                    : AnnotationUtils.findAnnotation(obj.getClass(), Order.class);
            return ann != null;
        };
        return AnnotationAwareOrderComparator.INSTANCE.thenComparing((o1, o2) -> {
            boolean p1 = isOrderAnnotated.apply(o1);
            boolean p2 = isOrderAnnotated.apply(o2);
            return p1 && !p2 ? -1 : p2 && !p1 ? 1 : 0;
        }).reversed();
    }


    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                GRpcServerRunner.this.server.awaitTermination();
            } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void destroy() {
        log.info("Shutting down gRPC server ...");
        server.getServices().forEach(def -> healthStatusManager.clearStatus(def.getServiceDescriptor().getName()));
        Optional.ofNullable(server).ifPresent(Server::shutdown);
        log.info("gRPC server stopped.");
    }

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) {
        return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name -> isGlobalInterceptor(name, annotationType));
    }

    private boolean isGlobalInterceptor(String name, Class<? extends Annotation> annotationType) {
        BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);

        boolean isSeparateClassBean = beansWithAnnotation.containsKey(name);
        if (isSeparateClassBean) {
            return true;
        } else if (beanDefinition.getSource() instanceof MethodMetadata) {
            MethodMetadata metadata = (MethodMetadata) beanDefinition.getSource();
            return metadata.isAnnotated(annotationType.getName());
        }

        return false;
    }

}

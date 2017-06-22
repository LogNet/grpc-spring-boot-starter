package org.lognet.springboot.grpc;

import com.google.common.base.Preconditions;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.type.StandardMethodMetadata;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Hosts embedded gRPC server.
 */
@Slf4j
public class GRpcServerRunner implements CommandLineRunner,DisposableBean  {

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private GRpcServerProperties gRpcServerProperties;

    private GRpcServerBuilderConfigurer configurer;

    private Server server;

    public GRpcServerRunner(GRpcServerBuilderConfigurer configurer) {
        this.configurer = configurer;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(gRpcServerProperties.getPort());
        Collection<ServerInterceptor> emptyInterceptors = Collections.emptyList();
        Collection<ServerInterceptor> globalInterceptors = getGlobalInterceptors();

        // find and register all GRpcService-enabled beans
        getBeanNamesByTypeWithAnnotation(GRpcService.class, BindableService.class)
                .forEach(name->{
                    BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = srv.bindService();
                    GRpcService gRpcServiceAnn = applicationContext.findAnnotationOnBean(name, GRpcService.class);

                    serviceDefinition  = bindInterceptors(
                            serviceDefinition,
                            concatInterceptors(
                                gRpcServiceAnn.applyGlobalInterceptors() ? globalInterceptors : emptyInterceptors,
                                configurer.getServerInterceptors(srv),
                                getStaticServiceInterceptors(gRpcServiceAnn),
                                getDynamicServiceInterceptors(srv)
                            )
                    );

                    serverBuilder.addService(serviceDefinition);
                    log.info("'{}' service has been registered.", srv.getClass().getName());
                });

        configurer.configure(serverBuilder);
        server = serverBuilder.build().start();
        log.info("gRPC Server started, listening on port {}.", gRpcServerProperties.getPort());
        startDaemonAwaitThread();
    }

    private  List<ServerInterceptor> concatInterceptors(Collection<ServerInterceptor> globalInterceptors, Collection<ServerInterceptor> serverInterceptors, Collection<ServerInterceptor> staticServiceInterceptors, Collection<ServerInterceptor> dynamicServiceInterceptors) {

        // Note the order that the interceptors are applied.
        // This run-time interceptor processing order is: global -> server -> static service -> dynamic service
        return Stream.concat(
                Stream.concat(
                        dynamicServiceInterceptors.stream(),
                        staticServiceInterceptors.stream()),
                Stream.concat(
                        serverInterceptors.stream(),
                        globalInterceptors.stream())
                )
                .distinct() // NOTE: This should be configurable, and probably default not-distinct
                .collect(Collectors.toList());
    }

    private  ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, List<? extends ServerInterceptor> interceptors) {
        Preconditions.checkNotNull(serviceDefinition, "serviceDefinition");
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
    }

    private Collection<ServerInterceptor> getGlobalInterceptors() throws Exception {
         return getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class, ServerInterceptor.class)
                .map(name -> applicationContext.getBeanFactory().getBean(name, ServerInterceptor.class))
                .collect(Collectors.toList());
    }

    private Collection<ServerInterceptor> getStaticServiceInterceptors(GRpcService gRpcService) {
        Stream<? extends ServerInterceptor> privateInterceptors = Stream.of(gRpcService.interceptors())
                .map(interceptorClass -> {
                    try {
                        return 0 < applicationContext.getBeanNamesForType(interceptorClass).length ?
                                applicationContext.getBean(interceptorClass) :
                                interceptorClass.newInstance();
                    } catch (Exception e) {
                        throw  new BeanCreationException("Failed to create interceptor instance.",e);
                    }
                });
        return privateInterceptors.collect(Collectors.toList());
    }

    private Collection<ServerInterceptor> getDynamicServiceInterceptors(BindableService srv) {
        if (srv instanceof GRpcServiceBuilder) {
            return ((GRpcServiceBuilder) srv).getServiceInterceptors();
        }

        return Collections.emptyList();
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

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) throws Exception{

       return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name->{
                    final BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);

                    if (!beansWithAnnotation.isEmpty()) {
                        if (beansWithAnnotation.containsKey(name)) {
                            return true;
                        }
                    }

                    if(beanDefinition.getSource() instanceof StandardMethodMetadata) {
                        StandardMethodMetadata metadata = (StandardMethodMetadata) beanDefinition.getSource();
                        return metadata.isAnnotated(annotationType.getName());
                    }

                    return false;
                });
    }
}

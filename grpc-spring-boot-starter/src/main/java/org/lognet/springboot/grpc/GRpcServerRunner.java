package org.lognet.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.type.StandardMethodMetadata;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    private Server server;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        Collection<ServerInterceptor> globalInterceptors = getTypedBeansWithAnnotation(GRpcGlobalInterceptor.class,ServerInterceptor.class);
        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(gRpcServerProperties.getPort());

        // find and register all GRpcService-enabled beans
        for (BindableService bindableService : getTypedBeansWithAnnotation(GRpcService.class, BindableService.class)) {

            ServerServiceDefinition serviceDefinition = bindableService.bindService();
            GRpcService gRpcServiceAnn;
            // if the service is a proxy, use its target class instead.
            if (AopUtils.isAopProxy(bindableService)) {
                gRpcServiceAnn = AopUtils.getTargetClass(bindableService).getAnnotation(GRpcService.class);
            } else {
                gRpcServiceAnn = bindableService.getClass().getAnnotation(GRpcService.class);
            }
            serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);
            serverBuilder.addService(serviceDefinition);
            log.info("'{}' service has been registered.", bindableService.getClass().getName());

        }

        server = serverBuilder.build().start();
        log.info("gRPC Server started, listening on port {}.", gRpcServerProperties.getPort());
        startDaemonAwaitThread();

    }

    private  ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, GRpcService gRpcService, Collection<ServerInterceptor> globalInterceptors) {


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

        List<ServerInterceptor> interceptors = Stream.concat(
                    gRpcService.applyGlobalInterceptors() ? globalInterceptors.stream(): Stream.empty(),
                    privateInterceptors)
                .distinct()
                .collect(Collectors.toList());
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
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

    private <T> Collection<T> getTypedBeansWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) throws Exception{


       return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name->{
                    BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
                    if( beanDefinition.getSource() instanceof StandardMethodMetadata) {
                        StandardMethodMetadata metadata = (StandardMethodMetadata) beanDefinition.getSource();
                        return metadata.isAnnotated(annotationType.getName());
                    }
                    return null!= applicationContext.getBeanFactory().findAnnotationOnBean(name,annotationType);
                })
               .map(name -> applicationContext.getBeanFactory().getBean(name,beanType))
               .collect(Collectors.toList());

    }


}

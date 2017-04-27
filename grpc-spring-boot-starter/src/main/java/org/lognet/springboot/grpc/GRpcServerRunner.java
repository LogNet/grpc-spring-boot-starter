package org.lognet.springboot.grpc;

import io.grpc.*;
import io.grpc.inprocess.InProcessServerBuilder;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
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


    private GRpcServerBuilderConfigurer configurer;

    private Server rpcServer;

    private Server inProcessServer;

    public GRpcServerRunner(GRpcServerBuilderConfigurer configurer) {
        this.configurer = configurer;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting gRPC Server ...");

        Collection<ServerInterceptor> globalInterceptors = getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class,ServerInterceptor.class)
                .map(name -> applicationContext.getBeanFactory().getBean(name,ServerInterceptor.class))
                .collect(Collectors.toList());

        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(gRpcServerProperties.getPort());
        final InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(gRpcServerProperties.getInProcessServerName());

        // find and register all GRpcService-enabled beans
        getBeanNamesByTypeWithAnnotation(GRpcService.class,BindableService.class)
                .forEach(name->{
                    GRpcService gRpcServiceAnn = applicationContext.findAnnotationOnBean(name,GRpcService.class);
                    BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = srv.bindService();
                    serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);

                    if ( gRpcServiceAnn.exposeRPC() ) {
                        serverBuilder.addService(serviceDefinition);
                        log.info("'{}' service has been registered for RPC.", srv.getClass().getName());
                    }

                    if ( gRpcServiceAnn.exposeInProcess() ) {
                        inProcessServerBuilder.addService(serviceDefinition);

                        log.info("'{}' service has been registered for in-process.", srv.getClass().getName());
                    }

                });

        configurer.configure(serverBuilder);
        rpcServer = serverBuilder.build().start();
        log.info("gRPC Server started, listening on port {}.", gRpcServerProperties.getPort());

        configurer.configureInProcessServerBuilder(inProcessServerBuilder);
        inProcessServer = inProcessServerBuilder.build().start();
        log.info("gRPC In-Process Server started, name {}.", gRpcServerProperties.getInProcessServerName());

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
        Thread awaitThreadRpc = new Thread() {
            @Override
            public void run() {
                try {
                    GRpcServerRunner.this.rpcServer.awaitTermination();
                } catch (InterruptedException e) {
                    log.error("gRPC rpcServer stopped.",e);
                }
            }

        };
        awaitThreadRpc.setDaemon(false);
        awaitThreadRpc.start();

        Thread awaitThreadInProcess = new Thread() {
            @Override
            public void run() {
                try {
                    GRpcServerRunner.this.inProcessServer.awaitTermination();
                } catch (InterruptedException e) {
                    log.error("gRPC inProcessServer stopped.",e);
                }
            }

        };
        awaitThreadInProcess.setDaemon(false);
        awaitThreadInProcess.start();
    }

    @Override
    public void destroy() throws Exception {
        log.info("Shutting down gRPC rpcServer ...");
        Optional.ofNullable(rpcServer).ifPresent(Server::shutdown);
        log.info("gRPC rpcServer stopped.");

        log.info("Shutting down gRPC inProcessServer ...");
        Optional.ofNullable(inProcessServer).ifPresent(Server::shutdown);
        log.info("gRPC inProcessServer stopped.");
    }

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) throws Exception{

       return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name->{
                    final BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);

                    if ( !beansWithAnnotation.isEmpty() ) {
                        return beansWithAnnotation.containsKey(name);
                    } else if( beanDefinition.getSource() instanceof StandardMethodMetadata) {
                        StandardMethodMetadata metadata = (StandardMethodMetadata) beanDefinition.getSource();
                        return metadata.isAnnotated(annotationType.getName());
                    }

                    return false;
                });
    }


}

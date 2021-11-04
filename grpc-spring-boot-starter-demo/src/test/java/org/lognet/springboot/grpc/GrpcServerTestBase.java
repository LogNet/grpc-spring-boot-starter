package org.lognet.springboot.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(
        initializers = GrpcServerTestBase.TessAppContextInitializer.class)
public abstract class GrpcServerTestBase {

    static class TessAppContextInitializer implements
            ApplicationContextInitializer<GenericApplicationContext> {

        @Override
        public void initialize(GenericApplicationContext applicationContext) {
            applicationContext.setAllowCircularReferences(false);
        }
    }

    @Autowired(required = false)
    @Qualifier("grpcServerRunner")
    protected GRpcServerRunner grpcServerRunner;

    @Autowired(required = false)
    @Qualifier("grpcInprocessServerRunner")
    protected GRpcServerRunner grpcInprocessServerRunner;

    protected ManagedChannel channel;
    protected ManagedChannel inProcChannel;
    protected Channel selectedChanel;

    @LocalRunningGrpcPort
    protected  int runningPort;

    @Autowired
    protected ApplicationContext context;

    @Autowired
    protected GRpcServerProperties gRpcServerProperties;

    protected String name="John";

    @Before
    public   void setupChannels() throws IOException {
        if(gRpcServerProperties.isEnabled()) {
            ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress("localhost", getPort());
            Resource certChain = Optional.ofNullable(gRpcServerProperties.getSecurity())
                    .map(GRpcServerProperties.SecurityProperties::getCertChain)
                    .orElse(null);
            if(null!= certChain){
                ((NettyChannelBuilder)channelBuilder)
                        .useTransportSecurity()
                        .sslContext(GrpcSslContexts.forClient().trustManager(certChain.getInputStream()).build());
            }else{
                channelBuilder.usePlaintext();
            }


            channel = onChannelBuild(channelBuilder).build();
        }
        if(StringUtils.hasText(gRpcServerProperties.getInProcessServerName())){
            inProcChannel = onChannelBuild(
                                InProcessChannelBuilder.forName(gRpcServerProperties.getInProcessServerName())
                                .usePlaintext()
                            ).build();

        }
        selectedChanel = getChannel();
    }

    protected  Channel getChannel(){
       return Optional.ofNullable(channel).orElse(inProcChannel);
    }
    protected int getPort(){
        return runningPort;
    }

    protected ManagedChannelBuilder<?>  onChannelBuild(ManagedChannelBuilder<?> channelBuilder){
        return  channelBuilder;
    }

    protected InProcessChannelBuilder onChannelBuild(InProcessChannelBuilder channelBuilder){
        return  channelBuilder;
    }

    @After
    public void shutdownChannels() {
        Optional.ofNullable(channel).ifPresent(ManagedChannel::shutdownNow);
        Optional.ofNullable(inProcChannel).ifPresent(ManagedChannel::shutdownNow);
    }

    @Test
    public void simpleGreeting() throws  Exception {



        final GreeterGrpc.GreeterFutureStub greeterFutureStub = GreeterGrpc.newFutureStub(selectedChanel);
        final GreeterOuterClass.HelloRequest helloRequest =GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        final String reply = beforeGreeting(greeterFutureStub).sayHello(helloRequest).get().getMessage();
        assertNotNull("Reply should not be null",reply);
        assertTrue(String.format("Reply should contain name '%s'",name),reply.contains(name));
        afterGreeting();

    }

    protected GreeterGrpc.GreeterFutureStub beforeGreeting(GreeterGrpc.GreeterFutureStub stub) {
        return  stub;
    }

    protected void afterGreeting() throws Exception {

    }
}

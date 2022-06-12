package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthGrpc;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ServiceRegistrationMode implements ServiceRegistrationStrategy {
    NOOP{
        @Override
        public Collection<NewService> createServices(Server grpcServer, ApplicationContext applicationContext) {
            return Collections.emptyList();
        }
    },
    SINGLE_SERVER_WITH_GLOBAL_CHECK {
        @Override
        public Collection<NewService> createServices(Server grpcServer, ApplicationContext applicationContext) {
            GRpcServerProperties gRpcServerProperties = applicationContext.getBean(GRpcServerProperties.class);
            ConsulDiscoveryProperties consulProperties = applicationContext.getBean(GrpcConsulProperties.class).getDiscovery();



            NewService grpcService = new NewService();
            grpcService.setPort(grpcServer.getPort());
            if (!consulProperties.isPreferAgentAddress()) {
                grpcService.setAddress(consulProperties.getHostname());
            }
            String appName = "grpc-" + ConsulAutoRegistration.getAppName(consulProperties, applicationContext.getEnvironment());
            grpcService.setName(ConsulAutoRegistration.normalizeForDns(appName));
            grpcService.setId("grpc-" + ConsulAutoRegistration.getInstanceId(consulProperties, applicationContext));
            grpcService.setTags(consulProperties.getTags());
            grpcService.setMeta(getMetadata(consulProperties,gRpcServerProperties));
            grpcService.setEnableTagOverride(consulProperties.getEnableTagOverride());
            if (consulProperties.isRegisterHealthCheck()) {

                final NewService.Check healthCheck = new NewService.Check();
                healthCheck.setGrpc(consulProperties.getHostname() + ":" + grpcServer.getPort());
                healthCheck.setGrpcUseTLS(Objects.nonNull(gRpcServerProperties.getSecurity()));
                healthCheck.setInterval(consulProperties.getHealthCheckInterval());
                healthCheck.setTimeout(consulProperties.getHealthCheckTimeout());
                healthCheck.setDeregisterCriticalServiceAfter(consulProperties.getHealthCheckCriticalTimeout());

                grpcService.setCheck(healthCheck);
            }


            return Collections.singleton(grpcService);
        }

        private Map<String, String> getMetadata(ConsulDiscoveryProperties properties,GRpcServerProperties gRpcServerProperties) {
            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
            if (!CollectionUtils.isEmpty(properties.getMetadata())) {
                metadata.putAll(properties.getMetadata());
            }

            if (StringUtils.hasText(properties.getInstanceZone())) {
                metadata.put(properties.getDefaultZoneMetadataName(), properties.getInstanceZone());
            }
            if (StringUtils.hasText(properties.getInstanceGroup())) {
                metadata.put("group", properties.getInstanceGroup());
            }

            metadata.put("secure", Boolean.toString(null!=gRpcServerProperties.getSecurity()));

            return metadata;
        }
    },
    SINGLE_SERVER_WITH_CHECK_PER_SERVICE {
        @Override
        public Collection<NewService> createServices(Server grpcServer, ApplicationContext applicationContext) {
            final NewService grpcService = SINGLE_SERVER_WITH_GLOBAL_CHECK.createServices(grpcServer, applicationContext).iterator().next();
            final NewService.Check prototypeCheck = grpcService.getCheck();


            if (null != prototypeCheck) {
                grpcService.setCheck(null);
                grpcService.setChecks(
                        grpcServer.getServices()
                                .stream()
                                .filter(d -> !d.getServiceDescriptor().equals(HealthGrpc.getServiceDescriptor()))
                                .map(d -> {
                                            final NewService.Check check = clone(prototypeCheck, NewService.Check.class);
                                            //append "/serviceName" to grpc address
                                            check.setGrpc(String.format("%s/%s", check.getGrpc(), d.getServiceDescriptor().getName()));
                                            return check;
                                        }
                                )
                                .collect(Collectors.toList())
                );
            }

            return Collections.singleton(grpcService);
        }
    },
    STANDALONE_SERVICES {
        @Override
        public Collection<NewService> createServices(Server grpcServer, ApplicationContext applicationContext) {
            final NewService grpcServicePrototype = SINGLE_SERVER_WITH_GLOBAL_CHECK.createServices(grpcServer, applicationContext).iterator().next();

            List<NewService> newServices = new ArrayList<>();
            final List<ServerServiceDefinition> services = grpcServer.getServices();
            for (int i = 0; i< services.size(); ++i) {
                final ServerServiceDefinition d = services.get(i);
                if (d.getServiceDescriptor().equals(HealthGrpc.getServiceDescriptor())) {
                    continue;
                }
                final NewService service = clone(grpcServicePrototype, NewService.class);
                service.setId(String.format("%s-%d",service.getId(),i));
                service.getTags().add(d.getServiceDescriptor().getName());
                Optional.ofNullable(service.getCheck())
                                .ifPresent(check->
                                    check.setGrpc(String.format("%s/%s", check.getGrpc(), d.getServiceDescriptor().getName()))
                                );

                newServices.add(service);
            }
            return newServices;


        }
    }
}

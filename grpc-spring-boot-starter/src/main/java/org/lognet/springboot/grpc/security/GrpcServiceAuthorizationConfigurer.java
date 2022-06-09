package org.lognet.springboot.grpc.security;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrpcServiceAuthorizationConfigurer
        extends SecurityConfigurerAdapter<ServerInterceptor, GrpcSecurity> {

    private final GrpcServiceAuthorizationConfigurer.Registry registry;

    public GrpcServiceAuthorizationConfigurer(GRpcServicesRegistry registry) {
        this.registry = new GrpcServiceAuthorizationConfigurer.Registry(registry);
    }

    public Registry getRegistry() {
        return registry;
    }

    @Override
    public void configure(GrpcSecurity builder) throws Exception {
        registry.processSecuredAnnotation();
        builder.setSharedObject(GrpcSecurityMetadataSource.class, new GrpcSecurityMetadataSource(registry.servicesRegistry,registry.securedMethods));
    }


    public class AuthorizedMethod {
        private List<MethodDescriptor<?, ?>> methods;

        private AuthorizedMethod(MethodDescriptor<?, ?>... methodDescriptor) {
            methods = Arrays.asList(methodDescriptor);
        }

        private AuthorizedMethod(ServiceDescriptor... serviceDescriptor) {
            methods = Stream.of(serviceDescriptor)
                    .flatMap(s -> s.getMethods().stream())
                    .collect(Collectors.toList());
        }

        public GrpcServiceAuthorizationConfigurer.Registry authenticated() {
            GrpcServiceAuthorizationConfigurer.this.registry.map(methods);
            return GrpcServiceAuthorizationConfigurer.this.registry;
        }

        public GrpcServiceAuthorizationConfigurer.Registry hasAnyRole(String... roles) {
            String rolePrefix = "ROLE_";
            for (String role : roles) {
                if (role.startsWith(rolePrefix)) {
                    throw new IllegalArgumentException(
                            "role should not start with 'ROLE_' since it is automatically inserted. Got '"
                                    + role + "'");
                }
            }
            return hasAnyAuthority(Arrays.stream(roles).map(rolePrefix::concat).toArray(String[]::new));
        }

        public GrpcServiceAuthorizationConfigurer.Registry hasAnyAuthority(String... authorities) {
            for (String auth : authorities) {
                GrpcServiceAuthorizationConfigurer.this.registry.map(auth, methods);
            }
            return GrpcServiceAuthorizationConfigurer.this.registry;
        }


    }

    public class Registry {

        private MultiValueMap<MethodDescriptor<?, ?>, ConfigAttribute> securedMethods = new LinkedMultiValueMap<>();
        GRpcServicesRegistry servicesRegistry;
        private boolean withSecuredAnnotation = true;

        Registry(GRpcServicesRegistry servicesRegistry) {
            this.servicesRegistry = servicesRegistry;
        }

        public GrpcSecurity withoutSecuredAnnotation() {
            return withSecuredAnnotation(false);
        }

        public AuthorizedMethod anyMethod() {
            return  anyMethodExcluding(s->false);
        }

        public AuthorizedMethod anyMethodExcluding(MethodDescriptor<?, ?>... methodDescriptor) {
            List<MethodDescriptor<?,?>> excludedMethods = Arrays.asList(methodDescriptor);
            return anyMethodExcluding(excludedMethods::contains);

        }


        public AuthorizedMethod anyMethodExcluding(Predicate<MethodDescriptor<?, ?>> excludePredicate) {
            MethodDescriptor<?,?>[] allMethods = servicesRegistry.getBeanNameToServiceBeanMap()
                    .values()
                    .stream()
                    .map(BindableService::bindService)
                    .map(ServerServiceDefinition::getServiceDescriptor)
                    .map(ServiceDescriptor::getMethods)
                    .flatMap(Collection::stream)
                    .filter(excludePredicate.negate())
                    .toArray(MethodDescriptor[]::new);
            return new AuthorizedMethod(allMethods);
        }


        public AuthorizedMethod anyService() {
            return anyServiceExcluding(s-> false);
        }
        public AuthorizedMethod anyServiceExcluding(ServiceDescriptor... serviceDescriptor) {
            List<ServiceDescriptor> excludedServices = Arrays.asList(serviceDescriptor);
            return anyServiceExcluding(excludedServices::contains);
        }
        public AuthorizedMethod anyServiceExcluding(Predicate<ServiceDescriptor> excludePredicate) {

            ServiceDescriptor[] allServices = servicesRegistry.getBeanNameToServiceBeanMap()
                    .values()
                    .stream()
                    .map(BindableService::bindService)
                    .map(ServerServiceDefinition::getServiceDescriptor)
                    .filter(excludePredicate.negate())
                    .toArray(ServiceDescriptor[]::new);
            return new AuthorizedMethod(allServices);
        }

        /**
         * Same as  {@code withSecuredAnnotation(true)}
         * @return GrpcSecurity configuration
         */
        public GrpcSecurity withSecuredAnnotation() {
            return withSecuredAnnotation(true);
        }
        public GrpcSecurity withSecuredAnnotation(boolean withSecuredAnnotation) {
            this.withSecuredAnnotation = withSecuredAnnotation;
            return and();
        }

        private void processSecuredAnnotation() {
            if (withSecuredAnnotation) {
                final Collection<BindableService> services = servicesRegistry.getBeanNameToServiceBeanMap().values();

                for (BindableService service : services) {
                    final ServerServiceDefinition serverServiceDefinition = service.bindService();
                    // service level security
                    {
                        Optional.ofNullable(AnnotationUtils.findAnnotation(service.getClass(), Secured.class))
                        .ifPresent(secured -> {
                            if (secured.value().length == 0) {
                                new AuthorizedMethod(serverServiceDefinition.getServiceDescriptor()).authenticated();
                            } else {
                                new AuthorizedMethod(serverServiceDefinition.getServiceDescriptor()).hasAnyAuthority(secured.value());
                            }
                        });

                    }
                    // method level security
                    for (ServerMethodDefinition<?, ?> methodDefinition : serverServiceDefinition.getMethods()) {
                        Stream.of(service.getClass().getMethods()) // get method from methodDefinition
                                .filter(m ->   m.getName().equalsIgnoreCase(methodDefinition.getMethodDescriptor().getBareMethodName()))
                                .findFirst()
                                .flatMap(m -> Optional.ofNullable(AnnotationUtils.findAnnotation(m, Secured.class)))
                                .ifPresent(secured -> {
                                    if (secured.value().length == 0) {
                                        new AuthorizedMethod(methodDefinition.getMethodDescriptor()).authenticated();
                                    } else {
                                        new AuthorizedMethod(methodDefinition.getMethodDescriptor()).hasAnyAuthority(secured.value());
                                    }
                                });

                    }
                }
            }

        }

        public AuthorizedMethod methods(MethodDescriptor<?, ?>... methodDescriptor) {
            return new AuthorizedMethod(methodDescriptor);
        }

        public AuthorizedMethod services(ServiceDescriptor... serviceDescriptor) {
            return new AuthorizedMethod(serviceDescriptor);
        }

        void map(List<MethodDescriptor<?, ?>> methods) {
            methods.forEach(m -> securedMethods.addAll(m, Collections.singletonList(new AuthenticatedConfigAttribute())));

        }

        void map(String attribute, List<MethodDescriptor<?, ?>> methods) {
            methods.forEach(m -> securedMethods.addAll(m, SecurityConfig.createList(attribute)));

        }

        public GrpcSecurity and() {
            return GrpcServiceAuthorizationConfigurer.this.and();
        }
    }
}

package org.lognet.springboot.grpc.security;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import org.springframework.context.ApplicationContext;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrpcServiceAuthorizationConfigurer
        extends SecurityConfigurerAdapter<ServerInterceptor, GrpcSecurity> {

    private final GrpcServiceAuthorizationConfigurer.Registry REGISTRY;

    public GrpcServiceAuthorizationConfigurer(ApplicationContext context) {
        this.REGISTRY = new GrpcServiceAuthorizationConfigurer.Registry(context);
    }

    public Registry getRegistry() {
        return REGISTRY;
    }

    @Override
    public void configure(GrpcSecurity builder) throws Exception {
        builder.setSharedObject(GrpcSecurityMetadataSource.class, new GrpcSecurityMetadataSource(REGISTRY.securedMethods));
    }


    public class AuthorizedMethod {
        private List<MethodDescriptor<?, ?>> methods;

        //    public void setServices(Collection<BindableService> services){
//
//        for (BindableService s : services) {
//            final Secured securedAnn = AnnotationUtils.findAnnotation(s.getClass(), Secured.class);
//            final ServerServiceDefinition serverServiceDefinition = s.bindService();
//            if (null != securedAnn) {
//                serverServiceDefinition.getMethods().forEach(m -> securedMethods.compute(m.getMethodDescriptor(), (k, v) -> {
//                            Set<String> roles = new HashSet<>(Arrays.asList(securedAnn.value()));
//                            if (null != v) {
//                                roles.addAll(v);
//                            }
//                            return roles;
//
//                        }
//                ));
//            }
//
//            serverServiceDefinition.getMethods().forEach(methodDefinition -> {
//                final Optional<Method> method = Stream.of(s.getClass().getMethods())
//
//                        .filter(m -> {
//                            final String methodName = methodDefinition.getMethodDescriptor().getFullMethodName().substring(methodDefinition.getMethodDescriptor().getServiceName().length()+1);
//                            return methodName.equalsIgnoreCase(m.getName());
//                        })
//                        .findFirst();
//
//                method.ifPresent(m -> {
//                    final Secured securedMethodAnn = AnnotationUtils.findAnnotation(m, Secured.class);
//                    if (null != securedMethodAnn) {
//                        securedMethods.compute(methodDefinition.getMethodDescriptor(), (k, v) ->
//                                new HashSet<>(Arrays.asList(securedMethodAnn.value()))
//                        );
//                    }
//                });
//
//            });
//        }
//
//    }

        private AuthorizedMethod(MethodDescriptor<?, ?>... methodDescriptor) {
            methods = Arrays.asList(methodDescriptor);
        }

        private AuthorizedMethod(ServiceDescriptor... serviceDescriptor) {
            methods = Stream.of(serviceDescriptor)
                    .flatMap(s -> s.getMethods().stream())
                    .collect(Collectors.toList());
        }

        public GrpcServiceAuthorizationConfigurer.Registry authenticated() {
            GrpcServiceAuthorizationConfigurer.this.REGISTRY.map(methods);
            return GrpcServiceAuthorizationConfigurer.this.REGISTRY;
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
                GrpcServiceAuthorizationConfigurer.this.REGISTRY.map(auth, methods);
            }
            return GrpcServiceAuthorizationConfigurer.this.REGISTRY;
        }


    }

    public class Registry {

        private MultiValueMap<MethodDescriptor<?, ?>, ConfigAttribute> securedMethods = new LinkedMultiValueMap<>();
        private ApplicationContext context;

        Registry(ApplicationContext context) {
            this.context = context;
        }

        public AuthorizedMethod anyMethod() {
            ServiceDescriptor[] allServices = context.getBeansOfType(BindableService.class)
                    .values()
                    .stream()
                    .map(BindableService::bindService)
                    .map(ServerServiceDefinition::getServiceDescriptor)
                    .toArray(ServiceDescriptor[]::new);
            return new AuthorizedMethod(allServices);

        }

        public GrpcSecurity withSecuredAnnotation() {
            final Collection<BindableService> services = context.getBeansOfType(BindableService.class).values();

            for (BindableService service : services) {
                final ServerServiceDefinition serverServiceDefinition = service.bindService();
                // service level security
                {
                    final Secured securedAnn = AnnotationUtils.findAnnotation(service.getClass(), Secured.class);

                    if (null != securedAnn) {
                        new AuthorizedMethod(serverServiceDefinition.getServiceDescriptor()).hasAnyAuthority(securedAnn.value());
                    }
                }
                // method level security
                serverServiceDefinition.getMethods()
                        .stream()
                        .map(methodDefinition -> Stream.of(service.getClass().getMethods()) // get method from methodDefinition
                                .filter(m -> {
                                    final String methodName = methodDefinition.getMethodDescriptor().getFullMethodName().substring(methodDefinition.getMethodDescriptor().getServiceName().length() + 1);
                                    return methodName.equalsIgnoreCase(m.getName());
                                })
                                .findFirst()
                        )
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(m ->Optional.ofNullable(AnnotationUtils.findAnnotation(m, Secured.class)))
                        .filter(Optional::isPresent)
                        .forEach(secured ->
                                new AuthorizedMethod(serverServiceDefinition.getServiceDescriptor())
                                        .hasAnyAuthority(secured.get().value())

                        );
            }
            return and();
        }

        public AuthorizedMethod methods(MethodDescriptor<?, ?>... methodDescriptor) {
            return new AuthorizedMethod(methodDescriptor);
        }

        public AuthorizedMethod services(ServiceDescriptor... serviceDescriptor) {
            return new AuthorizedMethod(serviceDescriptor);
        }

        void map(List<MethodDescriptor<?, ?>> methods) {
            methods.forEach(m -> securedMethods.addAll(m, Collections.<ConfigAttribute>emptyList()));

        }

        void map(String attribute, List<MethodDescriptor<?, ?>> methods) {
            methods.forEach(m -> securedMethods.addAll(m, SecurityConfig.createList(attribute)));

        }

        public GrpcSecurity and() {
            return GrpcServiceAuthorizationConfigurer.this.and();
        }
    }
}

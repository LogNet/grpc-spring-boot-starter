package org.lognet.springboot.grpc.security;

import io.grpc.*;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
    public void configure(GrpcSecurity builder) {
        registry.processSecuredAnnotation();
        builder.setSharedObject(GrpcSecurityMetadataSource.class,
                new GrpcSecurityMetadataSource(registry.servicesRegistry, registry.methodManagers));
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
            GrpcServiceAuthorizationConfigurer.this.registry.mapAuthenticated(methods);
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
            GrpcServiceAuthorizationConfigurer.this.registry.mapAuthorities(authorities, methods);
            return GrpcServiceAuthorizationConfigurer.this.registry;
        }


    }

    public class Registry {

        /**
         * Map from gRPC MethodDescriptor to the combined AuthorizationManager for that method.
         * Multiple rules for the same method are combined with anyOf (same semantics as the
         * old AffirmativeBased: grant if any voter grants).
         */
        private Map<MethodDescriptor<?, ?>, List<AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>>>> methodManagerLists
                = new LinkedHashMap<>();

        /**
         * Flattened/combined view built lazily in {@code configure()} via
         * {@link #buildMethodManagers()}.
         */
        Map<MethodDescriptor<?, ?>, AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>>> methodManagers
                = new LinkedHashMap<>();

        GRpcServicesRegistry servicesRegistry;
        private boolean withSecuredAnnotation = true;

        Registry(GRpcServicesRegistry servicesRegistry) {
            this.servicesRegistry = servicesRegistry;
        }

        public GrpcSecurity withoutSecuredAnnotation() {
            return withSecuredAnnotation(false);
        }

        public AuthorizedMethod anyMethod() {
            return anyMethodExcluding(s -> false);
        }

        public AuthorizedMethod anyMethodExcluding(MethodDescriptor<?, ?>... methodDescriptor) {
            List<MethodDescriptor<?, ?>> excludedMethods = Arrays.asList(methodDescriptor);
            return anyMethodExcluding(excludedMethods::contains);

        }


        public AuthorizedMethod anyMethodExcluding(Predicate<MethodDescriptor<?, ?>> excludePredicate) {
            MethodDescriptor<?, ?>[] allMethods = servicesRegistry.getBeanNameToServiceBeanMap()
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
            return anyServiceExcluding(s -> false);
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
         *
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

                        List<Secured> secureds = Stream.of(service.getClass().getMethods()) // get method from methodDefinition
                                .filter(m -> m.getName().equalsIgnoreCase(methodDefinition.getMethodDescriptor().getBareMethodName()))
                                .map(m -> AnnotationUtils.findAnnotation(m, Secured.class))
                                .filter(Objects::nonNull)
                                .toList();
                        if (secureds.isEmpty()) {
                            continue;
                        }
                        if (1 == secureds.size()) {
                            Secured secured = secureds.get(0);
                            if (secured.value().length == 0) {
                                new AuthorizedMethod(methodDefinition.getMethodDescriptor()).authenticated();
                            } else {
                                new AuthorizedMethod(methodDefinition.getMethodDescriptor()).hasAnyAuthority(secured.value());
                            }
                        } else {
                            String errorMessage = String.format("Ambiguous 'Secured'  method '%s' in service '%s'." +
                                            "When securing reactive method,  the @Secured  annotation should be added to the method getting 'Mono<Request>' and not with pure 'Request' argument.",
                                    methodDefinition.getMethodDescriptor().getBareMethodName(),
                                    service.getClass().getName()
                            );
                            throw new BeanCreationException(errorMessage);
                        }


                    }
                }
            }

            buildMethodManagers();
        }

        public AuthorizedMethod methods(MethodDescriptor<?, ?>... methodDescriptor) {
            return new AuthorizedMethod(methodDescriptor);
        }

        public AuthorizedMethod services(ServiceDescriptor... serviceDescriptor) {
            return new AuthorizedMethod(serviceDescriptor);
        }

        void mapAuthenticated(List<MethodDescriptor<?, ?>> methods) {
            AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>> manager =
                    AuthenticatedAuthorizationManager.authenticated();
            methods.forEach(m -> methodManagerLists.computeIfAbsent(m, k -> new ArrayList<>()).add(manager));
        }

        void mapAuthorities(String[] authorities, List<MethodDescriptor<?, ?>> methods) {
            AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>> manager =
                    AuthorityAuthorizationManager.hasAnyAuthority(authorities);
            methods.forEach(m -> methodManagerLists.computeIfAbsent(m, k -> new ArrayList<>()).add(manager));
        }

        @SuppressWarnings("unchecked")
        private void buildMethodManagers() {
            for (Map.Entry<MethodDescriptor<?, ?>, List<AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>>>> entry
                    : methodManagerLists.entrySet()) {
                List<AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>>> managers = entry.getValue();
                AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>> combined;
                if (managers.size() == 1) {
                    combined = managers.get(0);
                } else {
                    combined = AuthorizationManagers.anyOf(managers.toArray(new AuthorizationManager[0]));
                }
                methodManagers.put(entry.getKey(), combined);
            }
        }

        public GrpcSecurity and() {
            return GrpcServiceAuthorizationConfigurer.this.getBuilder();
        }
    }
}

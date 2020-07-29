package org.lognet.springboot.grpc.security;

import io.grpc.*;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        builder.setSharedObject(GrpcSecurityMetadataSource.class,new GrpcSecurityMetadataSource(REGISTRY.securedMethods));
    }


    public class AuthorizedMethod{
        private List<MethodDescriptor<?,?>> methods;

        private AuthorizedMethod(ApplicationContext context){
            methods= context.getBeansOfType(BindableService.class)
                    .values()
                    .stream()
                    .map(BindableService::bindService)
                    .flatMap(d->d.getMethods().stream())
                    .map(ServerMethodDefinition::getMethodDescriptor)
                    .collect(Collectors.toList());
        }
        private AuthorizedMethod(MethodDescriptor<?,?> ...methodDescriptor){
            methods = Arrays.asList(methodDescriptor);
        }
        private AuthorizedMethod(ServiceDescriptor  ...serviceDescriptor){
            methods = Stream.of(serviceDescriptor)
                    .flatMap(s->s.getMethods().stream())
                    .collect(Collectors.toList());
        }

        public GrpcServiceAuthorizationConfigurer.Registry authenticated() {
            GrpcServiceAuthorizationConfigurer.this.REGISTRY.map(methods);
            return GrpcServiceAuthorizationConfigurer.this.REGISTRY;
        }
        public GrpcServiceAuthorizationConfigurer.Registry hasRole(String role) {
            GrpcServiceAuthorizationConfigurer.this.REGISTRY.map(role,methods);
            return GrpcServiceAuthorizationConfigurer.this.REGISTRY;
        }


    }

    public class Registry {

        private MultiValueMap<MethodDescriptor<?,?>, ConfigAttribute> securedMethods  = new LinkedMultiValueMap<>();
        private ApplicationContext context;

        Registry(ApplicationContext context) {

            this.context = context;
        }

        public AuthorizedMethod anyMethod() {
            return new AuthorizedMethod(context);
        }
        public AuthorizedMethod methods(MethodDescriptor<?,?> ...methodDescriptor) {
            return new AuthorizedMethod(methodDescriptor);
        }
        public AuthorizedMethod services(ServiceDescriptor ...serviceDescriptor) {
            return new AuthorizedMethod(serviceDescriptor);
        }

        void map(List<MethodDescriptor<?,?>> methods){
            methods.forEach(m->securedMethods.addAll(m,Collections.<ConfigAttribute>emptyList()));

        }
        void map(String attribute ,List<MethodDescriptor<?,?>> methods){
            methods.forEach(m->securedMethods.addAll(m,SecurityConfig.createList(attribute)));

        }

        public GrpcSecurity and() {
            return GrpcServiceAuthorizationConfigurer.this.and();
        }
    }
}

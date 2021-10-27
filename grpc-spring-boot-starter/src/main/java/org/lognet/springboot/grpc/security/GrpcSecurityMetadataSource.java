package org.lognet.springboot.grpc.security;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.method.MethodSecurityMetadataSource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrpcSecurityMetadataSource implements MethodSecurityMetadataSource {
    private Map<MethodDescriptor<?,?>, List<ConfigAttribute>> methodDescriptorMap;
    private Map<Method,MethodDescriptor<?,?>> methodMap = new HashMap<>();

    public GrpcSecurityMetadataSource(GRpcServicesRegistry registry , Map<MethodDescriptor<?, ?>, List<ConfigAttribute>> methodDescriptorMap) {
        this.methodDescriptorMap = methodDescriptorMap;

        for(BindableService s:registry.getBeanNameToServiceBeanMap().values()){
            for(ServerMethodDefinition<?,?> md :s.bindService().getMethods()){
                final Method method = Stream.of(s.getClass().getMethods())
                        .filter(m -> md.getMethodDescriptor().getBareMethodName().equalsIgnoreCase(m.getName()))
                        .findFirst().get();
                methodMap.put(method,md.getMethodDescriptor());
            }
        }
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        final MethodDescriptor methodDescriptor = SecurityInterceptor.GrpcMethodInvocation.class.cast(object).getCall().getMethodDescriptor();
        return methodDescriptorMap.get(methodDescriptor);
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return methodDescriptorMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return SecurityInterceptor.GrpcMethodInvocation.class.isAssignableFrom(clazz);
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Method method, Class<?> targetClass) {
        final MethodDescriptor<?, ?> methodDescriptor = methodMap.get(method);
        return methodDescriptorMap.get(methodDescriptor);
    }
}

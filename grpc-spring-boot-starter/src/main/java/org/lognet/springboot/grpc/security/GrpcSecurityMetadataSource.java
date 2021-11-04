package org.lognet.springboot.grpc.security;

import io.grpc.MethodDescriptor;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.method.MethodSecurityMetadataSource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrpcSecurityMetadataSource implements MethodSecurityMetadataSource {
    private Map<MethodDescriptor<?,?>, List<ConfigAttribute>> methodDescriptorAttributes;
    private GRpcServicesRegistry registry;


    public GrpcSecurityMetadataSource(GRpcServicesRegistry registry , Map<MethodDescriptor<?, ?>, List<ConfigAttribute>> methodDescriptorAttributes) {
        this.methodDescriptorAttributes = methodDescriptorAttributes;
        this.registry = registry;


    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        final MethodDescriptor methodDescriptor = SecurityInterceptor.GrpcMethodInvocation.class.cast(object).getCall().getMethodDescriptor();
        return methodDescriptorAttributes.get(methodDescriptor);
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return methodDescriptorAttributes
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
        final MethodDescriptor<?, ?> methodDescriptor = registry.getMethodDescriptor(method);
        return methodDescriptorAttributes.get(methodDescriptor);
    }
}

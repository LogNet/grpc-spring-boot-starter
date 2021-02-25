package org.lognet.springboot.grpc.security;

import io.grpc.MethodDescriptor;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrpcSecurityMetadataSource implements SecurityMetadataSource {
    private Map<MethodDescriptor<?,?>, List<ConfigAttribute>> methodsMap;
    private int interceptorPrecedence;

    public GrpcSecurityMetadataSource(Map<MethodDescriptor<?, ?>, List<ConfigAttribute>> methodsMap, int interceptorPrecedence) {
        this.methodsMap = methodsMap;
        this.interceptorPrecedence = interceptorPrecedence;
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        return methodsMap.get(object);
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return methodsMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return MethodDescriptor.class.isAssignableFrom(clazz);
    }

    public int getInterceptorPrecedence() {
        return interceptorPrecedence;
    }
}

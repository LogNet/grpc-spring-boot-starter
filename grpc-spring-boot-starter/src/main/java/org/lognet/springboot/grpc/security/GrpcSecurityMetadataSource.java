package org.lognet.springboot.grpc.security;

import io.grpc.MethodDescriptor;
import org.lognet.springboot.grpc.GRpcServicesRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Holds a mapping from gRPC {@link MethodDescriptor} to
 * {@link AuthorizationManager} so the {@link SecurityInterceptor} can look up
 * the gRPC-specific (metadata-driven) authorization rule for each incoming call.
 *
 * <p>The authorization manager stored here covers only the rules that were
 * registered programmatically via
 * {@link GrpcServiceAuthorizationConfigurer.Registry} (e.g. {@code anyMethod().authenticated()}).
 * Annotation-based rules ({@code @PreAuthorize}, {@code @Secured}, etc.) are
 * applied by separate managers in {@link SecurityInterceptor} and are NOT stored here.
 */
public class GrpcSecurityMetadataSource {

    private final Map<MethodDescriptor<?, ?>, AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>>> methodDescriptorManagers;
    private final GRpcServicesRegistry registry;

    public GrpcSecurityMetadataSource(
            GRpcServicesRegistry registry,
            Map<MethodDescriptor<?, ?>, AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>>> methodDescriptorManagers) {
        this.methodDescriptorManagers = methodDescriptorManagers;
        this.registry = registry;
    }

    /**
     * Returns the {@link AuthorizationManager} registered for the gRPC method
     * represented by the given {@link SecurityInterceptor.GrpcMethodInvocation}, or
     * {@code null} if no rule was registered for that method.
     */
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>> getAuthorizationManager(
            SecurityInterceptor.GrpcMethodInvocation<ReqT, RespT> invocation) {
        MethodDescriptor<?, ?> descriptor = invocation.getCall().getMethodDescriptor();
        return methodDescriptorManagers.get(descriptor);
    }

    /**
     * Returns the {@link AuthorizationManager} registered for the Java
     * {@link Method}, or {@code null} if no rule was registered for that method.
     * Used to support annotation scanning path.
     */
    public AuthorizationManager<SecurityInterceptor.GrpcMethodInvocation<?, ?>> getAuthorizationManager(Method method) {
        MethodDescriptor<?, ?> descriptor = registry.getMethodDescriptor(method);
        if (descriptor == null) {
            return null;
        }
        return methodDescriptorManagers.get(descriptor);
    }
}

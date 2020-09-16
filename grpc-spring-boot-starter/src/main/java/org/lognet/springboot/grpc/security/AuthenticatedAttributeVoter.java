package org.lognet.springboot.grpc.security;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import java.util.Collection;

public class AuthenticatedAttributeVoter implements AccessDecisionVoter<Object> {
    @Override
    public boolean supports(ConfigAttribute attribute) {
        return AuthenticatedConfigAttribute.class.isInstance(attribute);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return io.grpc.MethodDescriptor.class.equals(clazz);
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
        return (authentication.isAuthenticated() && attributes.stream().anyMatch(this::supports))? ACCESS_GRANTED:ACCESS_ABSTAIN;
    }
}

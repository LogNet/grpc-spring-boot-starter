package org.lognet.springboot.grpc.security;

import org.springframework.security.access.ConfigAttribute;

class AuthenticatedConfigAttribute implements ConfigAttribute {
    @Override
    public String getAttribute() {
        return null;
    }
}

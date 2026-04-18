package org.lognet.springboot.grpc.security;

/**
 * @deprecated No longer used. Retained for binary compatibility only.
 *             Was removed when migrating from Spring Security legacy access-control
 *             ({@code ConfigAttribute}/{@code AccessDecisionVoter}) to
 *             {@code AuthorizationManager}-based infrastructure in Spring Security 6+/7.
 *             Authentication-based checks are now performed by
 *             {@link org.springframework.security.authorization.AuthenticatedAuthorizationManager}.
 */
@Deprecated
public class AuthenticatedAttributeVoter {
}

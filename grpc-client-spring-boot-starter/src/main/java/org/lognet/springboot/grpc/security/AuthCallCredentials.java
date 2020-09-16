package org.lognet.springboot.grpc.security;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import lombok.Builder;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Adds Authorization header with configured configured authentication scheme token supplied by tokeSupplier
 */
@Builder
public class AuthCallCredentials extends CallCredentials {
    private final Supplier<String> tokenSupplier;
    private final String authScheme;

    public static class AuthCallCredentialsBuilder {

        public AuthCallCredentials.AuthCallCredentialsBuilder  bearer() {
            return authScheme(Constants.BEARER_AUTH_SCHEME);
        }
        public AuthCallCredentials.AuthCallCredentialsBuilder basic() {
            return  authScheme(Constants.BASIC_AUTH_SCHEME);
        }


    }


    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier metadataApplier) {

        appExecutor.execute(()->{
                try {
                    Metadata headers = new Metadata();
                    headers.put(Constants.AUTH_HEADER_KEY,String.format("%s %s",authScheme, tokenSupplier.get()));
                    metadataApplier.apply(headers);
                } catch (Throwable e) {
                    metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
                }
            }
        );



    }

    @Override
    public void thisUsesUnstableApi() {
        // noop
    }
}

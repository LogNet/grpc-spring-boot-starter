package org.lognet.springboot.grpc.security;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

/**
 * Adds Authorization header with configured configured authentication scheme token supplied by tokeSupplier
 */

public class AuthCallCredentials extends CallCredentials   {
    private AuthHeader authHeader;

    public AuthCallCredentials(AuthHeader.AuthHeaderBuilder authHeaderBuilder) {
        this(authHeaderBuilder.build());
    }
    public AuthCallCredentials(AuthHeader authHeader) {
        this.authHeader = authHeader;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier metadataApplier) {

        appExecutor.execute(()->{
                try {
                    metadataApplier.apply(authHeader.attach(new Metadata()));
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

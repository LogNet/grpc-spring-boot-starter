package org.lognet.springboot.grpc.security;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Adds Authorization header with Bearer token supplied by tokeSupplier
 */
public class BearerAuthCallCredentials extends CallCredentials {
    static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);

    private Supplier<String> tokenSupplier;

    public BearerAuthCallCredentials(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier metadataApplier) {

        appExecutor.execute(()->{
                try {
                    Metadata headers = new Metadata();
                    headers.put(AUTHORIZATION_METADATA_KEY,String.format("Bearer %s", tokenSupplier.get()));
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

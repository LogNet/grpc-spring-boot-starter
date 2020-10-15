package org.lognet.springboot.grpc.security;

import io.grpc.Metadata;
import lombok.Builder;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.function.Supplier;

@Builder
public class AuthHeader implements Constants {
    private final Supplier<ByteBuffer> tokenSupplier;
    private final String authScheme;

    public static class AuthHeaderBuilder {

        public AuthHeader.AuthHeaderBuilder  bearer() {
            return authScheme(Constants.BEARER_AUTH_SCHEME);
        }
        public AuthHeader.AuthHeaderBuilder basic() {
            return  authScheme(Constants.BASIC_AUTH_SCHEME);
        }

        public AuthHeader.AuthHeaderBuilder basic(String userName, byte[] password) {
            final ByteBuffer buffer = ByteBuffer.allocate(userName.length() + password.length + 1)
                    .put(userName.getBytes())
                    .put((byte) ':')
                    .put(password);
            buffer.rewind();
            ByteBuffer token = Base64.getEncoder().encode(buffer);
            return authScheme(Constants.BASIC_AUTH_SCHEME)
                    .tokenSupplier(() -> {
                        token.rewind();
                        return token;
                    });
        }


    }
    public Metadata attach(Metadata metadataHeader){
        ByteBuffer token = tokenSupplier.get();
        final byte[] header = ByteBuffer.allocate(authScheme.length() + token.remaining() + 1)
                .put(authScheme.getBytes())
                .put((byte) ' ')
                .put(token)
                .array();
        metadataHeader.put(Constants.AUTH_HEADER_KEY,header);
        return metadataHeader;
    }
}

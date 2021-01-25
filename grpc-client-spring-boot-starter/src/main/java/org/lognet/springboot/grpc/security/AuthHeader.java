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
    @Builder.Default
    private final boolean binaryFormat = true;

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
            token.rewind();
            return authScheme(Constants.BASIC_AUTH_SCHEME)
                    .tokenSupplier( token::duplicate);
        }


    }

    public Metadata attach(Metadata metadataHeader){
        ByteBuffer token = tokenSupplier.get();
        final byte[] header = ByteBuffer.allocate(authScheme.length() + token.remaining() + 1)
                .put(authScheme.getBytes())
                .put((byte)' ')
                .put(token)
                .array();
        if(binaryFormat) {
            metadataHeader.put(Constants.AUTH_HEADER_BIN_KEY, header);
        }else{
            metadataHeader.put(Constants.AUTH_HEADER_KEY, new String(header));
        }
        return metadataHeader;
    }
}

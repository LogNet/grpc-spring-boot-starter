package org.lognet.springboot.grpc.security;

import io.grpc.Metadata;


public  class Constants {
    public static final Metadata.Key<String> AUTH_HEADER_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final String BEARER_AUTH_SCHEME="Bearer";
    public static final String BASIC_AUTH_SCHEME="Basic";

    private Constants() {
    }
}

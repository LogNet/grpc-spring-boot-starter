package org.lognet.springboot.grpc.security;

import io.grpc.Metadata;


public  interface Constants {

    Metadata.Key<String> AUTH_HEADER_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<byte[]> AUTH_HEADER_BIN_KEY = Metadata.Key.of(AUTH_HEADER_KEY.name()+Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER);
    String BEARER_AUTH_SCHEME="Bearer";
    String BASIC_AUTH_SCHEME="Basic";



}

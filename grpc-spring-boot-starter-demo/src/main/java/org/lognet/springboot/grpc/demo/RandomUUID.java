package org.lognet.springboot.grpc.demo;

import java.util.UUID;

public class RandomUUID {
    private final String id = UUID.randomUUID().toString();

    public String getId() {
        return this.id;
    }
}

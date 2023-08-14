package org.lognet.springboot.grpc.recovery

import io.grpc.Status

fun throwException(status: Status) {
    throw status.asException()
}
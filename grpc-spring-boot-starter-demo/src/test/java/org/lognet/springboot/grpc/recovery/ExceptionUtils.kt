package org.lognet.springboot.grpc.recovery

import io.grpc.Status

fun throwException(status: Status) {
    throw status.asException()
}
fun throwException(ex: Exception) {
    throw ex
}
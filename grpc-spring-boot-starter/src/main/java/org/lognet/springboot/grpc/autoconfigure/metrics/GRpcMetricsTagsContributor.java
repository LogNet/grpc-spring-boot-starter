package org.lognet.springboot.grpc.autoconfigure.metrics;

import io.grpc.Attributes;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.micrometer.core.instrument.Tag;
@FunctionalInterface
public interface GRpcMetricsTagsContributor  {

    Iterable<Tag> getTags(Status status, MethodDescriptor<?,?> methodDescriptor, Attributes attributes);
}

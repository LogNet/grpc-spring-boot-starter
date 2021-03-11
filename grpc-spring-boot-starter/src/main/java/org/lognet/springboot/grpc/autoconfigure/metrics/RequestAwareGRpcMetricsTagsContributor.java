package org.lognet.springboot.grpc.autoconfigure.metrics;

import io.grpc.Attributes;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.micrometer.core.instrument.Tag;

import java.util.Collections;

public abstract class RequestAwareGRpcMetricsTagsContributor<ReqT> implements GRpcMetricsTagsContributor  {
    private Class<ReqT> tClass;

    protected RequestAwareGRpcMetricsTagsContributor(Class<ReqT> tClass) {
        this.tClass = tClass;
    }
    public final boolean accepts(Object o){
        return tClass.isInstance(o);
    }

    @Override
    public Iterable<Tag> getTags(Status status, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
        return Collections.emptyList();
    }

    public abstract Iterable<Tag> getTags(ReqT request,MethodDescriptor<?, ?> methodDescriptor, Attributes attributes);

}

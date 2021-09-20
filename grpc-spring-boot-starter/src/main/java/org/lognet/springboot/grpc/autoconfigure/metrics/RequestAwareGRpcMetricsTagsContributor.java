package org.lognet.springboot.grpc.autoconfigure.metrics;

import static java.util.Collections.emptyList;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.grpc.Attributes;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public abstract class RequestAwareGRpcMetricsTagsContributor<ReqT> implements GRpcMetricsTagsContributor  {
    private final Set<MethodDescriptor.MethodType> methodTypes;
    private final Class<ReqT> tClass;

    protected RequestAwareGRpcMetricsTagsContributor(Class<ReqT> tClass, Set<MethodDescriptor.MethodType> methodTypes) {
        this.methodTypes = methodTypes;
        this.tClass = tClass;
    }

    protected RequestAwareGRpcMetricsTagsContributor(
        Class<ReqT> tClass, MethodDescriptor.MethodType firstMethodType, MethodDescriptor.MethodType... otherMethodTypes
    ) {
        this(tClass, EnumSet.of(firstMethodType, otherMethodTypes));
    }

    protected RequestAwareGRpcMetricsTagsContributor(Class<ReqT> tClass){
        this(tClass, EnumSet.of(UNARY));
    }

    public final boolean mightAccept(MethodDescriptor<?, ?> methodDescriptor) {
        MethodDescriptor.Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
        boolean acceptableMethodType = methodTypes.contains(methodDescriptor.getType());
        if (!acceptableMethodType || !(requestMarshaller instanceof MethodDescriptor.ReflectableMarshaller<?>)) {
            return acceptableMethodType;
        } else {
            Class<?> requestClass = ((MethodDescriptor.ReflectableMarshaller<?>) requestMarshaller).getMessageClass();
            return requestClass.isAssignableFrom(tClass) || tClass.isAssignableFrom(requestClass);
        }
    }

    public final boolean accepts(Object o){
        return tClass.isInstance(o);
    }

    /**
     * Contribute tags at the end of the call, independent of the request message.
     */
    @Override
    public Iterable<Tag> getTags(Status status, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
        return Collections.emptyList();
    }

    /**
     * Contribute tags when receiving the request message.
     */
    public Iterable<Tag> addTags(ReqT request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes, Tags existingTags) {
        return Tags.concat(getTags(request, methodDescriptor, attributes));
    }

    /**
     * Prefer overriding {@link #addTags(Object, MethodDescriptor, Attributes, Tags)}.
     */
    @Deprecated
    public Iterable<Tag> getTags(ReqT request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
        return emptyList();
    }
}

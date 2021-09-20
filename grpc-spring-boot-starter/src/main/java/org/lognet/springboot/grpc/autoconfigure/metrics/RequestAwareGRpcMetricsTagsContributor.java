package org.lognet.springboot.grpc.autoconfigure.metrics;

import io.grpc.Attributes;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static io.grpc.MethodDescriptor.MethodType.UNARY;
import static java.util.Collections.emptyList;

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
     * For streaming calls, this method is invoked several times (once per message). The implementation should NOT concatenate existing tags with newly produced tags.
     * It's supposed to produce new tags or accumulate existing tag's value.
     * @param request current request message
     * @param methodDescriptor
     * @param attributes
     * @param existingTags currently accumulated tags
     * @return new tags
     */
    public Iterable<Tag> addTags(ReqT request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes, Tags existingTags) {
        return Tags.concat(getTags(request, methodDescriptor, attributes));
    }

    /**
     * Prefer overriding {@link #addTags(Object, MethodDescriptor, Attributes, Tags)} if you want to get a hold of already added tags(for streaming calls) and
     * accumulate tag's value.
     */
    @Deprecated
    public Iterable<Tag> getTags(ReqT request, MethodDescriptor<?, ?> methodDescriptor, Attributes attributes) {
        return emptyList();
    }
}

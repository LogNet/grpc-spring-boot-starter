package org.lognet.springboot.grpc.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class GrpcSpringBootExtension {
    private final Project project;
    private final Property<String> grpcVersion;
    private final Property<String> grpcSpringBootStarterVersion;
    private final Property<String> protocVersion;
    private final Property<String> reactiveProtocVersion;
    private final Property<ReactiveFeature> reactiveFeature;

    public GrpcSpringBootExtension(Project project) {
        this.project = project;
        grpcVersion = this.project.getObjects().property(String.class);
        grpcVersion.set(GrpcSpringBootPlugin.class.getPackage().getSpecificationVersion());

        grpcSpringBootStarterVersion = this.project.getObjects().property(String.class);
        grpcSpringBootStarterVersion.set(GrpcSpringBootPlugin.class.getPackage().getImplementationVersion());


        protocVersion = this.project.getObjects().property(String.class);
        protocVersion.set("3.25.5");

        reactiveProtocVersion = this.project.getObjects().property(String.class);
        reactiveProtocVersion.set("1.2.4");

        reactiveFeature = this.project.getObjects().property(ReactiveFeature.class);
        reactiveFeature.set(ReactiveFeature.OFF);


    }

    public Property<String> getGrpcVersion() {
        return grpcVersion;
    }

    public Property<String> getGrpcSpringBootStarterVersion() {
        return grpcSpringBootStarterVersion;
    }

    public Property<String> getProtocVersion() {
        return protocVersion;
    }

    public Property<String> getReactiveProtocVersion() {
        return reactiveProtocVersion;
    }

    public Property<ReactiveFeature> getReactiveFeature() {
        return reactiveFeature;
    }
}

package org.lognet.springboot.grpc.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class GrpcSpringBootPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

        project.getExtensions().create("grpcSpringBoot", GrpcSpringBootExtension.class, project);

        project.afterEvaluate(p-> {
            project.getPluginManager().withPlugin("java",javaPlugin->{
                project.apply(a->a.from(getClass().getResource("/grpc-spring-boot.gradle")));
            });
        });
    }
}

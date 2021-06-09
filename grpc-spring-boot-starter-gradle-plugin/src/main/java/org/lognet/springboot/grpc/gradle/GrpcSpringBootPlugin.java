package org.lognet.springboot.grpc.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

public class GrpcSpringBootPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("java",javaPlugin->{
            project.apply(a->a.from(getClass().getResource("/grpc-boot.gradle")));
        });
    }
}

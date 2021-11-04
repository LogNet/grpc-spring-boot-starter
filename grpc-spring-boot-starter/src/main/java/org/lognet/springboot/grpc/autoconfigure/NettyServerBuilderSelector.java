package org.lognet.springboot.grpc.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NettyServerBuilderSelector implements ImportSelector, EnvironmentAware {

    private Environment environment;

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        ClassLoader classLoader = getClass().getClassLoader();

        String pureNettyConfig = ClassUtils
                .isPresent("io.grpc.netty.NettyServerBuilder", classLoader)
                ? "org.lognet.springboot.grpc.autoconfigure.PureNettyConfiguration" : null;

        String shadedNettyConfig = ClassUtils
                .isPresent("io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder", classLoader) ?
                "org.lognet.springboot.grpc.autoconfigure.ShadedNettyConfiguration" : null;

        Set<String> imports = new LinkedHashSet<>();
        if (null != pureNettyConfig && null != shadedNettyConfig) {
            final Boolean preferShadedNetty = Binder.get(environment).bind(ConfigurationPropertyName.of("grpc.netty-server.on-collision-prefer-shaded-netty"), Bindable.of(Boolean.class))
                    .orElse(true);
            imports.add(preferShadedNetty ? shadedNettyConfig : pureNettyConfig);
        }else {
            final String cfg = Optional.ofNullable(shadedNettyConfig)
                    .orElse(Optional.ofNullable(pureNettyConfig).orElse(null));
            Optional.ofNullable(cfg)
                    .ifPresent(imports::add);
        }


        return StringUtils.toStringArray(imports);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}

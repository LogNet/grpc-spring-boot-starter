/*
 * Copyright 2016-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lognet.springboot.grpc.autoconfigure;

import org.lognet.springboot.grpc.AnnotationGrpcServiceDiscoverer;
import org.lognet.springboot.grpc.GrpcServerFactory;
import org.lognet.springboot.grpc.GrpcServerLifecycle;
import org.lognet.springboot.grpc.GrpcServerProperties;
import org.lognet.springboot.grpc.GrpcServiceDiscoverer;
import org.lognet.springboot.grpc.NettyGrpcServerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.Server;
import io.grpc.ServerServiceDefinition;

/**
 * Autoconfiguration for gRPC server.
 * @author Ray Tsang
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass({ Server.class, GrpcServerFactory.class })
public class GrpcServerAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public GrpcServerProperties defaultGrpcServerProperties() {
		return new GrpcServerProperties();
	}

	@ConditionalOnMissingBean
	@Bean
	public GrpcServiceDiscoverer defaultGrpcServiceFinder() {
		return new AnnotationGrpcServiceDiscoverer();
	}

	@ConditionalOnMissingBean
	@Bean
	public GrpcServerFactory defaultGrpcServiceFactory(
			GrpcServerProperties properties, GrpcServiceDiscoverer discoverer) {
		NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties);
		for (ServerServiceDefinition service : discoverer.findGrpcServices()) {
			factory.addService(service);
		}

		return factory;
	}

	@ConditionalOnMissingBean
	@Bean
	public GrpcServerLifecycle grpcServerLifecycle(
			GrpcServerFactory factory) {
		return new GrpcServerLifecycle(factory);
	}
}

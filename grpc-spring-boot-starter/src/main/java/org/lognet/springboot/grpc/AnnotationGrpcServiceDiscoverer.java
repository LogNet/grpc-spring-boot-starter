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

package org.lognet.springboot.grpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import lombok.extern.slf4j.Slf4j;

/**
 * Discovers gRPC service implementations by the {@link GrpcService} annotation.
 * @author Ray Tsang
 * @author Furer Alexander
 * @author Oliver Trosien
 */
@Slf4j
public class AnnotationGrpcServiceDiscoverer
		implements ApplicationContextAware, GrpcServiceDiscoverer {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Collection<ServerServiceDefinition> findGrpcServices() {
		Map<String,Object> beans = applicationContext.getBeansWithAnnotation(GrpcService.class);
		List<ServerServiceDefinition> definitions = new ArrayList<>(beans.size());

		for (Map.Entry<String, Object> bean : beans.entrySet()) {
			if (BindableService.class.isAssignableFrom(bean.getValue().getClass())) {
				BindableService bindableService = BindableService.class.cast(bean.getValue());
				ServerServiceDefinition serviceDefinition = bindableService.bindService();
				definitions.add(serviceDefinition);
				log.info("Found bean '{}' registring gRPC service '{}'.",  serviceDefinition.getServiceDescriptor().getName());
			}

		}
		return definitions;
	}

}

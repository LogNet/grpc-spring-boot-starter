package org.lognet.springboot.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

@RunWith(SpringRunner.class)
@SpringBootApplication
@SpringBootTest
public class GrpcServiceDiscovererTest {

	@Autowired
	AnnotationGrpcServiceDiscoverer grpcServiceDiscoverer;

	@Test
	public void should_find_grpc_service() {
		Collection<ServerServiceDefinition> services = grpcServiceDiscoverer.findGrpcServices();
		assertThat(services).hasSize(1);
		assertThat(services.iterator().next().getServiceDescriptor().getName()).isEqualTo("dummyService");
	}

	@GrpcService
	static class DummyService implements BindableService {

		@Override
		public ServerServiceDefinition bindService() {
			return ServerServiceDefinition.builder("dummyService").build();
		}
		
	}
}

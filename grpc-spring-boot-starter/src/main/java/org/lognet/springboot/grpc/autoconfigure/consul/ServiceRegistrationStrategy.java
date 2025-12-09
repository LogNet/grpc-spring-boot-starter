package org.lognet.springboot.grpc.autoconfigure.consul;

import com.ecwid.consul.json.GsonFactory;
import io.grpc.Server;
import org.springframework.cloud.consul.model.http.agent.NewService;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

public interface ServiceRegistrationStrategy {
    Collection<NewService> createServices(Server grpcServer, ApplicationContext applicationContext);

    default <T> T clone(T obj,Class<T> clazz){
        return GsonFactory.getGson().fromJson(GsonFactory.getGson().toJson(obj), clazz);
    }
}

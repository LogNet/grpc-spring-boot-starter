package org.lognet.springboot.grpc.autoconfigure;

import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.GRpcErrorHandler;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.lognet.springboot.grpc.recovery.ErrorHandlerAdapter;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandler;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.lognet.springboot.grpc.recovery.GRpcServiceAdvice;
import org.lognet.springboot.grpc.validation.ValidatingInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Optional;

@Configuration
@ConditionalOnClass({Validator.class})

@EnableConfigurationProperties(GRpcValidationProperties.class)
public class GRpcValidationConfiguration {



    @Bean
    @ConditionalOnBean(Validator.class)
    @GRpcGlobalInterceptor
    public ValidatingInterceptor validatingInterceptor(@Lazy Validator validator, GRpcValidationProperties validationProperties,@Lazy FailureHandlingSupport failureHandlingSupport){
        return  new ValidatingInterceptor(validator,failureHandlingSupport)
                .order(validationProperties.getInterceptorOrder());
    }

    @ConditionalOnMissingErrorHandler(ConstraintViolationException.class)
    @Configuration
    static  class DefaultValidationHandlerConfig{
        @GRpcServiceAdvice
        @Slf4j
        public static class DefaultValidationErrorHandler extends ErrorHandlerAdapter {

            public DefaultValidationErrorHandler(Optional<GRpcErrorHandler> errorHandler) {
               super(errorHandler);
            }

            @GRpcExceptionHandler
            public Status handle(ConstraintViolationException e, GRpcExceptionScope scope){
                return handle(e,scope.getHintAs(Status.class).get(),scope);
            }
        }

    }



}

package br.com.cinepass.reserva.infrastructure.http;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(io.micrometer.observation.ObservationRegistry registry) {
        return RestClient.builder().observationRegistry(registry).requestInterceptor((request,body,execution)->{
            String correlation=br.com.cinepass.reserva.observability.TraceSupport.safe(org.slf4j.MDC.get("correlationId"));
            if(!correlation.isEmpty()) request.getHeaders().set("X-Correlation-Id",correlation);
            return execution.execute(request,body);
        });
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(io.micrometer.observation.ObservationRegistry registry) {
        return RestClient.builder().observationRegistry(registry).requestInterceptor((request,body,execution)->{
            String correlation=br.com.cinepass.reserva.observability.TraceSupport.safe(org.slf4j.MDC.get("correlationId"));
            if(!correlation.isEmpty()) request.getHeaders().set("X-Correlation-Id",correlation);
            return execution.execute(request,body);
        });
    }
}

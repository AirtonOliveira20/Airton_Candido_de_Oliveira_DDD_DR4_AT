package br.com.cinepass.reserva.infrastructure.mensageria;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MensageriaConfig {

    
    @Bean
    NewTopic topicoReservas(@Value("${cinepass.kafka.topico-reservas}") String nome,
                            @Value("${cinepass.kafka.particoes:3}") int particoes) {
        return TopicBuilder.name(nome).partitions(particoes).replicas(1).build();
    }
}

package br.com.cinepass.historico.mensageria;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {





    @Bean
    NewTopic topicoReservas(@Value("${cinepass.kafka.topico-reservas}") String nome,
                            @Value("${cinepass.kafka.particoes:3}") int particoes) {
        return TopicBuilder.name(nome).partitions(particoes).replicas(1).build();
    }

    @Bean
    NewTopic topicoReservasDlt(@Value("${cinepass.kafka.topico-reservas}") String nome,
                               @Value("${cinepass.kafka.particoes:3}") int particoes) {
        return TopicBuilder.name(nome + ".DLT").partitions(particoes).replicas(1).build();
    }






    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        var handler = new DefaultErrorHandler(new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS));
        handler.setClassifications(java.util.Map.of(), true);
        handler.setLogLevel(org.springframework.kafka.KafkaException.Level.DEBUG);
        handler.setRetryListeners((record,ex,attempt)->org.slf4j.LoggerFactory.getLogger(KafkaConsumerConfig.class)
            .error("operacao=INBOX_RECEIVE resultado=FALHA particao={} offset={} tentativa={} erro={}",record.partition(),record.offset(),attempt,ex.getClass().getSimpleName()));
        handler.setAckAfterHandle(false);
        handler.setCommitRecovered(false);
        return handler;
    }
}

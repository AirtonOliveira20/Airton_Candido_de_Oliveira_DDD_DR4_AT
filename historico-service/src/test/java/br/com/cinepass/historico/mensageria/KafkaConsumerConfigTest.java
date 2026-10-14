package br.com.cinepass.historico.mensageria;
import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.listener.MessageListenerContainer;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {
    @Test
    void eventoInvalidoNaoEhDescartadoDepoisDeTresTentativas() {
        var handler = new KafkaConsumerConfig().kafkaErrorHandler();
        var record = new ConsumerRecord<String, String>("eventos", 0, 10L, "reserva", "{}");
        var consumer = mock(Consumer.class);
        var container = mock(MessageListenerContainer.class);
        for (int i = 0; i < 5; i++) {
            assertFalse(handler.handleOne(new IllegalArgumentException("payload invalido"), record, consumer, container));
        }
        assertFalse(handler.isAckAfterHandle());
    }
}

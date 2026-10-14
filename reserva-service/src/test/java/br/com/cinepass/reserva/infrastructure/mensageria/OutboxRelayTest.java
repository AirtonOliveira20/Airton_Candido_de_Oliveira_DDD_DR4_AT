package br.com.cinepass.reserva.infrastructure.mensageria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxRelayTest {
    private br.com.cinepass.reserva.observability.TraceSupport traces(){
        var traces=mock(br.com.cinepass.reserva.observability.TraceSupport.class);
        when(traces.within(anyMap(),anyString(),any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(2)).get());
        return traces;
    }

    @Test
    void outraInstanciaComLockImpedeLeituraEPublicacao() {
        var em = mock(EntityManager.class);
        var query = mock(Query.class);
        var repo = mock(SpringDataOutboxRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(false);
        new OutboxRelay(repo, kafka, em, traces(), tools.jackson.databind.json.JsonMapper.builder().build()).publicarPendentes();
        verifyNoInteractions(repo, kafka);
    }

    @Test
    void falhaNoPrimeiroEventoNaoPublicaSucessor() {
        var em = mock(EntityManager.class);
        var query = mock(Query.class);
        var repo = mock(SpringDataOutboxRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(true);
        var id = UUID.randomUUID();
        var primeiro = new OutboxEventoJpaEntity(UUID.randomUUID(), "ReservaCriada", id, "corr", "eventos", "{}", Instant.now());
        var segundo = new OutboxEventoJpaEntity(UUID.randomUUID(), "ReservaConfirmada", id, "corr", "eventos", "{}", Instant.now());
        when(repo.findTop100ByPublicadoEmIsNullOrderBySequenciaAsc()).thenReturn(List.of(primeiro, segundo));
        when(kafka.send(any(org.apache.kafka.clients.producer.ProducerRecord.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka indisponivel")));
        new OutboxRelay(repo, kafka, em, traces(), tools.jackson.databind.json.JsonMapper.builder().build()).publicarPendentes();
        verify(kafka, times(1)).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
        assertNull(primeiro.getPublicadoEm());
        assertNull(segundo.getPublicadoEm());
        assertEquals(1, primeiro.getTentativas());
        assertEquals(0, segundo.getTentativas());
    }
}

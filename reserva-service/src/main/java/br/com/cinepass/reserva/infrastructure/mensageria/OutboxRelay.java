package br.com.cinepass.reserva.infrastructure.mensageria;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;


@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final br.com.cinepass.reserva.observability.TraceSupport traces;
    private final tools.jackson.databind.json.JsonMapper json;
    private final jakarta.persistence.EntityManager entityManager;
    private final SpringDataOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(SpringDataOutboxRepository outbox, KafkaTemplate<String, String> kafkaTemplate, jakarta.persistence.EntityManager entityManager, br.com.cinepass.reserva.observability.TraceSupport traces, tools.jackson.databind.json.JsonMapper json) {
        this.traces=traces; this.json=json;
        this.entityManager = entityManager;
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${cinepass.outbox.intervalo-ms:500}")
    @Transactional
    public void publicarPendentes() {


        if (!Boolean.TRUE.equals(entityManager.createNativeQuery(
                "select pg_try_advisory_xact_lock(1128877651)").getSingleResult())) {
            return;
        }
        for (OutboxEventoJpaEntity evento : outbox.findTop100ByPublicadoEmIsNullOrderBySequenciaAsc()) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        evento.getTopico(), evento.getAggregateId().toString(), evento.getPayload());
                record.headers()
                        .add("eventId", bytes(evento.getEventId().toString()))
                        .add("eventType", bytes(evento.getEventType()))
                        .add("correlationId", bytes(evento.getCorrelationId()));

                java.util.Map<String,String> carrier=evento.getTraceContext()==null ? new java.util.HashMap<>() :
                        json.readValue(evento.getTraceContext(),new tools.jackson.core.type.TypeReference<java.util.Map<String,String>>(){});
                carrier.put("correlationId",evento.getCorrelationId());
                SendResult<String,String> resultado=traces.within(carrier,"outbox.publish",()->{
                    try {
                        var sent=kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                        log.atInfo().addKeyValue("eventId",evento.getEventId()).addKeyValue("reservaId",evento.getAggregateId())
                            .addKeyValue("correlationId",evento.getCorrelationId())
                            .log("operacao=KAFKA resultado=PUBLICADO particao={} offset={}",sent.getRecordMetadata().partition(),sent.getRecordMetadata().offset());
                        return sent;
                    }
                    catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException("interrupted");}
                    catch(Exception ex){throw new IllegalStateException("publish failed");}
                });
                evento.marcarPublicado(Instant.now());

            } catch (Exception ex) {
                evento.registrarFalha();
                log.atWarn().addKeyValue("eventId",evento.getEventId()).addKeyValue("reservaId",evento.getAggregateId())
                    .addKeyValue("correlationId",evento.getCorrelationId())
                    .log("operacao=KAFKA resultado=FALHA tentativa={} erro={}",evento.getTentativas(),ex.getClass().getSimpleName());
                return;  
            }
        }
    }

    private static byte[] bytes(String valor) {
        return valor.getBytes(StandardCharsets.UTF_8);
    }
}

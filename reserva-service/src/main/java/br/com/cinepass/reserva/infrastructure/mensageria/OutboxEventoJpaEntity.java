package br.com.cinepass.reserva.infrastructure.mensageria;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "outbox_eventos", indexes = {
        @Index(name = "idx_outbox_pendentes", columnList = "publicado_em, sequencia"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_id")
})
public class OutboxEventoJpaEntity {

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequencia;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(nullable = false, length = 150)
    private String topico;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(nullable = false)
    private int tentativas;

    protected OutboxEventoJpaEntity() {}

    public OutboxEventoJpaEntity(UUID eventId, String eventType, UUID aggregateId, String correlationId,
                                 String topico, String payload, Instant ocorridoEm) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.topico = topico;
        this.payload = payload;
        this.ocorridoEm = ocorridoEm;
    }

    @Column(columnDefinition="text")
    private String traceContext;
    public String getTraceContext(){return traceContext;}
    public void setTraceContext(String value){traceContext=value;}

    public void marcarPublicado(Instant quando) { this.publicadoEm = quando; this.tentativas++; }
    public void registrarFalha() { this.tentativas++; }

    public Long getSequencia() { return sequencia; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getCorrelationId() { return correlationId; }
    public String getTopico() { return topico; }
    public String getPayload() { return payload; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public Instant getPublicadoEm() { return publicadoEm; }
    public int getTentativas() { return tentativas; }
}

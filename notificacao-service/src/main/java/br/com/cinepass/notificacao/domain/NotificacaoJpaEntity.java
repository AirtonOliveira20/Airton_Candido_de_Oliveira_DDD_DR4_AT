package br.com.cinepass.notificacao.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacoes", indexes = {
        @Index(name = "idx_notificacao_cliente", columnList = "destinatario_id"),
        @Index(name = "idx_notificacao_reserva", columnList = "reserva_id")
})
public class NotificacaoJpaEntity {

    @Id
    private UUID id;


    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;


    @Column(name = "destinatario_id", nullable = false)
    private UUID destinatarioId;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected NotificacaoJpaEntity() {}

    public NotificacaoJpaEntity(UUID eventId, String eventType, UUID reservaId, UUID destinatarioId,
                                String canal, String mensagem, String correlationId) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.eventType = eventType;
        this.reservaId = reservaId;
        this.destinatarioId = destinatarioId;
        this.canal = canal;
        this.mensagem = mensagem;
        this.correlationId = correlationId;
        this.criadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getReservaId() { return reservaId; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public String getCanal() { return canal; }
    public String getMensagem() { return mensagem; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCriadaEm() { return criadaEm; }
}

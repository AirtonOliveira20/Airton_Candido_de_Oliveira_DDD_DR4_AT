package br.com.cinepass.auditoria.domain;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;





@Entity
@Table(name = "eventos_auditados", indexes = {
        @Index(name = "idx_auditoria_reserva", columnList = "reserva_id, ocorrido_em"),
        @Index(name = "idx_auditoria_correlation", columnList = "correlation_id"),
        @Index(name = "idx_auditoria_tipo", columnList = "event_type")
})
public class EventoAuditadoJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(length = 60)
    private String produtor;

    @Column(name = "versao_contrato")
    private int versaoContrato;

    @Column(nullable = false, length = 150)
    private String topico;

    @Column(nullable = false)
    private int particao;

    @Column(name = "offset_kafka", nullable = false)
    private long offset;

    @Column(name = "chave", length = 100)
    private String chave;


    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    protected EventoAuditadoJpaEntity() {}

    public EventoAuditadoJpaEntity(UUID eventId, String eventType, UUID reservaId, String correlationId, Instant ocorridoEm,
                                   String produtor, int versaoContrato, String topico, int particao, long offset,
                                   String chave, String conteudo) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.reservaId = reservaId;
        this.correlationId = correlationId;
        this.ocorridoEm = ocorridoEm;
        this.produtor = produtor;
        this.versaoContrato = versaoContrato;
        this.topico = topico;
        this.particao = particao;
        this.offset = offset;
        this.chave = chave;
        this.conteudo = conteudo;
        this.registradoEm = Instant.now();
    }

    @Override public UUID getId() { return eventId; }
    @Override public boolean isNew() { return true; }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getReservaId() { return reservaId; }
    public String getCorrelationId() { return correlationId; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public String getProdutor() { return produtor; }
    public int getVersaoContrato() { return versaoContrato; }
    public String getTopico() { return topico; }
    public int getParticao() { return particao; }
    public long getOffset() { return offset; }
    public String getChave() { return chave; }
    public String getConteudo() { return conteudo; }
    public Instant getRegistradoEm() { return registradoEm; }
}

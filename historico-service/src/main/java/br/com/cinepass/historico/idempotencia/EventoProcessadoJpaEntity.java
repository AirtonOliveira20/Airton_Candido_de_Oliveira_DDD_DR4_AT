package br.com.cinepass.historico.idempotencia;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;






@Entity
@Table(name = "eventos_processados")
public class EventoProcessadoJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    protected EventoProcessadoJpaEntity() {}

    public EventoProcessadoJpaEntity(UUID eventId, String eventType, UUID reservaId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.reservaId = reservaId;
        this.processadoEm = Instant.now();
    }

    @Override public UUID getId() { return eventId; }
    @Override public boolean isNew() { return true; }
    public String getEventType() { return eventType; }
    public UUID getReservaId() { return reservaId; }
    public Instant getProcessadoEm() { return processadoEm; }
}

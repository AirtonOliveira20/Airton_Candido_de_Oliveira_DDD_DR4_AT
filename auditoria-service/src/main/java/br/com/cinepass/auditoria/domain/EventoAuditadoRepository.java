package br.com.cinepass.auditoria.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoAuditadoRepository extends JpaRepository<EventoAuditadoJpaEntity, UUID> {
    List<EventoAuditadoJpaEntity> findByReservaIdOrderByOcorridoEmAsc(UUID reservaId);
    List<EventoAuditadoJpaEntity> findByCorrelationIdOrderByOcorridoEmAsc(String correlationId);
    List<EventoAuditadoJpaEntity> findTop100ByEventTypeOrderByOcorridoEmDesc(String eventType);
    List<EventoAuditadoJpaEntity> findTop100ByOrderByOcorridoEmDesc();
}

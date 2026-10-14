package br.com.cinepass.auditoria.application;

import br.com.cinepass.auditoria.domain.EventoAuditadoJpaEntity;
import br.com.cinepass.auditoria.domain.EventoAuditadoRepository;
import br.com.cinepass.auditoria.mensageria.EventoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditoriaService {

    private final EventoAuditadoRepository repositorio;

    public AuditoriaService(EventoAuditadoRepository repositorio) {
        this.repositorio = repositorio;
    }


    @Transactional
    public boolean registrar(EventoReserva e, String topico, int particao, long offset, String chave, String conteudo) {
        if (repositorio.existsById(e.eventId())) {
            return false;
        }
        repositorio.save(new EventoAuditadoJpaEntity(e.eventId(), e.eventType(), e.reservaId(), e.correlationId(),
                e.occurredAt(), e.producer(), e.version(), topico, particao, offset, chave, conteudo));
        return true;
    }

    @Transactional(readOnly = true)
    public List<EventoAuditadoJpaEntity> consultar(UUID reservaId, String correlationId, String eventType) {
        if (reservaId != null) return repositorio.findByReservaIdOrderByOcorridoEmAsc(reservaId);
        if (correlationId != null) return repositorio.findByCorrelationIdOrderByOcorridoEmAsc(correlationId);
        if (eventType != null) return repositorio.findTop100ByEventTypeOrderByOcorridoEmDesc(eventType);
        return repositorio.findTop100ByOrderByOcorridoEmDesc();
    }

    @Transactional(readOnly = true)
    public Optional<EventoAuditadoJpaEntity> buscar(UUID eventId) {
        return repositorio.findById(eventId);
    }
}

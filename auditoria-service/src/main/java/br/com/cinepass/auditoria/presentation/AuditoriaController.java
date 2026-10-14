package br.com.cinepass.auditoria.presentation;

import br.com.cinepass.auditoria.application.AuditoriaService;
import br.com.cinepass.auditoria.domain.EventoAuditadoJpaEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditoria/eventos")
public class AuditoriaController {

    private final AuditoriaService service;
    private final JsonMapper jsonMapper;

    public AuditoriaController(AuditoriaService service, JsonMapper jsonMapper) {
        this.service = service;
        this.jsonMapper = jsonMapper;
    }


    @GetMapping
    public List<EventoAuditadoView> consultar(@RequestParam(required = false) UUID reservaId,
                                              @RequestParam(required = false) String correlationId,
                                              @RequestParam(required = false) String eventType) {
        return service.consultar(reservaId, correlationId, eventType).stream().map(this::view).toList();
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventoAuditadoView> buscar(@PathVariable UUID eventId) {
        return service.buscar(eventId).map(this::view).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private EventoAuditadoView view(EventoAuditadoJpaEntity e) {
        return new EventoAuditadoView(e.getEventId(), e.getEventType(), e.getReservaId(), e.getCorrelationId(),
                e.getOcorridoEm(), e.getRegistradoEm(), e.getProdutor(), e.getVersaoContrato(), e.getTopico(),
                e.getParticao(), e.getOffset(), e.getChave(), jsonMapper.readTree(e.getConteudo()));
    }

    public record EventoAuditadoView(UUID eventId, String eventType, UUID reservaId, String correlationId,
                                     Instant ocorridoEm, Instant registradoEm, String produtor, int versaoContrato,
                                     String topico, int particao, long offset, String chave, JsonNode conteudo) {}
}

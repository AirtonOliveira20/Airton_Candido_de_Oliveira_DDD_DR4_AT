package br.com.cinepass.notificacao.presentation;

import br.com.cinepass.notificacao.application.NotificacaoService;
import br.com.cinepass.notificacao.domain.NotificacaoJpaEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificacaoView> listar(@RequestParam(required = false) UUID clienteId,
                                        @RequestParam(required = false) UUID reservaId) {
        return service.listar(clienteId, reservaId).stream().map(NotificacaoView::de).toList();
    }

    public record NotificacaoView(UUID id, UUID eventId, String eventType, UUID reservaId, UUID destinatarioId,
                                  String canal, String mensagem, String correlationId, Instant criadaEm) {
        static NotificacaoView de(NotificacaoJpaEntity n) {
            return new NotificacaoView(n.getId(), n.getEventId(), n.getEventType(), n.getReservaId(), n.getDestinatarioId(),
                    n.getCanal(), n.getMensagem(), n.getCorrelationId(), n.getCriadaEm());
        }
    }
}

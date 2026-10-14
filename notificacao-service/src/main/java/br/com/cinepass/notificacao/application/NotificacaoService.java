package br.com.cinepass.notificacao.application;

import br.com.cinepass.notificacao.domain.NotificacaoJpaEntity;
import br.com.cinepass.notificacao.domain.NotificacaoRepository;
import br.com.cinepass.notificacao.idempotencia.EventoProcessadoJpaEntity;
import br.com.cinepass.notificacao.idempotencia.EventoProcessadoRepository;
import br.com.cinepass.notificacao.mensageria.EventoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static br.com.cinepass.notificacao.mensageria.EventoReserva.*;

@Service
public class NotificacaoService {

    static final String CANAL = "EMAIL";
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final NotificacaoRepository notificacoes;
    private final EventoProcessadoRepository processados;

    public NotificacaoService(NotificacaoRepository notificacoes, EventoProcessadoRepository processados) {
        this.notificacoes = notificacoes;
        this.processados = processados;
    }

    @Transactional
    public ResultadoProcessamento processar(EventoReserva evento) {
        if (processados.existsById(evento.eventId())) {
            return ResultadoProcessamento.DUPLICADO_IGNORADO;
        }

        String mensagem = mensagemPara(evento);
        ResultadoProcessamento resultado = ResultadoProcessamento.SEM_NOTIFICACAO;
        if (mensagem != null) {

            notificacoes.save(new NotificacaoJpaEntity(evento.eventId(), evento.eventType(), evento.reservaId(),
                    evento.payload().clienteId(), CANAL, mensagem, evento.correlationId()));
            resultado = ResultadoProcessamento.NOTIFICACAO_REGISTRADA;
        }

        processados.save(new EventoProcessadoJpaEntity(evento.eventId(), evento.eventType(), evento.reservaId()));
        return resultado;
    }

    String mensagemPara(EventoReserva e) {
        var p = e.payload();
        String assentos = p.assentos() == null ? "" : String.join(", ", p.assentos());
        return switch (e.eventType()) {
            case RESERVA_CRIADA -> "Recebemos sua reserva " + e.reservaId() + " (assentos " + assentos + ", total "
                    + moeda(p.valorTotal()) + "). Aguardando a confirmação do pagamento.";
            case PAGAMENTO_APROVADO -> "Pagamento da reserva " + e.reservaId() + " aprovado. Estamos emitindo seu ingresso.";
            case RESERVA_CONFIRMADA -> "Reserva " + e.reservaId() + " confirmada! Ingresso " + p.ingressoId()
                    + " para os assentos " + assentos + ". Bom filme!";
            case RESERVA_CANCELADA -> "Reserva " + e.reservaId() + " cancelada. " + motivo(p.motivo());
            default -> null;
        };
    }

    private String motivo(String motivo) {
        if (motivo == null) return "";
        return switch (motivo) {
            case "PAGAMENTO_RECUSADO" -> "Motivo: o pagamento foi recusado.";
            case "FALHA_EMISSAO_INGRESSO" -> "Motivo: não foi possível emitir o ingresso. O valor pago foi estornado.";
            default -> "Motivo: " + motivo + ".";
        };
    }

    private String moeda(BigDecimal valor) {
        return valor == null ? "-" : NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoJpaEntity> listar(UUID clienteId, UUID reservaId) {
        if (reservaId != null) return notificacoes.findByReservaIdOrderByCriadaEmAsc(reservaId);
        if (clienteId != null) return notificacoes.findByDestinatarioIdOrderByCriadaEmAsc(clienteId);
        return notificacoes.findTop100ByOrderByCriadaEmDesc();
    }
}

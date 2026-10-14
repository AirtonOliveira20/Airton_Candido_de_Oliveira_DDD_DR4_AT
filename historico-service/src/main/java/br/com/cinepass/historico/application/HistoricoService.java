package br.com.cinepass.historico.application;

import br.com.cinepass.historico.domain.*;
import br.com.cinepass.historico.idempotencia.EventoProcessadoJpaEntity;
import br.com.cinepass.historico.idempotencia.EventoProcessadoRepository;
import br.com.cinepass.historico.mensageria.EventoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static br.com.cinepass.historico.mensageria.EventoReserva.*;

@Service
public class HistoricoService {

    private final HistoricoClienteRepository clientes;
    private final ReservaHistoricoRepository reservas;
    private final EventoProcessadoRepository processados;

    public HistoricoService(HistoricoClienteRepository clientes, ReservaHistoricoRepository reservas,
                            EventoProcessadoRepository processados) {
        this.clientes = clientes;
        this.reservas = reservas;
        this.processados = processados;
    }





    @Transactional
    public ResultadoProcessamento processar(EventoReserva evento) {
        if (processados.existsById(evento.eventId())) {
            return ResultadoProcessamento.DUPLICADO_IGNORADO;
        }

        ResultadoProcessamento resultado = switch (evento.eventType()) {
            case RESERVA_CRIADA -> {
                cliente(evento).registrarReservaCriada();
                reserva(evento);
                yield ResultadoProcessamento.HISTORICO_ATUALIZADO;
            }
            case PAGAMENTO_APROVADO -> {
                reserva(evento).pagamentoAprovado(evento.occurredAt());
                yield ResultadoProcessamento.HISTORICO_ATUALIZADO;
            }
            case RESERVA_CONFIRMADA -> {
                cliente(evento).registrarConfirmacao(evento.quantidadeAssentos(), evento.payload().valorTotal());
                reserva(evento).confirmar(evento.payload().ingressoId(), evento.occurredAt());
                yield ResultadoProcessamento.HISTORICO_ATUALIZADO;
            }
            case RESERVA_CANCELADA -> {
                cliente(evento).registrarCancelamento();
                reserva(evento).cancelar(evento.payload().motivo(), evento.occurredAt());
                yield ResultadoProcessamento.HISTORICO_ATUALIZADO;
            }
            default -> ResultadoProcessamento.SEM_IMPACTO;
        };

        processados.save(new EventoProcessadoJpaEntity(evento.eventId(), evento.eventType(), evento.reservaId()));
        return resultado;
    }


    private HistoricoClienteJpaEntity cliente(EventoReserva e) {
        UUID clienteId = e.payload().clienteId();
        return clientes.findById(clienteId).orElseGet(() -> clientes.save(new HistoricoClienteJpaEntity(clienteId)));
    }

    private ReservaHistoricoJpaEntity reserva(EventoReserva e) {
        var p = e.payload();
        return reservas.findById(e.reservaId()).orElseGet(() -> reservas.save(
                new ReservaHistoricoJpaEntity(e.reservaId(), p.clienteId(), p.sessaoId(), p.assentos(), p.valorTotal(), e.occurredAt())));
    }

    @Transactional(readOnly = true)
    public HistoricoView consultar(UUID clienteId) {
        HistoricoClienteJpaEntity c = clientes.findById(clienteId)
                .orElseThrow(() -> new HistoricoNaoEncontradoException(clienteId));
        List<HistoricoView.ReservaView> lista = reservas.findByClienteIdOrderByCriadaEmDesc(clienteId).stream()
                .map(r -> new HistoricoView.ReservaView(r.getReservaId(), r.getSessaoId(), r.getAssentos(), r.getValorTotal(),
                        r.getStatus(), r.getIngressoId(), r.getMotivoCancelamento(), r.getCriadaEm(), r.getAtualizadaEm()))
                .toList();
        return new HistoricoView(c.getClienteId(), c.getReservasCriadas(), c.getReservasConfirmadas(),
                c.getReservasCanceladas(), c.getIngressosAdquiridos(), c.getValorTotalGasto(), c.getAtualizadoEm(), lista);
    }
}

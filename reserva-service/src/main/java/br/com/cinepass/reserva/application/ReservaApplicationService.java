package br.com.cinepass.reserva.application;

import br.com.cinepass.reserva.application.port.IngressoGateway;
import br.com.cinepass.reserva.application.port.PagamentoGateway;
import br.com.cinepass.reserva.application.port.ReservaEventoPublisher;
import br.com.cinepass.reserva.domain.model.*;
import br.com.cinepass.reserva.domain.repository.ReservaRepository;
import br.com.cinepass.reserva.domain.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReservaApplicationService {
    private final ReservaRepository reservaRepository;
    private final SessaoRepository sessaoRepository;
    private final PagamentoGateway pagamentoGateway;
    private final IngressoGateway ingressoGateway;
    private final ReservaEventoPublisher eventos;

    public ReservaApplicationService(ReservaRepository reservaRepository, SessaoRepository sessaoRepository,
                                     PagamentoGateway pagamentoGateway, IngressoGateway ingressoGateway,
                                     ReservaEventoPublisher eventos) {
        this.reservaRepository=reservaRepository; this.sessaoRepository=sessaoRepository;
        this.pagamentoGateway=pagamentoGateway; this.ingressoGateway=ingressoGateway;
        this.eventos=eventos;
    }


    @Transactional

    public ReservaDetalhe realizar(RealizarReservaCommand command) {
        Sessao sessao = sessaoRepository.buscarPorId(new SessaoId(command.sessaoId()))
                .orElseThrow(() -> new SessaoNaoEncontradaException(command.sessaoId()));

        List<AssentoId> assentos = command.assentos().stream().map(AssentoId::new).toList();
        sessao.reservar(assentos);
        sessaoRepository.salvar(sessao);

        Reserva reserva = Reserva.criar(new ClienteId(command.clienteId()), sessao.getId(), assentos, sessao.getPreco().multiplicar(assentos.size()));
        reserva.aguardarPagamento();
        reservaRepository.salvar(reserva);

        eventos.reservaCriada(reserva, command.correlationId());

        PagamentoGateway.PagamentoResultado pagamento = pagamentoGateway.cobrar(
                reserva.getId().valor(), reserva.getValorTotal().valor(), command.simularRecusaPagamento());

        if (!"APROVADO".equals(pagamento.status())) {
            throw new PagamentoRecusadoException(reserva.getId().valor());
        }

        reserva.confirmarPagamento(pagamento.pagamentoId());
        eventos.pagamentoAprovado(reserva);
        reserva.iniciarEmissaoIngresso();
        reservaRepository.salvar(reserva);

        try {
            IngressoGateway.IngressoResultado ingresso = ingressoGateway.emitir(
                    reserva.getId().valor(), reserva.getSessaoId().valor(),
                    reserva.getAssentos().stream().map(AssentoId::valor).toList(), command.simularFalhaIngresso());
            reserva.confirmar(ingresso.ingressoId());
            Reserva confirmada = reservaRepository.salvar(reserva);
            eventos.reservaConfirmada(confirmada);
            return ReservaDetalhe.de(confirmada);
        } catch (FalhaIntegracaoException ex) {

            throw new FalhaProcessamentoReservaException(reserva.getId().valor(), pagamento.pagamentoId(), ex);
        }
    }
    @Transactional
    public ReservaDetalhe confirmarPagamento(UUID reservaId, UUID pagamentoId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        reserva.confirmarPagamento(pagamentoId);
        Reserva salva = reservaRepository.salvar(reserva);
        eventos.pagamentoAprovado(salva);
        return ReservaDetalhe.de(salva);
    }
    @Transactional
    public ReservaDetalhe cancelar(UUID reservaId, String motivo){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        Sessao sessao = sessaoRepository
                .buscarPorId(
                        reserva.getSessaoId()
                )
                .orElseThrow(
                        () -> new SessaoNaoEncontradaException(
                                reserva.getSessaoId().valor()
                        )
                );
        if (reserva.getStatus() == StatusReserva.CANCELADA) {

            return ReservaDetalhe.de(reserva);
        }
        reserva.cancelar();
        sessao.liberar(reserva.getAssentos());
        sessaoRepository.salvar(sessao);
        reservaRepository.salvar(reserva);
        eventos.reservaCancelada(reserva, motivo);
        return ReservaDetalhe.de(reserva);

    }
    @Transactional
    public ReservaDetalhe iniciarEmissaoIngresso(UUID reservaId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        reserva.iniciarEmissaoIngresso();
        return ReservaDetalhe.de(reservaRepository.salvar(reserva));

    }
    @Transactional
    public ReservaDetalhe confirmar(UUID reservaId, UUID ingressoId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        reserva.confirmar(ingressoId);
        Reserva confirmada = reservaRepository.salvar(reserva);
        eventos.reservaConfirmada(confirmada);
        return ReservaDetalhe.de(confirmada);

    }
    @Transactional
    public ReservaDetalhe iniciar(RealizarReservaCommand command) {
        Sessao sessao = sessaoRepository
                .buscarPorId(
                        new SessaoId(command.sessaoId())
                )
                .orElseThrow(
                        () -> new SessaoNaoEncontradaException(
                                command.sessaoId()
                        )
                );

        List<AssentoId> assentos =
                command.assentos()
                        .stream()
                        .map(AssentoId::new)
                        .toList();

        sessao.reservar(assentos);

        sessaoRepository.salvar(sessao);

        Reserva reserva = Reserva.criar(
                new ClienteId(command.clienteId()),
                sessao.getId(),
                assentos,
                sessao.getPreco()
                        .multiplicar(assentos.size())
        );

        reserva.aguardarPagamento();

        reservaRepository.salvar(reserva);

        eventos.reservaCriada(reserva, command.correlationId());

        return ReservaDetalhe.de(reserva);
    }


    @Transactional
    public int reenviarEventos(UUID reservaId) {
        reservaRepository.buscarPorId(new ReservaId(reservaId))
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaId));
        return eventos.reenviarEventos(reservaId);
    }

    @Transactional(readOnly = true)
    public ReservaDetalhe buscar(UUID id) {
        return reservaRepository.buscarPorId(new ReservaId(id)).map(ReservaDetalhe::de)
                .orElseThrow(() -> new ReservaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaDetalhe> listar() { return reservaRepository.listar().stream().map(ReservaDetalhe::de).toList(); }
}

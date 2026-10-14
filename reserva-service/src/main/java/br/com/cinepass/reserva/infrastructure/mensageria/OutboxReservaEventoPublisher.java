package br.com.cinepass.reserva.infrastructure.mensageria;

import br.com.cinepass.reserva.application.port.ReservaEventoPublisher;
import br.com.cinepass.reserva.domain.model.AssentoId;
import br.com.cinepass.reserva.domain.model.Reserva;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static br.com.cinepass.reserva.infrastructure.mensageria.TiposEventoReserva.*;


@Component
public class OutboxReservaEventoPublisher implements ReservaEventoPublisher {

    static final String PRODUTOR = "reserva-service";
    static final int VERSAO_CONTRATO = 1;

    private final SpringDataOutboxRepository outbox;
    private final JsonMapper jsonMapper;
    private final String topico;
    private final br.com.cinepass.reserva.observability.TraceSupport traces;

    public OutboxReservaEventoPublisher(SpringDataOutboxRepository outbox, JsonMapper jsonMapper,
                                        @Value("${cinepass.kafka.topico-reservas}") String topico, br.com.cinepass.reserva.observability.TraceSupport traces) {
        this.traces=traces;
        this.outbox = outbox;
        this.jsonMapper = jsonMapper;
        this.topico = topico;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reservaCriada(Reserva reserva, String correlationId) {
        registrar(RESERVA_CRIADA, reserva, correlationId, null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void pagamentoAprovado(Reserva reserva) {
        registrar(PAGAMENTO_APROVADO, reserva, correlacaoDa(reserva), null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reservaConfirmada(Reserva reserva) {
        registrar(RESERVA_CONFIRMADA, reserva, correlacaoDa(reserva), null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reservaCancelada(Reserva reserva, String motivo) {
        registrar(RESERVA_CANCELADA, reserva, correlacaoDa(reserva), motivo);
    }

    @Override
    @Transactional
    public int reenviarEventos(UUID reservaId) {
        return outbox.marcarParaReenvio(reservaId);
    }

    private void registrar(String tipo, Reserva reserva, String correlationId, String motivo) {
        UUID reservaId = reserva.getId().valor();
        Instant agora = Instant.now();
        var payload = new EventoReservaMensagem.Payload(
                reserva.getClienteId().valor(),
                reserva.getSessaoId().valor(),
                reserva.getAssentos().stream().map(AssentoId::valor).toList(),
                reserva.getValorTotal().valor(),
                reserva.getStatus().name(),
                reserva.getPagamentoId(),
                reserva.getIngressoId(),
                motivo);
        var mensagem = new EventoReservaMensagem(UUID.randomUUID(), tipo, reservaId, agora, correlationId,
                VERSAO_CONTRATO, PRODUTOR, payload);

        var row=new OutboxEventoJpaEntity(mensagem.eventId(), tipo, reservaId, correlationId, topico,
                jsonMapper.writeValueAsString(mensagem), agora);
        var carrier=traces.capture(); carrier.put("correlationId",correlationId);
        carrier.put("eventId",mensagem.eventId().toString()); carrier.put("reservaId",reservaId.toString());
        row.setTraceContext(jsonMapper.writeValueAsString(carrier));
        outbox.save(row);
        org.slf4j.LoggerFactory.getLogger(getClass()).atInfo().addKeyValue("eventId",mensagem.eventId())
            .addKeyValue("reservaId",reservaId).addKeyValue("correlationId",correlationId)
            .log("operacao=OUTBOX resultado=REGISTRADO");
    }


    private String correlacaoDa(Reserva reserva) {
        UUID reservaId = reserva.getId().valor();
        return outbox.findFirstByAggregateIdOrderBySequenciaAsc(reservaId)
                .map(OutboxEventoJpaEntity::getCorrelationId)
                .orElse(reservaId.toString());
    }
}

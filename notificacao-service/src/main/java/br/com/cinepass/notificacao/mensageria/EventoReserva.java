package br.com.cinepass.notificacao.mensageria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;





@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoReserva(
        UUID eventId,
        String eventType,
        UUID reservaId,
        Instant occurredAt,
        String correlationId,
        int version,
        String producer,
        Payload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(
            UUID clienteId,
            UUID sessaoId,
            List<String> assentos,
            BigDecimal valorTotal,
            String statusReserva,
            UUID pagamentoId,
            UUID ingressoId,
            String motivo
    ) {}

    public static final String RESERVA_CRIADA = "ReservaCriada";
    public static final String PAGAMENTO_APROVADO = "PagamentoAprovado";
    public static final String RESERVA_CONFIRMADA = "ReservaConfirmada";
    public static final String RESERVA_CANCELADA = "ReservaCancelada";


    public void validar() {
        if (eventId == null || eventType == null || eventType.isBlank() || reservaId == null
                || occurredAt == null || payload == null || payload.clienteId() == null) {
            throw new EventoInvalidoException("Evento sem campos obrigatórios");
        }
    }

    public int quantidadeAssentos() {
        return payload.assentos() == null ? 0 : payload.assentos().size();
    }
}

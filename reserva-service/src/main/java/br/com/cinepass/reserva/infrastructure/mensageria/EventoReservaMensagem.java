package br.com.cinepass.reserva.infrastructure.mensageria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record EventoReservaMensagem(
        UUID eventId,
        String eventType,
        UUID reservaId,
        Instant occurredAt,
        String correlationId,
        int version,
        String producer,
        Payload payload
) {
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
}

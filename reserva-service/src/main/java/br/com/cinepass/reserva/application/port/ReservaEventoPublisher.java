package br.com.cinepass.reserva.application.port;

import br.com.cinepass.reserva.domain.model.Reserva;

import java.util.UUID;


public interface ReservaEventoPublisher {
    void reservaCriada(Reserva reserva, String correlationId);
    void pagamentoAprovado(Reserva reserva);
    void reservaConfirmada(Reserva reserva);
    void reservaCancelada(Reserva reserva, String motivo);

   
    int reenviarEventos(UUID reservaId);
}

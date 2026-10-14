package br.com.cinepass.reserva.application;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.UUID;

public record RealizarReservaCommand(UUID clienteId, UUID sessaoId, List<String> assentos,
                                     boolean simularRecusaPagamento, boolean simularFalhaIngresso,
                                     String correlationId) {
  
    @JsonCreator
    public RealizarReservaCommand {
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
    }

    public RealizarReservaCommand(UUID clienteId, UUID sessaoId, List<String> assentos,
                                  boolean simularRecusaPagamento, boolean simularFalhaIngresso) {
        this(clienteId, sessaoId, assentos, simularRecusaPagamento, simularFalhaIngresso, null);
    }
}

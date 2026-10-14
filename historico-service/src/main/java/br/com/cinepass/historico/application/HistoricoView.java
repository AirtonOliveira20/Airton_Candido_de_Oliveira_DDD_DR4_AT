package br.com.cinepass.historico.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HistoricoView(UUID clienteId, int reservasCriadas, int reservasConfirmadas, int reservasCanceladas,
                            int ingressosAdquiridos, BigDecimal valorTotalGasto, Instant atualizadoEm,
                            List<ReservaView> reservas) {

    public record ReservaView(UUID reservaId, UUID sessaoId, List<String> assentos, BigDecimal valorTotal, String status,
                              UUID ingressoId, String motivoCancelamento, Instant criadaEm, Instant atualizadaEm) {}
}

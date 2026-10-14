package br.com.cinepass.historico.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;








@Entity
@Table(name = "historico_clientes")
public class HistoricoClienteJpaEntity {

    @Id
    @Column(name = "cliente_id")
    private UUID clienteId;

    @Version
    private Long versao;

    @Column(name = "reservas_criadas", nullable = false)
    private int reservasCriadas;

    @Column(name = "reservas_confirmadas", nullable = false)
    private int reservasConfirmadas;

    @Column(name = "reservas_canceladas", nullable = false)
    private int reservasCanceladas;

    @Column(name = "ingressos_adquiridos", nullable = false)
    private int ingressosAdquiridos;

    @Column(name = "valor_total_gasto", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotalGasto = BigDecimal.ZERO;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected HistoricoClienteJpaEntity() {}

    public HistoricoClienteJpaEntity(UUID clienteId) {
        this.clienteId = clienteId;
        this.atualizadoEm = Instant.now();
    }

    public void registrarReservaCriada() {
        reservasCriadas++;
        tocar();
    }

    public void registrarConfirmacao(int ingressos, BigDecimal valor) {
        reservasConfirmadas++;
        ingressosAdquiridos += ingressos;
        valorTotalGasto = valorTotalGasto.add(valor == null ? BigDecimal.ZERO : valor);
        tocar();
    }

    public void registrarCancelamento() {
        reservasCanceladas++;
        tocar();
    }

    private void tocar() { atualizadoEm = Instant.now(); }

    public UUID getClienteId() { return clienteId; }
    public int getReservasCriadas() { return reservasCriadas; }
    public int getReservasConfirmadas() { return reservasConfirmadas; }
    public int getReservasCanceladas() { return reservasCanceladas; }
    public int getIngressosAdquiridos() { return ingressosAdquiridos; }
    public BigDecimal getValorTotalGasto() { return valorTotalGasto; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}

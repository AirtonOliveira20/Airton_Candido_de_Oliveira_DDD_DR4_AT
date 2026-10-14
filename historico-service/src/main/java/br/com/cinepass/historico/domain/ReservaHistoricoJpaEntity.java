package br.com.cinepass.historico.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "historico_reservas", indexes = @Index(name = "idx_historico_reserva_cliente", columnList = "cliente_id"))
public class ReservaHistoricoJpaEntity {

    @Id
    @Column(name = "reserva_id")
    private UUID reservaId;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "sessao_id")
    private UUID sessaoId;

    @Column(length = 200)
    private String assentos;

    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "ingresso_id")
    private UUID ingressoId;

    @Column(name = "motivo_cancelamento", length = 60)
    private String motivoCancelamento;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected ReservaHistoricoJpaEntity() {}

    public ReservaHistoricoJpaEntity(UUID reservaId, UUID clienteId, UUID sessaoId, List<String> assentos,
                                     BigDecimal valorTotal, Instant criadaEm) {
        this.reservaId = reservaId;
        this.clienteId = clienteId;
        this.sessaoId = sessaoId;
        this.assentos = assentos == null ? null : String.join(",", assentos);
        this.valorTotal = valorTotal;
        this.status = "AGUARDANDO_PAGAMENTO";
        this.criadaEm = criadaEm;
        this.atualizadaEm = criadaEm;
    }

    public void pagamentoAprovado(Instant quando) { mudar("PAGAMENTO_APROVADO", quando); }

    public void confirmar(UUID ingressoId, Instant quando) {
        this.ingressoId = ingressoId;
        mudar("CONFIRMADA", quando);
    }

    public void cancelar(String motivo, Instant quando) {
        this.motivoCancelamento = motivo;
        mudar("CANCELADA", quando);
    }

    private void mudar(String novoStatus, Instant quando) {
        this.status = novoStatus;
        this.atualizadaEm = quando;
    }

    public UUID getReservaId() { return reservaId; }
    public UUID getClienteId() { return clienteId; }
    public UUID getSessaoId() { return sessaoId; }
    public List<String> getAssentos() { return assentos == null || assentos.isBlank() ? List.of() : List.of(assentos.split(",")); }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getStatus() { return status; }
    public UUID getIngressoId() { return ingressoId; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}

package br.com.cinepass.historico.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservaHistoricoRepository extends JpaRepository<ReservaHistoricoJpaEntity, UUID> {
    List<ReservaHistoricoJpaEntity> findByClienteIdOrderByCriadaEmDesc(UUID clienteId);
}

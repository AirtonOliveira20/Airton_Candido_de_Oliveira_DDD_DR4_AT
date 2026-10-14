package br.com.cinepass.historico.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HistoricoClienteRepository extends JpaRepository<HistoricoClienteJpaEntity, UUID> {}

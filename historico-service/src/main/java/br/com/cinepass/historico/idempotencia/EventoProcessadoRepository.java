package br.com.cinepass.historico.idempotencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessadoJpaEntity, UUID> {}

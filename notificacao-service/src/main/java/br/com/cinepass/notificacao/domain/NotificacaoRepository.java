package br.com.cinepass.notificacao.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<NotificacaoJpaEntity, UUID> {
    List<NotificacaoJpaEntity> findByDestinatarioIdOrderByCriadaEmAsc(UUID destinatarioId);
    List<NotificacaoJpaEntity> findByReservaIdOrderByCriadaEmAsc(UUID reservaId);
    List<NotificacaoJpaEntity> findTop100ByOrderByCriadaEmDesc();
}

package br.com.cinepass.reserva.infrastructure.mensageria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataOutboxRepository extends JpaRepository<OutboxEventoJpaEntity, Long> {

    List<OutboxEventoJpaEntity> findTop100ByPublicadoEmIsNullOrderBySequenciaAsc();

    Optional<OutboxEventoJpaEntity> findFirstByAggregateIdOrderBySequenciaAsc(UUID aggregateId);

    @Modifying
    @Query("update OutboxEventoJpaEntity e set e.publicadoEm = null where e.aggregateId = :aggregateId")
    int marcarParaReenvio(@Param("aggregateId") UUID aggregateId);
}

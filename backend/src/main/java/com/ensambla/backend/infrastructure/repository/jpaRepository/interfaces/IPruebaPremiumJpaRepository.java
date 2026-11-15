package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IPruebaPremiumJpaRepository extends JpaRepository<PruebaPremiumEntity, UUID> {

    /** Candidatos globales del proceso interno; el servicio vuelve a comprobar bajo bloqueo por negocio. */
    @Query(value = """
            SELECT negocio_id FROM suscripcion
            WHERE estado = 'PRUEBA' AND fin_en <= :ahora
            ORDER BY fin_en, negocio_id LIMIT 100
            """, nativeQuery = true)
    List<UUID> findNegociosVencidos(@Param("ahora") OffsetDateTime ahora);
}

package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IRegistroJpaRepository extends JpaRepository<RegistroEntity, UUID> {

    /** Registros del bloque cuyo valor contiene el patrón (LIKE con {@code \} de escape); sin patrón, todos. */
    String FILTRADOS = """
            FROM registro r
            WHERE r.negocio_id = :negocioId
              AND r.bloque_id = :bloqueId
              AND (CAST(:patron AS text) IS NULL OR EXISTS (
                    SELECT 1 FROM jsonb_each_text(r.valores) AS v
                    WHERE v.value ILIKE CAST(:patron AS text) ESCAPE '\\'))
            """;

    /**
     * Ordena por el valor del campo según su tipo JSON (números, sí/no, textos sin distinguir mayúsculas), con los
     * registros que no tienen el dato al final; con campo nulo todas las claves son nulas y desempata la carga.
     */
    String ORDEN_ASCENDENTE = """
            ORDER BY
              CASE jsonb_typeof(r.valores -> CAST(:campoOrden AS text))
                WHEN 'number' THEN CAST(r.valores ->> CAST(:campoOrden AS text) AS numeric) END ASC NULLS LAST,
              CASE jsonb_typeof(r.valores -> CAST(:campoOrden AS text))
                WHEN 'boolean' THEN CAST(r.valores ->> CAST(:campoOrden AS text) AS boolean) END ASC NULLS LAST,
              CASE jsonb_typeof(r.valores -> CAST(:campoOrden AS text))
                WHEN 'string' THEN lower(r.valores ->> CAST(:campoOrden AS text)) END ASC NULLS LAST,
              r.creado_en ASC, r.id ASC
            """;

    String ORDEN_DESCENDENTE = """
            ORDER BY
              CASE jsonb_typeof(r.valores -> CAST(:campoOrden AS text))
                WHEN 'number' THEN CAST(r.valores ->> CAST(:campoOrden AS text) AS numeric) END DESC NULLS LAST,
              CASE jsonb_typeof(r.valores -> CAST(:campoOrden AS text))
                WHEN 'boolean' THEN CAST(r.valores ->> CAST(:campoOrden AS text) AS boolean) END DESC NULLS LAST,
              CASE jsonb_typeof(r.valores -> CAST(:campoOrden AS text))
                WHEN 'string' THEN lower(r.valores ->> CAST(:campoOrden AS text)) END DESC NULLS LAST,
              r.creado_en DESC, r.id DESC
            """;

    Optional<RegistroEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    @Query(value = "SELECT r.* " + FILTRADOS + ORDEN_ASCENDENTE,
            countQuery = "SELECT count(*) " + FILTRADOS,
            nativeQuery = true)
    Page<RegistroEntity> buscarAscendente(@Param("negocioId") UUID negocioId,
                                          @Param("bloqueId") UUID bloqueId,
                                          @Param("patron") String patron,
                                          @Param("campoOrden") String campoOrden,
                                          Pageable pageable);

    @Query(value = "SELECT r.* " + FILTRADOS + ORDEN_DESCENDENTE,
            countQuery = "SELECT count(*) " + FILTRADOS,
            nativeQuery = true)
    Page<RegistroEntity> buscarDescendente(@Param("negocioId") UUID negocioId,
                                           @Param("bloqueId") UUID bloqueId,
                                           @Param("patron") String patron,
                                           @Param("campoOrden") String campoOrden,
                                           Pageable pageable);
}

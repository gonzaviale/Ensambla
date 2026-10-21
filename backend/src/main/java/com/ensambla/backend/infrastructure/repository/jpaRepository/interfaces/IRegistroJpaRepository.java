package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
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

    /**
     * Suma por campo de los valores numéricos de los registros filtrados (mismo filtro que el listado, sin paginar).
     * Un registro que no cargó el dato no suma; un campo sin ningún dato numérico queda en cero.
     */
    String SUMAS_POR_CAMPO = """
            SELECT t.key AS campo,
                   COALESCE(SUM(CASE WHEN jsonb_typeof(t.value) = 'number'
                                     THEN CAST(CAST(t.value AS text) AS numeric) END), 0) AS suma
            FROM (SELECT r.valores AS valores
            """ + FILTRADOS + """
            ) f
            CROSS JOIN LATERAL jsonb_each(f.valores) AS t(key, value)
            WHERE t.key IN (:campoIds)
            GROUP BY t.key
            """;

    /**
     * Registros de un reporte: los del bloque que cumplen el texto, se cargaron en [{@code desde}, {@code hasta}) y
     * tienen {@code valorFiltro} en el campo {@code campoFiltro}; cada filtro nulo no se aplica. Las fechas viajan
     * como instantes ISO.
     */
    String FILTRADOS_REPORTE = """
            FROM registro r
            WHERE r.negocio_id = :negocioId
              AND r.bloque_id = :bloqueId
              AND (CAST(:patron AS text) IS NULL OR EXISTS (
                    SELECT 1 FROM jsonb_each_text(r.valores) AS v
                    WHERE v.value ILIKE CAST(:patron AS text) ESCAPE '\\'))
              AND (CAST(:desde AS text) IS NULL OR r.creado_en >= CAST(CAST(:desde AS text) AS timestamptz))
              AND (CAST(:hasta AS text) IS NULL OR r.creado_en < CAST(CAST(:hasta AS text) AS timestamptz))
              AND (CAST(:campoFiltro AS text) IS NULL
                   OR r.valores ->> CAST(:campoFiltro AS text) = CAST(:valorFiltro AS text))
            """;

    /** Igual que {@link #SUMAS_POR_CAMPO} pero con el filtro del reporte. */
    String SUMAS_POR_CAMPO_REPORTE = """
            SELECT t.key AS campo,
                   COALESCE(SUM(CASE WHEN jsonb_typeof(t.value) = 'number'
                                     THEN CAST(CAST(t.value AS text) AS numeric) END), 0) AS suma
            FROM (SELECT r.valores AS valores
            """ + FILTRADOS_REPORTE + """
            ) f
            CROSS JOIN LATERAL jsonb_each(f.valores) AS t(key, value)
            WHERE t.key IN (:campoIds)
            GROUP BY t.key
            """;

    Optional<RegistroEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Filas de todos los bloques del negocio, en el orden que la persona les dio dentro de cada bloque. */
    List<RegistroEntity> findAllByNegocioIdOrderByOrdenAscCreadoEnAscIdAsc(UUID negocioId);

    List<RegistroEntity> findAllByBloqueIdInAndNegocioId(Collection<UUID> bloqueIds, UUID negocioId);

    List<RegistroEntity> findAllByBloqueIdAndNegocioIdOrderByCreadoEnAscIdAsc(UUID bloqueId, UUID negocioId);

    List<RegistroEntity> findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
            Collection<UUID> ids, UUID bloqueId, UUID negocioId);

    @Query(value = SUMAS_POR_CAMPO, nativeQuery = true)
    List<SumaPorCampo> sumarNumericos(@Param("negocioId") UUID negocioId,
                                      @Param("bloqueId") UUID bloqueId,
                                      @Param("patron") String patron,
                                      @Param("campoIds") Collection<String> campoIds);

    /** Una fila del resultado de {@link #sumarNumericos}: el id del campo y la suma de sus valores. */
    interface SumaPorCampo {

        String getCampo();

        BigDecimal getSuma();
    }

    @Query(value = "SELECT r.* " + FILTRADOS_REPORTE + " ORDER BY r.creado_en DESC, r.id DESC LIMIT :limite",
            nativeQuery = true)
    List<RegistroEntity> buscarParaReporte(@Param("negocioId") UUID negocioId,
                                           @Param("bloqueId") UUID bloqueId,
                                           @Param("patron") String patron,
                                           @Param("desde") String desde,
                                           @Param("hasta") String hasta,
                                           @Param("campoFiltro") String campoFiltro,
                                           @Param("valorFiltro") String valorFiltro,
                                           @Param("limite") int limite);

    @Query(value = "SELECT count(*) " + FILTRADOS_REPORTE, nativeQuery = true)
    long contarParaReporte(@Param("negocioId") UUID negocioId,
                           @Param("bloqueId") UUID bloqueId,
                           @Param("patron") String patron,
                           @Param("desde") String desde,
                           @Param("hasta") String hasta,
                           @Param("campoFiltro") String campoFiltro,
                           @Param("valorFiltro") String valorFiltro);

    @Query(value = SUMAS_POR_CAMPO_REPORTE, nativeQuery = true)
    List<SumaPorCampo> sumarNumericosParaReporte(@Param("negocioId") UUID negocioId,
                                                 @Param("bloqueId") UUID bloqueId,
                                                 @Param("patron") String patron,
                                                 @Param("desde") String desde,
                                                 @Param("hasta") String hasta,
                                                 @Param("campoFiltro") String campoFiltro,
                                                 @Param("valorFiltro") String valorFiltro,
                                                 @Param("campoIds") Collection<String> campoIds);

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

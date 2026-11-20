package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ProgresoObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IProgresoObjetivoJpaRepository extends JpaRepository<ProgresoObjetivoEntity, UUID> {

    /** Las fotos del objetivo y las de sus mini-objetivos, de la más vieja a la más nueva. */
    List<ProgresoObjetivoEntity> findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(UUID objetivoId);

    /**
     * Guarda la foto del día o, si ya había una del mismo objetivo, mini-objetivo y día, la pisa. Un solo comando:
     * dos pasadas a la vez (o dos instancias) no chocan. El mini-objetivo vacío es el objetivo mayor.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO progreso_objetivo (id, objetivo_id, mini_objetivo_id, dia, valor, medido_en)
            VALUES (:id, :objetivoId, CAST(:miniObjetivoId AS uuid), :dia, :valor, :medidoEn)
            ON CONFLICT ON CONSTRAINT ux_progreso_dia
            DO UPDATE SET valor = EXCLUDED.valor, medido_en = EXCLUDED.medido_en
            """, nativeQuery = true)
    void registrar(UUID id, UUID objetivoId, UUID miniObjetivoId, LocalDate dia, BigDecimal valor,
                   OffsetDateTime medidoEn);
}

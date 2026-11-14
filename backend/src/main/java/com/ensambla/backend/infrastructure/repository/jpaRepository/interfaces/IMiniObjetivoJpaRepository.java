package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMiniObjetivoJpaRepository extends JpaRepository<MiniObjetivoEntity, UUID> {

    /** Nunca lista mini-objetivos de otro negocio. */
    List<MiniObjetivoEntity> findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(
            UUID objetivoId, UUID negocioId);

    /** Los manuales en curso del negocio sin respuesta, del más viejo al más nuevo. Nunca los de otro negocio. */
    @Query("""
            select m from MiniObjetivoEntity m
            where m.negocioId = :negocioId and m.modo = :modo and m.estado = :estado
              and not exists (select 1 from RespuestaMiniObjetivoEntity r where r.miniObjetivoId = m.id)
            order by m.creadoEn asc, m.posicion asc""")
    List<MiniObjetivoEntity> findPendientesDeRespuesta(
            @Param("negocioId") UUID negocioId, @Param("modo") ModoMiniObjetivo modo,
            @Param("estado") EstadoMiniObjetivo estado);

    /** Un mini-objetivo de otro objetivo o de otro negocio no se encuentra, igual que uno que no existe. */
    Optional<MiniObjetivoEntity> findByIdAndObjetivoIdAndNegocioId(UUID id, UUID objetivoId, UUID negocioId);
}

package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAvisoJpaRepository extends JpaRepository<AvisoEntity, UUID> {

    Optional<AvisoEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    Optional<AvisoEntity> findByNegocioIdAndHuella(UUID negocioId, String huella);

    List<AvisoEntity> findByNegocioIdAndTipoAndEstado(UUID negocioId, TipoAviso tipo, EstadoAviso estado);

    List<AvisoEntity> findByNegocioId(UUID negocioId);

    /** Sólo los del negocio: un id de otro negocio no viene. */
    List<AvisoEntity> findByIdInAndNegocioId(Collection<UUID> ids, UUID negocioId);

    List<AvisoEntity> findByNegocioIdAndEstado(UUID negocioId, EstadoAviso estado);

    /** Cuántos avisos en ese estado tiene cada uno de esos negocios; los que no tienen ninguno no vienen. */
    @Query("""
            select a.negocioId as negocioId, count(a) as cantidad
            from AvisoEntity a
            where a.negocioId in :negocioIds
              and a.estado = :estado
            group by a.negocioId
            """)
    List<AvisosPorNegocio> contarPorNegocio(@Param("negocioIds") Collection<UUID> negocioIds,
                                            @Param("estado") EstadoAviso estado);

    interface AvisosPorNegocio {

        UUID getNegocioId();

        long getCantidad();
    }
}

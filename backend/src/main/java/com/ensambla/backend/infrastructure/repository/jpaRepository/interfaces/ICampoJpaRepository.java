package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICampoJpaRepository extends JpaRepository<CampoEntity, UUID> {

    List<CampoEntity> findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(UUID bloqueId, UUID negocioId);

    List<CampoEntity> findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(List<UUID> bloqueIds, UUID negocioId);

    Optional<CampoEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Campos de otros bloques del negocio que apuntan a {@code bloqueId}; la autorreferencia no cuenta. */
    @Query("""
            select c from CampoEntity c
            where c.negocioId = :negocioId
              and c.bloqueRelacionadoId = :bloqueId
              and c.bloqueId <> :bloqueId
            order by c.bloqueId, c.posicion, c.id
            """)
    List<CampoEntity> findRelacionesHacia(UUID bloqueId, UUID negocioId);

    /** Borra sólo el campo: los registros no tienen FK al campo, sus valores quedan en el JSON sin usarse. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CampoEntity c where c.id = :id and c.negocioId = :negocioId")
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}

package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.infrastructure.repository.entity.OrdenCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IOrdenCompraJpaRepository extends JpaRepository<OrdenCompraEntity, UUID> {

    List<OrdenCompraEntity> findTop100ByNegocioIdOrderByCreadoEnDescIdDesc(UUID negocioId);

    List<OrdenCompraEntity> findTop100ByNegocioIdAndEstadoOrderByCreadoEnDescIdDesc(UUID negocioId,
                                                                                    EstadoOrdenCompra estado);

    Optional<OrdenCompraEntity> findByIdAndNegocioId(UUID id, UUID negocioId);
}

package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.OrdenCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IOrdenCompraJpaRepository extends JpaRepository<OrdenCompraEntity, UUID> {
}

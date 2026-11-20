package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ResumenSemanalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IResumenSemanalJpaRepository extends JpaRepository<ResumenSemanalEntity, UUID> {

    /** Del más nuevo al más viejo. */
    List<ResumenSemanalEntity> findAllByObjetivoIdOrderBySemanaInicioDesc(UUID objetivoId);
}

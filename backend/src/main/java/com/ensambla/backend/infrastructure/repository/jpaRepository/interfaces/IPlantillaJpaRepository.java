package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPlantillaJpaRepository extends JpaRepository<PlantillaEntity, UUID> {

    List<PlantillaEntity> findByActivaTrueOrderByNombreAscIdAsc();

    Optional<PlantillaEntity> findByIdAndActivaTrue(UUID id);
}

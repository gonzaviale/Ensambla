package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.zona.Zona;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZonaRepository {

    List<Zona> findAll();

    Optional<Zona> findById(UUID id);
}

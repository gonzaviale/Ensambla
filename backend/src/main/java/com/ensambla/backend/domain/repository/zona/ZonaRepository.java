package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.util.List;
import java.util.Optional;

public interface ZonaRepository {

    List<Zona> findAll();

    Optional<Zona> findById(ZonaId id);
}

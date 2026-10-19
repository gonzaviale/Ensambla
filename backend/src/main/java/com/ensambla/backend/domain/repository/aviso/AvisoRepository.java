package com.ensambla.backend.domain.repository.aviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvisoRepository {

    Aviso save(Aviso aviso);

    Optional<Aviso> findByIdAndNegocioId(UUID id, UUID negocioId);

    Optional<Aviso> findByNegocioIdAndHuella(UUID negocioId, String huella);

    List<Aviso> findByNegocioIdAndTipoAndEstado(UUID negocioId, TipoAviso tipo, EstadoAviso estado);

    List<Aviso> findByNegocioId(UUID negocioId);

    List<Aviso> findByNegocioIdAndEstado(UUID negocioId, EstadoAviso estado);
}

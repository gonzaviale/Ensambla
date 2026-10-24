package com.ensambla.backend.domain.repository.aviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.List;
import java.util.Optional;

public interface AvisoRepository {

    Aviso save(Aviso aviso);

    Optional<Aviso> findByIdAndNegocioId(AvisoId id, NegocioId negocioId);

    Optional<Aviso> findByNegocioIdAndHuella(NegocioId negocioId, String huella);

    List<Aviso> findByNegocioIdAndTipoAndEstado(NegocioId negocioId, TipoAviso tipo, EstadoAviso estado);

    List<Aviso> findByNegocioId(NegocioId negocioId);

    List<Aviso> findByNegocioIdAndEstado(NegocioId negocioId, EstadoAviso estado);
}

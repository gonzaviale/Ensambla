package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import lombok.RequiredArgsConstructor;

/** Lo comparten aplicar y descartar: el aviso pedido, sólo si es del negocio de la sesión. */
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository avisoRepository;

    public Aviso obtener(AvisoId avisoId, NegocioId negocioId) {
        return avisoRepository.findByIdAndNegocioId(avisoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El aviso no existe"));
    }
}

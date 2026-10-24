package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Lo comparten aplicar y descartar: el aviso pedido, sólo si es del negocio de la sesión. */
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository avisoRepository;

    public Aviso obtener(UUID avisoId, UUID negocioId) {
        return avisoRepository.findByIdAndNegocioId(avisoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El aviso no existe"));
    }
}

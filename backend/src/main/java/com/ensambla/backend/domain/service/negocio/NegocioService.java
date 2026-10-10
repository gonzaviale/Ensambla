package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NegocioService {

    private final NegocioRepository negocioRepository;

    /** Negocio de la sesión: en el MVP 2 cada usuario es dueño de un único negocio. */
    public UUID obtenerIdDelUsuario(UUID usuarioId) {
        return negocioRepository.findByDuenioId(usuarioId)
                .map(Negocio::getId)
                .orElseThrow(() -> new ResourceNotFoundException("El negocio no existe"));
    }
}

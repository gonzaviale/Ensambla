package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class NegocioService {

    private final NegocioRepository negocioRepository;

    public UUID obtenerDelUsuario(UUID usuarioId) {
        return negocioRepository.findIdByDuenioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
    }

    public Negocio obtenerConfiguracion(UUID usuarioId) {
        return negocioRepository.findByDuenioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
    }
}


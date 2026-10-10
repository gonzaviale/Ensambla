package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
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

    public UUID obtenerDelUsuario(UUID usuarioId) {
        return negocioRepository.findIdByDuenioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
    }
}


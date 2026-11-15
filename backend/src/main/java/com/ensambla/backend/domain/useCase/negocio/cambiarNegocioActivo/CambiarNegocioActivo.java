package com.ensambla.backend.domain.useCase.negocio.cambiarNegocioActivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CambiarNegocioActivo {

    private final NegocioRepository negocioRepository;

    private final NegocioActivoPort negocioActivoPort;

    public void execute(CambiarNegocioActivoRequest request) {
        negocioRepository.findByIdAndDuenioId(request.negocioId(), request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado"));
        negocioActivoPort.activar(request.usuarioId(), request.negocioId());
    }
}

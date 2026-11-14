package com.ensambla.backend.domain.useCase.caja.obtenerCierre;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerCierre {

    private final NegocioRepository negocioRepository;
    private final CierreCajaRepository cierreCajaRepository;

    public CierreCajaItem execute(ObtenerCierreRequest request) {
        return CierreCajaItem.de(cierreCajaRepository.findByIdAndNegocioId(
                request.id(), negocioRepository.obtenerIdDelUsuario(request.usuarioId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cierre no encontrado")));
    }
}

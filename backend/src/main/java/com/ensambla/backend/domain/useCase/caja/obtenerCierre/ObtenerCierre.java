package com.ensambla.backend.domain.useCase.caja.obtenerCierre;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerCierre {

    private final NegocioRepository negocioRepository;
    private final CierreCajaRepository cierreCajaRepository;

    public CierreCajaItem execute(ObtenerCierreRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return CierreCajaItem.de(obtenerCierre(request.id(), negocioId));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private CierreCaja obtenerCierre(CierreCajaId id, NegocioId negocioId) {
        return cierreCajaRepository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Cierre no encontrado"));
    }
}

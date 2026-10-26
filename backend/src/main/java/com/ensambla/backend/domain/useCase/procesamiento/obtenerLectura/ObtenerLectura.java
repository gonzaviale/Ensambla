package com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerLectura {

    private final NegocioRepository negocioRepository;
    private final ProcesamientoRepository procesamientoRepository;

    public LecturaItem execute(UsuarioLoteRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return LecturaItem.de(procesamientoRepository.obtenerEstado(negocioId, request.loteId()));
    }
}

package com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura;

import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerLectura {

    private final NegocioService negocioService;
    private final ProcesamientoService procesamientoService;

    public LecturaItem execute(UsuarioLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        return LecturaItem.de(procesamientoService.obtener(negocioId, request.loteId()));
    }
}

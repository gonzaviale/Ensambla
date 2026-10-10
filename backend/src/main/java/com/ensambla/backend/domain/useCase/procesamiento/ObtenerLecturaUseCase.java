package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LecturaItem;
import com.ensambla.backend.domain.dto.procesamiento.UsuarioLoteRequest;
import com.ensambla.backend.domain.mapper.procesamiento.ObtenerLecturaMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerLecturaUseCase {

    private final NegocioService negocioService;
    private final ProcesamientoService procesamientoService;
    private final ObtenerLecturaMapper obtenerLecturaMapper;

    public LecturaItem execute(UsuarioLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        return obtenerLecturaMapper.toResponse(procesamientoService.obtener(negocioId, request.loteId()));
    }
}

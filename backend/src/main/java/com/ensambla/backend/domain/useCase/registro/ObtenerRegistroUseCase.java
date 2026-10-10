package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.mapper.registro.ObtenerRegistroMapper;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerRegistroUseCase {

    private final NegocioService negocioService;
    private final RegistroService registroService;
    private final ObtenerRegistroMapper mapper;

    public ObtenerRegistroResponse execute(ObtenerRegistroRequest request) {
        UUID negocioId = negocioService.obtenerIdDelUsuario(request.usuarioId());
        Registro registro = registroService.obtener(request.registroId(), negocioId);
        return mapper.toResponse(registro);
    }
}

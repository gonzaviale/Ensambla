package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.mapper.registro.ObtenerRegistroMapper;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerRegistroUseCase {

    private final NegocioService negocioService;
    private final CampoService campoService;
    private final RegistroDatosService registroDatosService;
    private final ObtenerRegistroMapper mapper;

    public ObtenerRegistroResponse execute(ObtenerRegistroRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro registro = registroDatosService.obtener(request.registroId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(registro.getBloqueId(), negocioId);
        return mapper.toResponse(registro.conFormulas(campos));
    }
}

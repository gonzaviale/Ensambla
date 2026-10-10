package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.EliminarRegistroRequest;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarRegistroUseCase {

    private final NegocioService negocioService;
    private final RegistroService registroService;

    public void execute(EliminarRegistroRequest request) {
        UUID negocioId = negocioService.obtenerIdDelUsuario(request.usuarioId());
        Registro registro = registroService.obtener(request.registroId(), negocioId);
        registroService.eliminar(registro);
    }
}

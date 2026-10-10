package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.EliminarRegistroRequest;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarRegistroUseCase {

    private final NegocioService negocioService;
    private final RegistroDatosService registroDatosService;

    public void execute(EliminarRegistroRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro registro = registroDatosService.obtener(request.registroId(), negocioId);
        registroDatosService.eliminar(registro);
    }
}

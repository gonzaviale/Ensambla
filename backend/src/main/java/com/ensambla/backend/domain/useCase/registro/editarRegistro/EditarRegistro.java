package com.ensambla.backend.domain.useCase.registro.editarRegistro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class EditarRegistro {

    private final NegocioService negocioService;
    private final CampoService campoService;
    private final RegistroDatosService registroDatosService;
    private final Clock clock;

    public EditarRegistroResponse execute(EditarRegistroRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro registro = registroDatosService.obtener(request.registroId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(registro.getBloqueId(), negocioId);
        Registro editado = registroDatosService.editar(registro, campos, request.valores(), OffsetDateTime.now(clock));
        return EditarRegistroResponse.de(editado.conFormulas(campos));
    }
}

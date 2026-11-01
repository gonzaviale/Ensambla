package com.ensambla.backend.domain.useCase.registro.editarRegistro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class EditarRegistro {

    private final NegocioRepository negocioRepository;
    private final CampoRepository campoRepository;
    private final RegistroDatosService registroDatosService;

    private final RegistroRepository registroRepository;
    private final Clock clock;

    public EditarRegistroResponse execute(EditarRegistroRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Registro registro = registroRepository.obtener(request.registroId(), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(registro.getBloqueId(), negocioId);
        Registro editado = registroDatosService.editar(registro, campos, request.valores(), OffsetDateTime.now(clock));
        return EditarRegistroResponse.de(editado.conFormulas(campos));
    }
}

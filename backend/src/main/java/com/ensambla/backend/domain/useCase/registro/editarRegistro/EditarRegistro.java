package com.ensambla.backend.domain.useCase.registro.editarRegistro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class EditarRegistro {

    private final NegocioRepository negocioRepository;
    private final CampoRepository campoRepository;
    private final RegistroDatosService registroDatosService;

    private final RegistroRepository registroRepository;
    private final Clock clock;

    public EditarRegistroResponse execute(EditarRegistroRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Registro registro = obtenerRegistro(request.registroId(), negocioId);
        List<Campo> campos = obtenerCamposDelBloqueDe(registro, negocioId);
        Registro editado = editar(registro, campos, request.valores());
        return EditarRegistroResponse.de(editado.conFormulas(campos));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Registro obtenerRegistro(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.obtener(registroId, negocioId);
    }

    private List<Campo> obtenerCamposDelBloqueDe(Registro registro, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(registro.getBloqueId(), negocioId);
    }

    private Registro editar(Registro registro, List<Campo> campos, Map<String, Object> valores) {
        return registroDatosService.editar(registro, campos, valores, OffsetDateTime.now(clock));
    }
}

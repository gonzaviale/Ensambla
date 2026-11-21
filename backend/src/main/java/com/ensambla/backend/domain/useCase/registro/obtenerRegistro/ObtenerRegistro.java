package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ObtenerRegistro {

    private final NegocioRepository negocioRepository;
    private final CampoRepository campoRepository;
    private final RegistroRepository registroRepository;

    public ObtenerRegistroResponse execute(ObtenerRegistroRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Registro registro = obtenerRegistro(request.registroId(), negocioId);
        List<Campo> campos = obtenerCamposDelBloqueDe(registro, negocioId);
        return ObtenerRegistroResponse.de(registro.conFormulas(campos));
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
}

package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Registro registro = registroRepository.obtener(request.registroId(), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(registro.getBloqueId(), negocioId);
        return ObtenerRegistroResponse.de(registro.conFormulas(campos));
    }
}

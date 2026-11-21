package com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ObtenerImpactoAvisos {

    private final NegocioRepository negocioRepository;
    private final AvisoRepository avisoRepository;

    public ImpactoAvisos execute(UsuarioId usuarioId) {
        NegocioId negocioId = obtenerNegocioDelUsuario(usuarioId);
        return ImpactoAvisos.de(obtenerAvisos(negocioId));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<Aviso> obtenerAvisos(NegocioId negocioId) {
        return avisoRepository.findByNegocioId(negocioId);
    }
}

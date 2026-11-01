package com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerImpactoAvisos {

    private final NegocioRepository negocioRepository;
    private final AvisoRepository avisoRepository;

    public ImpactoAvisos execute(UsuarioId usuarioId) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(usuarioId);
        return ImpactoAvisos.de(avisoRepository.findByNegocioId(negocioId));
    }
}

package com.ensambla.backend.domain.useCase.aviso.contarAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ContarAvisos {

    private final NegocioRepository negocioRepository;
    private final AvisoRepository avisoRepository;

    public ContadorAvisos execute(UsuarioId usuarioId) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(usuarioId);
        List<Aviso> pendientes = avisoRepository.findByNegocioIdAndEstado(negocioId, EstadoAviso.PENDIENTE);
        return new ContadorAvisos(pendientes.size(), pendientes.stream().filter(Aviso::esUrgente).count());
    }
}

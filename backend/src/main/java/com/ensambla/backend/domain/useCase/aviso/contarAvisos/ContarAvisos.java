package com.ensambla.backend.domain.useCase.aviso.contarAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ContarAvisos {

    private final NegocioService negocioService;
    private final AvisoRepository avisoRepository;

    public ContadorAvisos execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        List<Aviso> pendientes = avisoRepository.findByNegocioIdAndEstado(negocioId, EstadoAviso.PENDIENTE);
        return new ContadorAvisos(pendientes.size(), pendientes.stream().filter(Aviso::esUrgente).count());
    }
}

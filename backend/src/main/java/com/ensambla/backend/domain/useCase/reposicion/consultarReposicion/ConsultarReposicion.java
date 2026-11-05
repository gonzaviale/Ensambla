package com.ensambla.backend.domain.useCase.reposicion.consultarReposicion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.reposicion.ReposicionService;
import lombok.RequiredArgsConstructor;

/** Qué productos de un sistema hay que reponer y cuánto pedir de cada uno, agrupados por proveedor. */
@RequiredArgsConstructor
public class ConsultarReposicion {

    private final NegocioRepository negocioRepository;

    private final ReposicionService reposicionService;

    public ConsultarReposicionResponse execute(ConsultarReposicionRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Faltantes faltantes = reposicionService.faltantesDe(negocioId, request.proyectoId());
        return ConsultarReposicionResponse.de(faltantes);
    }
}

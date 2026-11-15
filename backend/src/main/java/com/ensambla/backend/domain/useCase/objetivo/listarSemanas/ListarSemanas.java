package com.ensambla.backend.domain.useCase.objetivo.listarSemanas;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import lombok.RequiredArgsConstructor;

/**
 * Los resúmenes del domingo de un objetivo del negocio de la sesión, con los reajustes de cada semana. Un objetivo
 * ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class ListarSemanas {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final ResumenSemanalRepository resumenSemanalRepository;

    public ListarSemanasResponse execute(ListarSemanasRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Objetivo objetivo = objetivoRepository.obtener(negocioId, request.objetivoId());
        return ListarSemanasResponse.de(resumenSemanalRepository.findAllByObjetivoId(objetivo.getId()));
    }
}

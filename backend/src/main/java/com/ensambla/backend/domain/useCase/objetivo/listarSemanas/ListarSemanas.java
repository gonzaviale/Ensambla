package com.ensambla.backend.domain.useCase.objetivo.listarSemanas;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Objetivo objetivo = obtenerObjetivo(negocioId, request.objetivoId());
        return ListarSemanasResponse.de(obtenerResumenes(objetivo));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Objetivo obtenerObjetivo(NegocioId negocioId, ObjetivoId objetivoId) {
        return objetivoRepository.obtener(negocioId, objetivoId);
    }

    private List<ResumenSemanal> obtenerResumenes(Objetivo objetivo) {
        return resumenSemanalRepository.findAllByObjetivoId(objetivo.getId());
    }
}

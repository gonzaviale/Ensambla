package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

/** Los mini-objetivos de un objetivo del negocio de la sesión; un objetivo ajeno responde como inexistente. */
@RequiredArgsConstructor
public class ListarMiniObjetivos {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    public ListarMiniObjetivosResponse execute(ListarMiniObjetivosRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Objetivo objetivo = objetivoRepository.obtener(negocioId, request.objetivoId());
        return ListarMiniObjetivosResponse.de(
                objetivo, miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(request.objetivoId(), negocioId));
    }
}

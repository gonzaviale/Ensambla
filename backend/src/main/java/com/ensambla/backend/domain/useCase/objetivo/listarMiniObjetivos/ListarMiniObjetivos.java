package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Los mini-objetivos de un objetivo del negocio de la sesión; un objetivo ajeno responde como inexistente. */
@RequiredArgsConstructor
public class ListarMiniObjetivos {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    public ListarMiniObjetivosResponse execute(ListarMiniObjetivosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Objetivo objetivo = obtenerObjetivo(negocioId, request.objetivoId());
        return ListarMiniObjetivosResponse.de(objetivo, obtenerMiniObjetivos(negocioId, request.objetivoId()));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Objetivo obtenerObjetivo(NegocioId negocioId, ObjetivoId objetivoId) {
        return objetivoRepository.obtener(negocioId, objetivoId);
    }

    private List<MiniObjetivo> obtenerMiniObjetivos(NegocioId negocioId, ObjetivoId objetivoId) {
        return miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivoId, negocioId);
    }
}

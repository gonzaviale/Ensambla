package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las decisiones de la persona frente a un objetivo del negocio de la sesión, por semana, con el efecto de los avisos
 * aplicados cuando ya se midió. Los avisos se leen todos de una vez. Un objetivo ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class ListarDecisiones {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final DecisionObjetivoRepository decisionObjetivoRepository;

    private final AvisoRepository avisoRepository;

    public ListarDecisionesResponse execute(ListarDecisionesRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        objetivoRepository.obtener(negocioId, request.objetivoId());
        List<DecisionObjetivo> decisiones =
                decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(request.objetivoId(), negocioId);
        return ListarDecisionesResponse.de(decisiones, avisosDe(decisiones, negocioId));
    }

    private Map<AvisoId, Aviso> avisosDe(List<DecisionObjetivo> decisiones, NegocioId negocioId) {
        List<AvisoId> ids = decisiones.stream().map(DecisionObjetivo::avisoId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return avisoRepository.findAllByIdsAndNegocioId(ids, negocioId).stream()
                .collect(Collectors.toMap(Aviso::getId, Function.identity()));
    }
}

package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        exigirObjetivoDelNegocio(negocioId, request.objetivoId());
        List<DecisionObjetivo> decisiones = obtenerDecisiones(negocioId, request.objetivoId());
        return ListarDecisionesResponse.de(decisiones, avisosDe(decisiones, negocioId));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirObjetivoDelNegocio(NegocioId negocioId, ObjetivoId objetivoId) {
        objetivoRepository.obtener(negocioId, objetivoId);
    }

    private List<DecisionObjetivo> obtenerDecisiones(NegocioId negocioId, ObjetivoId objetivoId) {
        return decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivoId, negocioId);
    }

    private Map<AvisoId, Aviso> avisosDe(List<DecisionObjetivo> decisiones, NegocioId negocioId) {
        List<AvisoId> ids = decisiones.stream().map(DecisionObjetivo::avisoId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return obtenerAvisos(ids, negocioId).stream()
                .collect(Collectors.toMap(Aviso::getId, Function.identity()));
    }

    private List<Aviso> obtenerAvisos(List<AvisoId> ids, NegocioId negocioId) {
        return avisoRepository.findAllByIdsAndNegocioId(ids, negocioId);
    }
}

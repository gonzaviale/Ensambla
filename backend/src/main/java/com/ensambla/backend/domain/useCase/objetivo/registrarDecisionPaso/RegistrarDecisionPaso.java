package com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.EfectoDecision;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Anota que la persona aceptó («Lo hago») o ignoró («Ahora no») el paso de un mini-objetivo en curso. No cambia nada del
 * cuaderno: Ensambla no aplica nada sin la persona. Un objetivo o mini-objetivo ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class RegistrarDecisionPaso {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final DecisionObjetivoRepository decisionObjetivoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public DecisionItem execute(RegistrarDecisionPasoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        exigirObjetivoDelNegocio(negocioId, request.objetivoId());
        MiniObjetivo mini = obtenerMiniObjetivo(negocioId, request);
        mini.exigirEnCurso();
        DecisionObjetivo decision = anotarEnTransaccion(mini, request, OffsetDateTime.now(clock));
        return DecisionItem.de(decision, EfectoDecision.SIN_MEDICION);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirObjetivoDelNegocio(NegocioId negocioId, ObjetivoId objetivoId) {
        objetivoRepository.obtener(negocioId, objetivoId);
    }

    private MiniObjetivo obtenerMiniObjetivo(NegocioId negocioId, RegistrarDecisionPasoRequest request) {
        return miniObjetivoRepository.obtener(negocioId, request.objetivoId(), request.miniObjetivoId());
    }

    private DecisionObjetivo anotarEnTransaccion(MiniObjetivo mini, RegistrarDecisionPasoRequest request,
                                                 OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> guardarDecision(DecisionObjetivo.dePaso(mini, request.decision(), ahora)));
    }

    private DecisionObjetivo guardarDecision(DecisionObjetivo decision) {
        return decisionObjetivoRepository.save(decision);
    }
}

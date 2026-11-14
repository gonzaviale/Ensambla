package com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.RespuestaMiniObjetivoRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * La persona cuenta si hizo un mini-objetivo manual y qué pasó. La respuesta queda guardada con su fecha (no se pisa:
 * un manual se responde una sola vez) y anota una decisión en el historial del objetivo. No cambia el estado del
 * mini-objetivo: el cierre del domingo lo renueva. Un objetivo o mini-objetivo ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class ResponderMiniObjetivo {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final RespuestaMiniObjetivoRepository respuestaMiniObjetivoRepository;

    private final DecisionObjetivoRepository decisionObjetivoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ResponderMiniObjetivoResponse execute(ResponderMiniObjetivoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        objetivoRepository.obtener(negocioId, request.objetivoId());
        MiniObjetivo mini = miniObjetivoRepository.obtener(negocioId, request.objetivoId(), request.miniObjetivoId());
        mini.exigirManualEnCurso();
        exigirSinRespuesta(mini);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        RespuestaMiniObjetivo respuesta = RespuestaMiniObjetivo.de(
                mini, request.hizo(), request.quePaso(), request.comentario(), ahora);
        return ResponderMiniObjetivoResponse.de(transaccionPort.ejecutar(() -> guardar(mini, respuesta, ahora)));
    }

    private void exigirSinRespuesta(MiniObjetivo mini) {
        if (respuestaMiniObjetivoRepository.existsByMiniObjetivoId(mini.getId())) {
            throw new DuplicateResourceException("Ya respondiste este mini-objetivo");
        }
    }

    private RespuestaMiniObjetivo guardar(MiniObjetivo mini, RespuestaMiniObjetivo respuesta, OffsetDateTime ahora) {
        decisionObjetivoRepository.save(DecisionObjetivo.deRespuesta(mini, respuesta, ahora));
        return respuestaMiniObjetivoRepository.save(respuesta);
    }
}

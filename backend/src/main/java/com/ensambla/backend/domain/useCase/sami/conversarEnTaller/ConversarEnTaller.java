package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sami.TallerSamiService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConversarEnTaller {

    private final NegocioService negocioService;

    private final TallerSamiService tallerSamiService;

    private final Clock clock;

    private final int limiteDiario;

    public ConversarEnTallerResponse execute(ConversarEnTallerRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        RespuestaTaller respuesta = tallerSamiService.conversar(
                negocioId,
                request.mensaje(),
                request.imagenTaller(),
                request.estadoTaller(),
                request.historialTaller(),
                OffsetDateTime.now(clock),
                limiteDiario);
        return ConversarEnTallerResponse.de(respuesta);
    }
}

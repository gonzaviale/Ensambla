package com.ensambla.backend.domain.useCase.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerRequest;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.domain.mapper.sami.ConversarEnTallerMapper;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sami.TallerSamiService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConversarEnTallerUseCase {

    private final NegocioService negocioService;

    private final TallerSamiService tallerSamiService;

    private final ConversarEnTallerMapper conversarEnTallerMapper;

    private final Clock clock;

    private final int limiteDiario;

    public ConversarEnTallerResponse execute(ConversarEnTallerRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        RespuestaTaller respuesta = tallerSamiService.conversar(
                negocioId,
                request.mensaje(),
                conversarEnTallerMapper.toImagen(request.imagen()),
                conversarEnTallerMapper.toEstado(request.estado()),
                conversarEnTallerMapper.toHistorial(request.historial()),
                OffsetDateTime.now(clock),
                limiteDiario);
        return conversarEnTallerMapper.toResponse(respuesta);
    }
}

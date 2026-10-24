package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TipoPaso;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.sami.ConversacionSamiService;
import com.ensambla.backend.domain.service.sami.RespaldoIaSamiService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ConversarConSami {

    private final NegocioService negocioService;

    private final PlantillaService plantillaService;

    private final ConversacionSamiService conversacionSamiService;

    private final RespaldoIaSamiService respaldoIaSamiService;

    private final Clock clock;

    private final double umbralConfianza;

    private final int maxPreguntas;

    private final int limiteDiarioIa;

    public ConversarConSamiResponse execute(ConversarConSamiRequest request) {
        List<PlantillaProyecto> plantillas = plantillaService.listarParaProyecto();
        List<RespuestaSami> respuestas = request.respuestasSami();
        PasoConversacionSami paso = conversacionSamiService.siguientePaso(
                request.texto(), respuestas, plantillas, umbralConfianza, maxPreguntas);
        if (paso.tipo() != TipoPaso.RESPALDO_IA) {
            return ConversarConSamiResponse.de(paso);
        }
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        InterpretacionSami propuesta = respaldoIaSamiService.proponer(
                negocioId, request.texto(), respuestas, plantillas, OffsetDateTime.now(clock), limiteDiarioIa);
        return ConversarConSamiResponse.de(
                propuesta, PropuestaSamiItem.ORIGEN_IA, paso.preguntasHechas());
    }
}

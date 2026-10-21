package com.ensambla.backend.domain.mapper.sami;

import com.ensambla.backend.domain.dto.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.dto.sami.ConversarConSamiResponse;
import com.ensambla.backend.domain.dto.sami.OpcionSamiItem;
import com.ensambla.backend.domain.dto.sami.PreguntaSamiItem;
import com.ensambla.backend.domain.dto.sami.PropuestaSamiItem;
import com.ensambla.backend.domain.dto.sami.RespuestaSamiItem;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.PreguntaSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;

import java.util.List;

public class ConversarConSamiMapper {

    public static final String ORIGEN_ALGORITMO = "ALGORITMO";
    public static final String ORIGEN_IA = "IA";

    public List<RespuestaSami> toRespuestas(List<RespuestaSamiItem> respuestas) {
        return respuestas.stream()
                .map(respuesta -> new RespuestaSami(respuesta.preguntaId(), respuesta.opcionId()))
                .toList();
    }

    public ConversarConSamiResponse toResponse(PasoConversacionSami paso) {
        PreguntaSamiItem pregunta = paso.pregunta() == null ? null : toItem(paso.pregunta());
        PropuestaSamiItem propuesta = switch (paso.tipo()) {
            case PROPUESTA -> toPropuesta(paso.interpretacion(), ORIGEN_ALGORITMO);
            case PREGUNTA, RESPALDO_IA -> null;
        };
        return new ConversarConSamiResponse(paso.tipo().name(), pregunta, propuesta, paso.preguntasHechas());
    }

    public ConversarConSamiResponse toResponse(InterpretacionSami interpretacion, String origen, int preguntasHechas) {
        return new ConversarConSamiResponse(
                "PROPUESTA", null, toPropuesta(interpretacion, origen), preguntasHechas);
    }

    private PropuestaSamiItem toPropuesta(InterpretacionSami interpretacion, String origen) {
        return new PropuestaSamiItem(
                interpretacion.plantillaId(),
                interpretacion.plantilla(),
                interpretacion.columnasExtra().stream().map(this::toItem).toList(),
                interpretacion.confianza(),
                origen);
    }

    private ColumnaSamiItem toItem(ColumnaSugerida columna) {
        return new ColumnaSamiItem(columna.nombre(), columna.tipo().name(), columna.opciones());
    }

    private PreguntaSamiItem toItem(PreguntaSami pregunta) {
        return new PreguntaSamiItem(
                pregunta.id(),
                pregunta.texto(),
                pregunta.opciones().stream().map(opcion -> new OpcionSamiItem(opcion.id(), opcion.texto())).toList());
    }
}

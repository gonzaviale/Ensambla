package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.OpcionSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PreguntaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PropuestaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.RespuestaSamiItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarSamiResponse;
import com.ensambla.backend.infrastructure.presentation.dto.sami.CrearProyectoSamiRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SamiPresentationMapper {

    public ConversarConSamiRequest toRequest(UsuarioId usuarioId, ConversarSamiRequest body) {
        List<RespuestaSamiItem> respuestas = body.respuestas() == null
                ? List.of()
                : body.respuestas().stream()
                        .map(respuesta -> new RespuestaSamiItem(respuesta.preguntaId(), respuesta.opcionId()))
                        .toList();
        return new ConversarConSamiRequest(usuarioId, body.texto().trim(), respuestas);
    }

    public CrearProyectoDesdeSamiRequest toCrearProyectoRequest(UsuarioId usuarioId, CrearProyectoSamiRequest body) {
        List<ColumnaSamiItem> columnas = body.columnas().stream()
                .map(columna -> new ColumnaSamiItem(columna.nombre(), columna.tipo(), columna.opciones()))
                .toList();
        return new CrearProyectoDesdeSamiRequest(usuarioId, body.plantillaId(), body.nombre(), columnas);
    }

    public ConversarSamiResponse toResponse(ConversarConSamiResponse response) {
        return new ConversarSamiResponse(
                response.tipo(),
                response.pregunta() == null ? null : toResponse(response.pregunta()),
                response.propuesta() == null ? null : toResponse(response.propuesta()),
                response.preguntasHechas());
    }

    private ConversarSamiResponse.Pregunta toResponse(PreguntaSamiItem pregunta) {
        return new ConversarSamiResponse.Pregunta(
                pregunta.id(),
                pregunta.texto(),
                pregunta.opciones().stream().map(this::toResponse).toList());
    }

    private ConversarSamiResponse.Opcion toResponse(OpcionSamiItem opcion) {
        return new ConversarSamiResponse.Opcion(opcion.id(), opcion.texto());
    }

    private ConversarSamiResponse.Propuesta toResponse(PropuestaSamiItem propuesta) {
        return new ConversarSamiResponse.Propuesta(
                propuesta.plantillaId(),
                propuesta.plantilla(),
                propuesta.columnas().stream().map(this::toResponse).toList(),
                propuesta.confianza(),
                propuesta.origen());
    }

    private ConversarSamiResponse.Columna toResponse(ColumnaSamiItem columna) {
        return new ConversarSamiResponse.Columna(columna.nombre(), columna.tipo(), columna.opciones());
    }
}

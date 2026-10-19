package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerRequest;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem.CampoTallerItem;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem.PiezaTallerItem;
import com.ensambla.backend.domain.dto.sami.MensajeTallerItem;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TallerPresentationMapper {

    public ConversarEnTallerRequest toRequest(UUID usuarioId, ConversarTallerRequest body) {
        List<PiezaTallerItem> piezas = body.estado().piezas().stream()
                .map(pieza -> new PiezaTallerItem(
                        pieza.id(),
                        pieza.tipo(),
                        pieza.nombre(),
                        pieza.seccion(),
                        pieza.campos().stream()
                                .map(campo -> new CampoTallerItem(campo.etiqueta(), campo.tipo()))
                                .toList()))
                .toList();
        List<MensajeTallerItem> historial = body.historial() == null
                ? List.of()
                : body.historial().stream()
                        .map(mensaje -> new MensajeTallerItem(mensaje.quien(), mensaje.texto()))
                        .toList();
        return new ConversarEnTallerRequest(
                usuarioId,
                body.mensaje().trim(),
                new EstadoTallerItem(body.estado().nombre(), body.estado().distribucion(), piezas),
                historial);
    }

    public ConversarTallerResponse toResponse(ConversarEnTallerResponse response) {
        List<ConversarTallerResponse.Accion> acciones = response.acciones().stream()
                .map(accion -> new ConversarTallerResponse.Accion(accion.herramienta(), accion.argumentos()))
                .toList();
        ConversarTallerResponse.Opciones opciones = response.opciones() == null
                ? null
                : new ConversarTallerResponse.Opciones(response.opciones().pregunta(), response.opciones().opciones());
        return new ConversarTallerResponse(response.respuesta(), acciones, opciones);
    }
}

package com.ensambla.backend.infrastructure.presentation.mapper.sami;

import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.CampoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.PiezaTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ImagenTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.MensajeTallerItem;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sami.ConversarTallerResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TallerPresentationMapper {

    public ConversarEnTallerRequest toRequest(UsuarioId usuarioId, ConversarTallerRequest body) {
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
                historial,
                imagen(body.imagen()));
    }

    /** Decodifica el base64; si la foto no vale (tipo, tamaño o datos), {@link ImagenTaller} la rechaza con un 400. */
    private ImagenTallerItem imagen(ConversarTallerRequest.Imagen imagen) {
        if (imagen == null) {
            return null;
        }
        ImagenTaller decodificada = ImagenTaller.desdeBase64(imagen.tipo(), imagen.datos());
        return new ImagenTallerItem(decodificada.tipo(), decodificada.datos());
    }

    public ConversarTallerResponse toResponse(ConversarEnTallerResponse response) {
        List<ConversarTallerResponse.Accion> acciones = response.acciones().stream()
                .map(accion -> new ConversarTallerResponse.Accion(accion.herramienta(), accion.argumentos()))
                .toList();
        List<ConversarTallerResponse.Opciones> preguntas = response.preguntas().stream()
                .map(duda -> new ConversarTallerResponse.Opciones(duda.tema(), duda.pregunta(), duda.opciones()))
                .toList();
        return new ConversarTallerResponse(response.respuesta(), acciones, preguntas);
    }
}

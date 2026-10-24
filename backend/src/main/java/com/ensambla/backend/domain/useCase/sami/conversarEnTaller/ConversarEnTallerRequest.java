package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;

import java.util.List;
import java.util.UUID;

/**
 * @param imagen la foto que adjuntó la persona en este turno; {@code null} si no adjuntó ninguna
 */
public record ConversarEnTallerRequest(
        UUID usuarioId,
        String mensaje,
        EstadoTallerItem estado,
        List<MensajeTallerItem> historial,
        ImagenTallerItem imagen
) {

    /** {@code null} si no adjuntó foto. */
    public ImagenTaller imagenTaller() {
        return imagen == null ? null : imagen.aImagenTaller();
    }

    public EstadoTaller estadoTaller() {
        return estado.aEstadoTaller();
    }

    public List<MensajeTaller> historialTaller() {
        return historial.stream().map(mensaje -> new MensajeTaller(mensaje.quien(), mensaje.texto())).toList();
    }
}

package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Lo que se le manda a la IA en un turno: el mensaje, el estado compacto del sistema, la charla reciente, si se
 * reconoció el rubro su kit y, si la persona adjuntó una, la foto.
 *
 * @param kit    {@code null} si no se reconoció el rubro
 * @param imagen {@code null} si no adjuntó ninguna foto
 */
public record ConsultaTaller(String mensaje, EstadoTaller estado, List<MensajeTaller> historial, KitRubro kit,
                             ImagenTaller imagen) {

    public ConsultaTaller(String mensaje, EstadoTaller estado, List<MensajeTaller> historial, KitRubro kit) {
        this(mensaje, estado, historial, kit, null);
    }

    public ConsultaTaller(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
        this(mensaje, estado, historial, null, null);
    }
}

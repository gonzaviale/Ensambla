package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

/** Un turno de la charla tal como llegó: de quién es, qué dijo la persona y cómo estaba el taller. */
record PedidoDelTurno(NegocioId negocioId, LocalDate fecha, OffsetDateTime ahora, String mensaje,
                      ImagenTaller imagen, EstadoTaller estado, List<MensajeTaller> historial) {

    private static final Pattern PIDE_CARGAR = Pattern.compile("(?iU)\\b(re)?carg\\w*");

    boolean tieneImagen() {
        return imagen != null;
    }

    /** Los ejemplos del kit sólo sirven para imaginar un sistema nuevo: con una foto, o ajustando uno armado, no. */
    boolean sinDatosDeEjemplo() {
        return tieneImagen() || !estado.piezas().isEmpty();
    }

    boolean pideCargar() {
        return PIDE_CARGAR.matcher(mensaje).find();
    }

    /** El estado del taller con las piezas que ya armó este turno. */
    EstadoTaller estadoConPiezas(List<PiezaTaller> piezas) {
        return new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
    }
}

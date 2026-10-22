package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;

import java.util.List;
import java.util.Objects;

/**
 * Pedido al modelo con herramientas disponibles: instrucciones fijas, el contexto del turno y las fotos que adjuntó la
 * persona (van en el mismo mensaje, antes del texto).
 */
public record PeticionConHerramientas(String instrucciones, String contexto, List<ImagenTaller> imagenes) {

    public PeticionConHerramientas {
        imagenes = List.copyOf(Objects.requireNonNullElse(imagenes, List.of()));
    }

    public PeticionConHerramientas(String instrucciones, String contexto) {
        this(instrucciones, contexto, List.of());
    }
}

package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.marca.LogoDeMarca;

import java.util.List;

/** Lee los píxeles de un logo como colores RGB (0xRRGGBB). */
public interface LectorDePixelesPort {

    /** Una muestra de los píxeles del logo; falla con {@code BusinessRuleException} si no es una imagen legible. */
    List<Integer> leer(LogoDeMarca logo);
}

package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;

/** Dibuja un cartel ya compuesto en un formato que se puede mostrar. */
public interface DibujanteDeCartelPort {

    String dibujar(ComposicionDeCartel composicion);
}

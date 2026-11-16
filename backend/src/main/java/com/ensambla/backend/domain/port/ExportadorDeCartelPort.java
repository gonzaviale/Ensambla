package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;

/** Convierte un cartel compuesto en un archivo para descargar. Cada exportador sabe de qué salida se ocupa. */
public interface ExportadorDeCartelPort {

    SalidaDeCartel salida();

    byte[] exportar(ComposicionDeCartel composicion);
}

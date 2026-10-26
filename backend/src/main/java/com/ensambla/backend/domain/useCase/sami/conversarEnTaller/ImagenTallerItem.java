package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;

/** La foto adjunta en un turno del taller, ya decodificada. */
public record ImagenTallerItem(String tipo, byte[] datos) {

    /** Si la foto no vale, {@link ImagenTaller} la rechaza. */
    ImagenTaller aImagenTaller() {
        return new ImagenTaller(tipo, datos);
    }
}

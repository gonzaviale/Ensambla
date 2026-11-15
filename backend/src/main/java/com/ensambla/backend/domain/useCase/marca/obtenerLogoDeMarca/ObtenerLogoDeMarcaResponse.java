package com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca;

import com.ensambla.backend.domain.model.marca.LogoDeMarca;

public record ObtenerLogoDeMarcaResponse(byte[] contenido, String tipoMime) {

    public static ObtenerLogoDeMarcaResponse de(LogoDeMarca logo) {
        return new ObtenerLogoDeMarcaResponse(logo.contenido(), logo.tipoMime());
    }
}

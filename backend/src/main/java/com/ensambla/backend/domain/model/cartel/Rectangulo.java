package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

public record Rectangulo(Zona zona, ColorHex color) implements ElementoDeCartel {

    @Override
    public <T> T aceptar(VisitanteDeElemento<T> visitante) {
        return visitante.visitar(this);
    }
}

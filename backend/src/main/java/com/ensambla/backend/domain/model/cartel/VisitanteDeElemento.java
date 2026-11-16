package com.ensambla.backend.domain.model.cartel;

/** Quien recorre las piezas de un cartel (para dibujarlas, por ejemplo) sin preguntarles de qué clase son. */
public interface VisitanteDeElemento<T> {

    T visitar(Rectangulo rectangulo);

    T visitar(TextoDeCartel texto);
}

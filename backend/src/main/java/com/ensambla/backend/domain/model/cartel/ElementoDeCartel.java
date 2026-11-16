package com.ensambla.backend.domain.model.cartel;

/** Una pieza dibujada del cartel: un rectángulo de color o un texto. */
public sealed interface ElementoDeCartel permits Rectangulo, TextoDeCartel {

    <T> T aceptar(VisitanteDeElemento<T> visitante);
}

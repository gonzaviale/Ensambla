package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;

/** Una de las opciones que se le muestran a la persona: su composición y el dibujo listo para ver. */
public record OpcionDeCartelItem(VarianteDeComposicion variante, String dibujo) {
}

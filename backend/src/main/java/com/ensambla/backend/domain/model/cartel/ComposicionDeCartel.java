package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.model.marca.Tipografia;

import java.util.List;

/** Un cartel ya compuesto: fondo, tipografía y las piezas dibujadas de atrás hacia adelante. */
public record ComposicionDeCartel(FormatoDeCartel formato, VarianteDeComposicion variante, ColorHex fondo,
                                  Tipografia tipografia, List<ElementoDeCartel> elementos) {
}

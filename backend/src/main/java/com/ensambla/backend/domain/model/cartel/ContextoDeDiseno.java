package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.Tipografia;

/** Todo lo que una variante necesita para componer: el formato, los colores, la letra y lo que dice el cartel. */
public record ContextoDeDiseno(FormatoDeCartel formato, PaletaDeCartel paleta, Tipografia tipografia,
                               ContenidoDeCartel contenido) {

    public double ancho() {
        return formato.ancho();
    }

    public double alto() {
        return formato.alto();
    }

    public double margen() {
        return formato.margen();
    }

    public Zona hoja() {
        return new Zona(0, 0, ancho(), alto());
    }
}

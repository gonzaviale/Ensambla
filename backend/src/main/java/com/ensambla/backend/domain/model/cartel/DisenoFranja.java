package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

import java.util.List;

/** Fondo de papel con una franja del color principal que atraviesa el cartel justo detrás del protagonista. */
public class DisenoFranja extends DisenoDeVariante {

    private static final double ALTO_DE_LA_ZONA = 0.82;

    @Override
    public ColorHex fondo(ContextoDeDiseno contexto) {
        return contexto.paleta().papel();
    }

    @Override
    protected List<ElementoDeCartel> adornos(ContextoDeDiseno contexto) {
        return List.of(new Rectangulo(franjaDelProtagonista(contexto), contexto.paleta().principal()));
    }

    @Override
    protected Zona zonaDeTexto(ContextoDeDiseno contexto) {
        return contexto.hoja().franja(0, ALTO_DE_LA_ZONA).interior(contexto.margen());
    }

    @Override
    protected Zona zonaDeMarca(ContextoDeDiseno contexto) {
        return contexto.hoja().franja(ALTO_DE_LA_ZONA, 1 - ALTO_DE_LA_ZONA).interior(contexto.margen());
    }

    @Override
    protected ColorHex superficieDeTexto(ContextoDeDiseno contexto) {
        return contexto.paleta().papel();
    }

    @Override
    protected ColorHex superficieDeMarca(ContextoDeDiseno contexto) {
        return contexto.paleta().papel();
    }

    @Override
    protected ColorHex superficieDeLoDestacado(ContextoDeDiseno contexto) {
        return contexto.paleta().principal();
    }

    @Override
    protected AlineacionDeTexto alineacion() {
        return AlineacionDeTexto.IZQUIERDA;
    }

    private Zona franjaDelProtagonista(ContextoDeDiseno contexto) {
        Zona lugar = RolDeTexto.PROTAGONISTA.lugarEn(zonaDeTexto(contexto));
        return new Zona(0, lugar.y(), contexto.ancho(), lugar.alto());
    }
}

package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

import java.util.List;

/** Fondo del color principal con una tarjeta de papel centrada que lleva el mensaje. */
public class DisenoMarco extends DisenoDeVariante {

    private static final double ALTO_DE_LA_TARJETA = 0.84;

    @Override
    public ColorHex fondo(ContextoDeDiseno contexto) {
        return contexto.paleta().principal();
    }

    @Override
    protected List<ElementoDeCartel> adornos(ContextoDeDiseno contexto) {
        return List.of(new Rectangulo(tarjeta(contexto), contexto.paleta().papel()));
    }

    @Override
    protected Zona zonaDeTexto(ContextoDeDiseno contexto) {
        return tarjeta(contexto).interior(contexto.margen());
    }

    @Override
    protected Zona zonaDeMarca(ContextoDeDiseno contexto) {
        return contexto.hoja().franja(ALTO_DE_LA_TARJETA, 1 - ALTO_DE_LA_TARJETA).interior(contexto.margen());
    }

    @Override
    protected ColorHex superficieDeTexto(ContextoDeDiseno contexto) {
        return contexto.paleta().papel();
    }

    @Override
    protected ColorHex superficieDeMarca(ContextoDeDiseno contexto) {
        return contexto.paleta().principal();
    }

    @Override
    protected AlineacionDeTexto alineacion() {
        return AlineacionDeTexto.CENTRO;
    }

    private Zona tarjeta(ContextoDeDiseno contexto) {
        return contexto.hoja().franja(0, ALTO_DE_LA_TARJETA).interior(contexto.margen());
    }
}

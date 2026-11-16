package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

import java.util.List;

/** Un gran bloque del color principal arriba con el mensaje, y el nombre del negocio sobre papel abajo. */
public class DisenoBloque extends DisenoDeVariante {

    private static final double ALTO_DEL_BLOQUE = 0.76;

    @Override
    public ColorHex fondo(ContextoDeDiseno contexto) {
        return contexto.paleta().papel();
    }

    @Override
    protected List<ElementoDeCartel> adornos(ContextoDeDiseno contexto) {
        return List.of(new Rectangulo(bloque(contexto), contexto.paleta().principal()));
    }

    @Override
    protected Zona zonaDeTexto(ContextoDeDiseno contexto) {
        return bloque(contexto).interior(contexto.margen());
    }

    @Override
    protected Zona zonaDeMarca(ContextoDeDiseno contexto) {
        return contexto.hoja().franja(ALTO_DEL_BLOQUE, 1 - ALTO_DEL_BLOQUE).interior(contexto.margen());
    }

    @Override
    protected ColorHex superficieDeTexto(ContextoDeDiseno contexto) {
        return contexto.paleta().principal();
    }

    @Override
    protected ColorHex superficieDeMarca(ContextoDeDiseno contexto) {
        return contexto.paleta().papel();
    }

    @Override
    protected AlineacionDeTexto alineacion() {
        return AlineacionDeTexto.IZQUIERDA;
    }

    private Zona bloque(ContextoDeDiseno contexto) {
        return contexto.hoja().franja(0, ALTO_DEL_BLOQUE);
    }
}

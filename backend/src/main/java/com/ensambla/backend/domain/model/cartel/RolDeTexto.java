package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

import java.util.Arrays;

/**
 * Cada texto del cartel tiene un rol que fija cuánto lugar ocupa en la zona de texto (de arriba hacia abajo), qué
 * tan grande se escribe respecto de ese lugar y con qué peso. Los pesos de lugar suman 1.
 */
public enum RolDeTexto {

    TITULAR(0.14, 0.55, 700, false),
    PROTAGONISTA(0.30, 0.70, 800, true),
    BAJADA(0.12, 0.50, 500, false),
    DETALLE(0.08, 0.50, 400, false),
    LINEAS(0.36, 0.60, 500, false);

    private final double lugar;
    private final double tamanioRelativo;
    private final int peso;
    private final boolean esDestacado;

    RolDeTexto(double lugar, double tamanioRelativo, int peso, boolean esDestacado) {
        this.lugar = lugar;
        this.tamanioRelativo = tamanioRelativo;
        this.peso = peso;
        this.esDestacado = esDestacado;
    }

    public boolean esDestacado() {
        return esDestacado;
    }

    /** El protagonista se escribe en el color de acento cuando va sobre el papel; el resto, en la tinta que se lee. */
    public ColorHex colorSobre(ColorHex superficie, PaletaDeCartel paleta) {
        return esDestacado && superficie.equals(paleta.papel()) ? paleta.acento() : paleta.tintaSobre(superficie);
    }

    public int peso() {
        return peso;
    }

    /** La franja que le toca a este rol dentro de la zona de texto. */
    public Zona lugarEn(Zona zona) {
        return zona.franja(inicio(), lugar);
    }

    public double tamanioEn(Zona franja) {
        return franja.alto() * tamanioRelativo;
    }

    private double inicio() {
        return Arrays.stream(values()).filter(rol -> rol.ordinal() < ordinal()).mapToDouble(rol -> rol.lugar).sum();
    }
}

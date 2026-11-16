package com.ensambla.backend.domain.model.cartel;

/**
 * Los formatos en que se descarga un cartel. Las medidas son unidades de dibujo: décimas de milímetro en las hojas
 * (A4 = 2100 × 2970) y píxeles en las redes. El margen es la zona segura donde cae todo lo importante.
 */
public enum FormatoDeCartel {

    A4(2100, 2970, 100, 32),
    A5(1480, 2100, 80, 32),
    CUADRADO(1080, 1080, 60, 28),
    VERTICAL(1080, 1350, 60, 28);

    private final int ancho;
    private final int alto;
    private final int margen;
    private final int tamanioMinimoDeTexto;

    FormatoDeCartel(int ancho, int alto, int margen, int tamanioMinimoDeTexto) {
        this.ancho = ancho;
        this.alto = alto;
        this.margen = margen;
        this.tamanioMinimoDeTexto = tamanioMinimoDeTexto;
    }

    public int ancho() {
        return ancho;
    }

    public int alto() {
        return alto;
    }

    public int margen() {
        return margen;
    }

    /** El texto informativo no baja de acá: 9 pt en las hojas y 28 px en las redes. */
    public int tamanioMinimoDeTexto() {
        return tamanioMinimoDeTexto;
    }
}

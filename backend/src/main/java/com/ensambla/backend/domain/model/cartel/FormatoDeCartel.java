package com.ensambla.backend.domain.model.cartel;

/**
 * Los formatos en que se descarga un cartel. Las medidas son unidades de dibujo: décimas de milímetro en las hojas
 * (A4 = 2100 × 2970) y píxeles en las redes. El margen es la zona segura donde cae todo lo importante.
 */
public enum FormatoDeCartel {

    A4(2100, 2970, 100, 32, SalidaDeCartel.PDF),
    A5(1480, 2100, 80, 32, SalidaDeCartel.PDF),
    CUADRADO(1080, 1080, 60, 28, SalidaDeCartel.PNG),
    VERTICAL(1080, 1350, 60, 28, SalidaDeCartel.PNG);

    private final int ancho;
    private final int alto;
    private final int margen;
    private final int tamanioMinimoDeTexto;
    private final SalidaDeCartel salida;

    FormatoDeCartel(int ancho, int alto, int margen, int tamanioMinimoDeTexto, SalidaDeCartel salida) {
        this.ancho = ancho;
        this.alto = alto;
        this.margen = margen;
        this.tamanioMinimoDeTexto = tamanioMinimoDeTexto;
        this.salida = salida;
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

    /** Las hojas se bajan en PDF para imprimir y los formatos de redes, en PNG. */
    public SalidaDeCartel salida() {
        return salida;
    }

    /** El texto informativo no baja de acá: 9 pt en las hojas y 28 px en las redes. */
    public int tamanioMinimoDeTexto() {
        return tamanioMinimoDeTexto;
    }
}

package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

/** Un texto ya ubicado. Si no entra en el ancho se achica hasta un piso y después se corta con puntos suspensivos. */
public record TextoDeCartel(String texto, double x, double y, double tamanio, int peso, ColorHex color,
                            AlineacionDeTexto alineacion) implements ElementoDeCartel {

    private static final double ANCHO_DE_LETRA = 0.58;
    private static final String PUNTOS = "…";

    @Override
    public <T> T aceptar(VisitanteDeElemento<T> visitante) {
        return visitante.visitar(this);
    }

    public TextoDeCartel ajustadoA(double anchoDisponible, double tamanioMinimo) {
        double tamanioQueEntra = anchoDisponible / (texto.length() * ANCHO_DE_LETRA);
        double nuevoTamanio = Math.max(tamanioMinimo, Math.min(tamanio, tamanioQueEntra));
        return new TextoDeCartel(cortadoParaQueEntre(anchoDisponible, nuevoTamanio), x, y, nuevoTamanio, peso, color,
                alineacion);
    }

    private String cortadoParaQueEntre(double anchoDisponible, double tamanioElegido) {
        int letrasQueEntran = Math.max(2, (int) (anchoDisponible / (tamanioElegido * ANCHO_DE_LETRA)));
        return texto.length() <= letrasQueEntran ? texto : texto.substring(0, letrasQueEntran - 1).stripTrailing() + PUNTOS;
    }
}

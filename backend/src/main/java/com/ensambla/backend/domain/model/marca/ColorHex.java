package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Un color de la marca, escrito como {@code #RRGGBB}. */
public record ColorHex(String valor) {

    private static final Pattern FORMATO = Pattern.compile("#[0-9A-F]{6}");

    public ColorHex {
        valor = normalizar(valor);
        exigirFormatoValido(valor);
    }

    public static ColorHex de(String valor) {
        return new ColorHex(valor);
    }

    public static ColorHex de(int rgb) {
        return new ColorHex(String.format("#%06X", rgb & 0xFFFFFF));
    }

    public int rgb() {
        return Integer.parseInt(valor.substring(1), 16);
    }

    public double distanciaA(ColorHex otro) {
        return Math.sqrt(cuadrado(rojo() - otro.rojo()) + cuadrado(verde() - otro.verde())
                + cuadrado(azul() - otro.azul()));
    }

    private int rojo() {
        return (rgb() >> 16) & 0xFF;
    }

    private int verde() {
        return (rgb() >> 8) & 0xFF;
    }

    private int azul() {
        return rgb() & 0xFF;
    }

    private static int cuadrado(int numero) {
        return numero * numero;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.strip().toUpperCase(Locale.ROOT);
    }

    private static void exigirFormatoValido(String valor) {
        if (!FORMATO.matcher(valor).matches()) {
            throw new BusinessRuleException("El color tiene que escribirse como #RRGGBB");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}

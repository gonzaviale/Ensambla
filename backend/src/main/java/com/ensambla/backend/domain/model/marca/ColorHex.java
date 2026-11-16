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

    /** Luminancia relativa según WCAG, de 0 (negro) a 1 (blanco). */
    public double luminancia() {
        return 0.2126 * linealizado(rojo()) + 0.7152 * linealizado(verde()) + 0.0722 * linealizado(azul());
    }

    /** Razón de contraste WCAG con otro color, de 1 (iguales) a 21 (blanco sobre negro). */
    public double contrasteCon(ColorHex otro) {
        double claro = Math.max(luminancia(), otro.luminancia());
        double oscuro = Math.min(luminancia(), otro.luminancia());
        return (claro + 0.05) / (oscuro + 0.05);
    }

    /** Este color acercado al otro: con 0 queda igual y con 1 es el otro. */
    public ColorHex mezcladoCon(ColorHex otro, double proporcion) {
        return ColorHex.de(canal(rojo(), otro.rojo(), proporcion) << 16 | canal(verde(), otro.verde(), proporcion) << 8
                | canal(azul(), otro.azul(), proporcion));
    }

    private static int canal(int propio, int ajeno, double proporcion) {
        return (int) Math.round(propio + (ajeno - propio) * proporcion);
    }

    private static double linealizado(int canal) {
        double normalizado = canal / 255.0;
        return normalizado <= 0.03928 ? normalizado / 12.92 : Math.pow((normalizado + 0.055) / 1.055, 2.4);
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

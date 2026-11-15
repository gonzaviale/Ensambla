package com.ensambla.backend.domain.model.marca;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Propone los dos colores de la marca a partir de los píxeles del logo, sin IA: agrupa los colores parecidos, descarta
 * colores sin saturación (blancos, negros y grises) y casi negros, y elige los dos grupos más frecuentes que se distinguen entre sí.
 */
public final class SugeridorDeColores {

    private static final int BITS_DESCARTADOS = 3;
    private static final int GRIS_MAXIMO = 24;
    private static final int OSCURO_MAXIMO = 30;
    private static final double DISTANCIA_MINIMA = 80;

    private SugeridorDeColores() {
    }

    public static ColoresSugeridos sugerir(List<Integer> pixeles, ColoresSugeridos reserva) {
        List<ColorHex> frecuentes = coloresFrecuentes(pixeles);
        if (frecuentes.isEmpty()) {
            return reserva;
        }
        ColorHex principal = frecuentes.getFirst();
        return new ColoresSugeridos(principal, secundarioDistinto(principal, frecuentes, reserva));
    }

    private static ColorHex secundarioDistinto(ColorHex principal, List<ColorHex> frecuentes, ColoresSugeridos reserva) {
        return frecuentes.stream()
                .filter(color -> color.distanciaA(principal) >= DISTANCIA_MINIMA)
                .findFirst()
                .orElseGet(() -> reservaMasLejanaDe(principal, reserva));
    }

    private static ColorHex reservaMasLejanaDe(ColorHex principal, ColoresSugeridos reserva) {
        return Stream.of(reserva.principal(), reserva.secundario())
                .max(Comparator.comparingDouble(color -> color.distanciaA(principal)))
                .orElseThrow();
    }

    private static List<ColorHex> coloresFrecuentes(List<Integer> pixeles) {
        Map<Integer, Grupo> grupos = new HashMap<>();
        pixeles.stream().filter(SugeridorDeColores::esColorDeMarca)
                .forEach(pixel -> grupos.computeIfAbsent(claveDe(pixel), clave -> new Grupo()).sumar(pixel));
        return grupos.values().stream()
                .sorted(Comparator.comparingInt(Grupo::cantidad).reversed())
                .map(Grupo::promedio)
                .toList();
    }

    private static boolean esColorDeMarca(int pixel) {
        int maximo = Math.max(rojo(pixel), Math.max(verde(pixel), azul(pixel)));
        int minimo = Math.min(rojo(pixel), Math.min(verde(pixel), azul(pixel)));
        return maximo - minimo > GRIS_MAXIMO && maximo > OSCURO_MAXIMO;
    }

    private static int claveDe(int pixel) {
        return (rojo(pixel) >> BITS_DESCARTADOS) << 10 | (verde(pixel) >> BITS_DESCARTADOS) << 5
                | (azul(pixel) >> BITS_DESCARTADOS);
    }

    private static int rojo(int pixel) {
        return (pixel >> 16) & 0xFF;
    }

    private static int verde(int pixel) {
        return (pixel >> 8) & 0xFF;
    }

    private static int azul(int pixel) {
        return pixel & 0xFF;
    }

    private static final class Grupo {

        private int cantidad;
        private long rojos;
        private long verdes;
        private long azules;

        void sumar(int pixel) {
            cantidad++;
            rojos += rojo(pixel);
            verdes += verde(pixel);
            azules += azul(pixel);
        }

        int cantidad() {
            return cantidad;
        }

        ColorHex promedio() {
            return ColorHex.de((int) (rojos / cantidad) << 16 | (int) (verdes / cantidad) << 8
                    | (int) (azules / cantidad));
        }
    }
}

package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Los colores con que se dibuja un cartel: el papel y la tinta de Ensambla, más los dos colores del negocio ya
 * ajustados para que todo texto se lea (contraste mínimo 4,5 : 1).
 */
public record PaletaDeCartel(ColorHex papel, ColorHex principal, ColorHex acento, ColorHex pizarra) {

    public static final ColorHex PAPEL = ColorHex.de("#FFFDFA");
    public static final ColorHex PIZARRA = ColorHex.de("#263443");
    public static final ColorHex BLANCO = ColorHex.de("#FFFFFF");
    public static final ColorHex NEGRO = ColorHex.de("#000000");
    public static final double CONTRASTE_MINIMO = 4.5;

    private static final double PASO_DE_AJUSTE = 0.08;
    private static final double LUMINANCIA_DE_CORTE = 0.2;

    public static PaletaDeCartel de(ColorHex principal, ColorHex acento) {
        return new PaletaDeCartel(PAPEL, conTextoLegibleEncima(principal), legibleSobrePapel(acento), PIZARRA);
    }

    /** La tinta que mejor se lee sobre esa superficie: pizarra o blanco. */
    public ColorHex tintaSobre(ColorHex superficie) {
        return Stream.of(pizarra, BLANCO)
                .max(Comparator.comparingDouble(superficie::contrasteCon))
                .orElseThrow();
    }

    private static ColorHex conTextoLegibleEncima(ColorHex color) {
        ColorHex extremo = color.luminancia() > LUMINANCIA_DE_CORTE ? BLANCO : NEGRO;
        ColorHex ajustado = color;
        while (!tieneTintaLegible(ajustado)) {
            ajustado = ajustado.mezcladoCon(extremo, PASO_DE_AJUSTE);
        }
        return ajustado;
    }

    private static boolean tieneTintaLegible(ColorHex superficie) {
        return Math.max(superficie.contrasteCon(PIZARRA), superficie.contrasteCon(BLANCO)) >= CONTRASTE_MINIMO;
    }

    private static ColorHex legibleSobrePapel(ColorHex color) {
        ColorHex ajustado = color;
        while (ajustado.contrasteCon(PAPEL) < CONTRASTE_MINIMO) {
            ajustado = ajustado.mezcladoCon(NEGRO, PASO_DE_AJUSTE);
        }
        return ajustado;
    }
}

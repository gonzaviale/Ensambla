package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Números y pesos como los dice el negocio en los textos de los mini-objetivos: punto de miles y coma decimal. */
final class FormatoObjetivo {

    private static final Locale ARGENTINA = Locale.of("es", "AR");

    private FormatoObjetivo() {
    }

    /** Pesos enteros: $12.600. */
    static String pesos(BigDecimal importe) {
        return "$" + formato("#,##0").format(importe);
    }

    /** Un número con a lo sumo un decimal: 14 y 35,7. */
    static String numero(BigDecimal valor) {
        return formato("#,##0.#").format(valor);
    }

    private static DecimalFormat formato(String patron) {
        DecimalFormat formato = new DecimalFormat(patron, DecimalFormatSymbols.getInstance(ARGENTINA));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato;
    }
}

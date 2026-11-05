package com.ensambla.backend.domain.model.promocion;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Cómo se escribe un precio para el cliente: pesos con punto de miles y coma decimal, sin decimales si no hacen falta. */
final class Precio {

    private static final Locale ARGENTINA = Locale.of("es", "AR");

    private Precio() {
    }

    static String formato(BigDecimal importe) {
        return "$ " + new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ARGENTINA)).format(importe);
    }
}

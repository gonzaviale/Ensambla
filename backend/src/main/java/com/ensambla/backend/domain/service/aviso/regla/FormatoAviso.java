package com.ensambla.backend.domain.service.aviso.regla;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.Registro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/** Lecturas y textos que comparten las reglas de avisos de ventas: números, pesos y fechas como los dice el negocio. */
final class FormatoAviso {

    static final String CLAVE_NOMBRE = "name";
    static final String CLAVE_PRECIO = "price";
    static final String CLAVE_COSTO = "cost";
    static final String CLAVE_CANTIDAD = "quantity";

    private static final BigDecimal DIEZ = BigDecimal.TEN;
    private static final Locale ARGENTINA = Locale.of("es", "AR");

    private FormatoAviso() {
    }

    static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    static String nombreDe(BloqueInventario inventario, Registro registro) {
        Object nombre = inventario.valorDe(registro, CLAVE_NOMBRE);
        return nombre instanceof String texto && !texto.isBlank() ? texto.trim() : "Un producto sin nombre";
    }

    /** El valor redondeado a la decena más cercana ($1.147 queda en $1.150), como se ponen los precios en un cartel. */
    static BigDecimal aDiez(BigDecimal valor) {
        return valor.divide(DIEZ, 0, RoundingMode.HALF_UP).multiply(DIEZ);
    }

    /** Número con coma decimal y sin ceros de más: 12,5 y 3. */
    static String texto(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Pesos con punto de miles y coma decimal, sin decimales si no hacen falta: $1.150 y $1.150,50. */
    static String pesos(BigDecimal importe) {
        DecimalFormat formato = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ARGENTINA));
        return "$" + formato.format(importe);
    }

    /** Día y mes sin ceros: 5/10. */
    static String fechaCorta(LocalDate fecha) {
        return fecha.getDayOfMonth() + "/" + fecha.getMonthValue();
    }
}

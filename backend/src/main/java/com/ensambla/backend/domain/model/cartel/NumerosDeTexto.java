package com.ensambla.backend.domain.model.cartel;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Los números que escribe un texto, leídos como se escriben en Argentina ({@code $2.500}, {@code 20,5 %},
 * {@code 22/11}), para compararlos con los datos reales del cartel.
 */
public final class NumerosDeTexto {

    private static final Pattern NUMERO = Pattern.compile("\\d+(?:[.,]\\d+)*");
    private static final Pattern CON_MILES = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?");

    private NumerosDeTexto() {
    }

    public static Set<BigDecimal> en(String texto) {
        Matcher encontrado = NUMERO.matcher(texto);
        return encontrado.results()
                .map(resultado -> valorDe(resultado.group()))
                .collect(Collectors.toSet());
    }

    public static BigDecimal normalizado(BigDecimal numero) {
        return numero.stripTrailingZeros();
    }

    private static BigDecimal valorDe(String numero) {
        String conPunto = CON_MILES.matcher(numero).matches() ? numero.replace(".", "") : numero;
        return normalizado(new BigDecimal(conPunto.replace(',', '.')));
    }
}

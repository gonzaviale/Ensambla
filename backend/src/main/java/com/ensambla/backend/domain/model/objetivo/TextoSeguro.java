package com.ensambla.backend.domain.model.objetivo;

import java.util.regex.Pattern;

/**
 * Lo que se le manda a la IA de un texto escrito por la persona: sin correos ni números largos (teléfonos, DNI, CUIT),
 * que son datos personales de ella o de sus clientes y que la IA no necesita para armar un objetivo. Un importe como
 * «$1.400.000» no se toca.
 */
public final class TextoSeguro {

    public static final String OCULTO = "[dato oculto]";

    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");

    /** Ocho dígitos o más, con espacios, puntos, guiones o paréntesis entre ellos, que no sigan a un «$». */
    private static final Pattern NUMERO_LARGO = Pattern.compile("(?<![\\w$])\\+?\\d(?:[\\s.\\-()]?\\d){7,}");

    private TextoSeguro() {
    }

    public static String de(String texto) {
        String sinCorreos = CORREO.matcher(texto).replaceAll(OCULTO);
        return NUMERO_LARGO.matcher(sinCorreos).replaceAll(OCULTO);
    }
}

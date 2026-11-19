package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lo que la persona escribió para pedir una métrica («¿cuánto vendí esta semana?»), listo para buscarle frases del
 * diccionario sin que importen las tildes, las mayúsculas ni los plurales.
 */
public record PedidoEnPalabras(String texto, List<String> palabras) {

    private static final String SIGNOS_DE_LOS_BORDES = "^[\\s¿¡?!.,;:]+|[\\s¿¡?!.,;:]+$";

    public static PedidoEnPalabras de(String texto) {
        return new PedidoEnPalabras(texto, TextoNormalizado.palabras(texto));
    }

    /** {@code true} si aparece alguna de las frases, completa y en orden. */
    public boolean menciona(List<String> frases) {
        return frases.stream().anyMatch(this::mencionaLaFrase);
    }

    /** La primera de las opciones, en su orden, que el pedido menciona con alguna de sus frases. */
    public <T extends Enum<T>> Optional<T> loPrimeroQueMenciona(T[] opciones, Map<T, List<String>> frasesDe) {
        return Arrays.stream(opciones)
                .filter(opcion -> menciona(frasesDe.getOrDefault(opcion, List.of())))
                .findFirst();
    }

    /** El pedido como nombre de la métrica: sin los signos de los bordes, con mayúscula y del largo permitido. */
    public String nombreSugerido() {
        String limpio = texto.replaceAll(SIGNOS_DE_LOS_BORDES, "");
        String recortado = limpio.substring(0, Math.min(limpio.length(), DefinicionMetrica.LARGO_MAXIMO_DEL_NOMBRE));
        return conMayuscula(recortado.strip());
    }

    private boolean mencionaLaFrase(String frase) {
        return TextoNormalizado.contiene(palabras, TextoNormalizado.palabras(frase));
    }

    private static String conMayuscula(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}

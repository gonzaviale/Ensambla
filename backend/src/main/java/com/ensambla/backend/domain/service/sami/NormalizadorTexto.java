package com.ensambla.backend.domain.service.sami;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Deja el texto de la persona y el del diccionario en la misma forma: minúsculas, sin tildes ni signos y con el
 * plural y la vocal final recortados ({@code velas} y {@code vela} dan lo mismo). No conjuga verbos: el diccionario
 * lista las formas que importan ({@code vendo}, {@code vendemos}, {@code vender}).
 */
final class NormalizadorTexto {

    private static final int LARGO_MINIMO_PARA_RECORTAR = 4;

    private NormalizadorTexto() {
    }

    static List<String> palabras(String texto) {
        String sinTildes = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String soloLetras = sinTildes.replaceAll("[^a-z0-9]+", " ").trim();
        return Arrays.stream(soloLetras.split(" "))
                .filter(palabra -> !palabra.isEmpty())
                .map(NormalizadorTexto::raiz)
                .toList();
    }

    /** {@code true} si {@code frase} aparece completa y en orden dentro de {@code palabras}. */
    static boolean contiene(List<String> palabras, List<String> frase) {
        return !frase.isEmpty() && Collections.indexOfSubList(palabras, frase) >= 0;
    }

    /** Forma comparable de un nombre: {@code «Teléfono»} y {@code «telefonos»} dan la misma clave. */
    static String clave(String nombre) {
        return String.join(" ", palabras(nombre));
    }

    private static String raiz(String palabra) {
        String sinPlural = palabra;
        if (sinPlural.length() > LARGO_MINIMO_PARA_RECORTAR && sinPlural.endsWith("es")) {
            sinPlural = sinPlural.substring(0, sinPlural.length() - 2);
        } else if (sinPlural.length() > LARGO_MINIMO_PARA_RECORTAR - 1 && sinPlural.endsWith("s")) {
            sinPlural = sinPlural.substring(0, sinPlural.length() - 1);
        }
        boolean terminaEnVocal = "aeiou".indexOf(sinPlural.charAt(sinPlural.length() - 1)) >= 0;
        if (sinPlural.length() > LARGO_MINIMO_PARA_RECORTAR && terminaEnVocal) {
            return sinPlural.substring(0, sinPlural.length() - 1);
        }
        return sinPlural;
    }
}

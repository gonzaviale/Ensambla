package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Encuentra, entre las filas que una tabla ya tiene, la que la persona quiso nombrar aunque no la haya escrito tal
 * cual está en la base: sin tildes ni mayúsculas, en plural o singular, con una letra de más o de menos, o con sólo
 * parte del nombre («harina» por «Harina 000 x kg»). Si dos filas parecen igual de buenas no elige ninguna: es mejor
 * agregar una fila que pisar la equivocada.
 */
public final class CoincidenciaDeFilas {

    private static final int LARGO_MINIMO_PARA_PREFIJO = 3;
    private static final int LARGO_MINIMO_PARA_ERRATA = 4;
    private static final double JACCARD_MINIMO = 0.5;
    private static final double PUNTAJE_EXACTO = 2.0;

    private CoincidenciaDeFilas() {
    }

    /**
     * @param buscado    lo que la persona escribió
     * @param candidatos el nombre de cada fila existente
     * @return la posición de la fila que corresponde, o vacío si ninguna lo es con claridad
     */
    public static Optional<Integer> buscar(String buscado, List<String> candidatos) {
        List<String> palabrasBuscadas = TextoNormalizado.palabras(buscado);
        double mejor = 0;
        List<Integer> mejores = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            double puntaje = puntaje(palabrasBuscadas, TextoNormalizado.palabras(candidatos.get(i)));
            if (puntaje > mejor) {
                mejor = puntaje;
                mejores.clear();
            }
            if (puntaje > 0 && puntaje == mejor) {
                mejores.add(i);
            }
        }
        return mejores.size() == 1 ? Optional.of(mejores.getFirst()) : Optional.empty();
    }

    /** 0 si no se parecen; si no, qué parte de las palabras comparten (de 0 a 1), y 2 si son las mismas palabras. */
    private static double puntaje(List<String> buscadas, List<String> delCandidato) {
        if (!buscadas.isEmpty() && buscadas.equals(delCandidato)) {
            return PUNTAJE_EXACTO;
        }
        long coinciden = buscadas.stream()
                .filter(palabra -> delCandidato.stream().anyMatch(otra -> parecidas(palabra, otra)))
                .count();
        double jaccard = (double) coinciden / (buscadas.size() + delCandidato.size() - coinciden);
        boolean nombraTodo = coinciden == buscadas.size() && coinciden > 0;
        return nombraTodo || jaccard >= JACCARD_MINIMO ? jaccard : 0;
    }

    private static boolean parecidas(String una, String otra) {
        if (una.equals(otra)) {
            return true;
        }
        int corta = Math.min(una.length(), otra.length());
        if (corta >= LARGO_MINIMO_PARA_PREFIJO && (una.startsWith(otra) || otra.startsWith(una))) {
            return true;
        }
        return corta >= LARGO_MINIMO_PARA_ERRATA && distancia(una, otra) <= 1;
    }

    /** Distancia de edición entre dos palabras cortas. */
    private static int distancia(String una, String otra) {
        int[] anterior = new int[otra.length() + 1];
        for (int j = 0; j <= otra.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= una.length(); i++) {
            int[] actual = new int[otra.length() + 1];
            actual[0] = i;
            for (int j = 1; j <= otra.length(); j++) {
                int costo = una.charAt(i - 1) == otra.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + costo);
            }
            anterior = actual;
        }
        return anterior[otra.length()];
    }
}

package com.ensambla.backend.domain.model.objetivo;

import java.util.List;
import java.util.Optional;

/**
 * Lo que se le consulta a la IA para armar un objetivo: la conversación (ya sin datos personales), el catálogo de
 * métricas posibles con sus números calculados por el servidor y lo que se sabe del negocio. La IA elige del catálogo
 * por código ({@code c1}, {@code c2}…) o redacta acciones; nunca pone un número.
 */
public record ConsultaObjetivoIa(
        List<MensajeObjetivo> mensajes, List<CandidatoMiniObjetivo> catalogo, ContextoObjetivoIa contexto) {

    /** El código de cada métrica del catálogo es su lugar en la lista: c1 es la primera. */
    public static String codigo(int indice) {
        return "c" + (indice + 1);
    }

    /** La métrica del catálogo con ese código; vacía si la IA nombró una que no existe. */
    public Optional<CandidatoMiniObjetivo> candidato(String codigo) {
        for (int indice = 0; indice < catalogo.size(); indice++) {
            if (codigo(indice).equals(codigo)) {
                return Optional.of(catalogo.get(indice));
            }
        }
        return Optional.empty();
    }
}

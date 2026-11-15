package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Un mini-objetivo posible, con los números calculados con los datos del negocio y su texto. Todavía no es de nadie:
 * se vuelve un {@link MiniObjetivo} cuando se elige.
 *
 * @param registroId     el producto, si el tipo es de producto
 * @param referencia     el nombre del producto o de la categoría de gasto
 * @param aporteEstimado pesos por mes que sumaría al resultado del negocio
 * @param textoConIa     si el texto lo redactó la IA; el de reserva es {@code false}
 */
public record CandidatoMiniObjetivo(
        TipoMiniObjetivo tipo,
        RegistroId registroId,
        String referencia,
        BigDecimal valorInicial,
        BigDecimal meta,
        BigDecimal aporteEstimado,
        String titulo,
        String justificacion,
        boolean textoConIa) {

    /** El mismo candidato con el texto que redactó la IA. */
    public CandidatoMiniObjetivo conTextoDeIa(TextoMiniObjetivo texto) {
        return new CandidatoMiniObjetivo(tipo, registroId, referencia, valorInicial, meta, aporteEstimado,
                texto.titulo(), texto.justificacion(), true);
    }

    /** Si es el que la persona eligió: mismo tipo, mismo producto y misma referencia. */
    public boolean esElMismo(TipoMiniObjetivo otroTipo, RegistroId otroRegistroId, String otraReferencia) {
        return tipo == otroTipo
                && Objects.equals(registroId, otroRegistroId)
                && Objects.equals(referencia, otraReferencia);
    }
}

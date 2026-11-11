package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.util.List;

/**
 * Lo que la IA redactó para los candidatos que se le pasaron.
 *
 * @param textos un texto por candidato y en el mismo orden
 * @param consumo lo que gastó, para registrarlo contra el límite diario
 */
public record RedaccionObjetivosIa(List<TextoMiniObjetivo> textos, ConsumoIaSami consumo) {
}

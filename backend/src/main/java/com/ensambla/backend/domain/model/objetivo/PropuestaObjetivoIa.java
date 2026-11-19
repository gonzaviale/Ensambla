package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.util.List;

/**
 * Lo que respondió la IA, todavía sin validar.
 *
 * @param respuesta   lo que Sami le dice a la persona
 * @param entendido   cómo entendió el objetivo; vacío si necesita aclarar
 * @param esMonetario si la persona contó una meta en pesos por mes, que se define en el formulario de siempre
 * @param consumo     lo que gastó, para registrarlo contra el límite diario
 */
public record PropuestaObjetivoIa(
        String respuesta, String entendido, boolean esMonetario, List<EleccionIa> elecciones,
        ConsumoIaSami consumo) {
}

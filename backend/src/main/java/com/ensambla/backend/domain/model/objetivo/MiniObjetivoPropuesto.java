package com.ensambla.backend.domain.model.objetivo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Un mini-objetivo que Sami propone y la persona todavía no guardó: uno medido (con los números del servidor) o uno
 * manual (una acción). Se vuelve un {@link MiniObjetivo} cuando se guarda el objetivo.
 *
 * @param candidato   la métrica con sus números; vacío si es manual
 * @param accion      la acción; vacía si es medido
 * @param textoConIa  si el texto lo redactó la IA
 */
public record MiniObjetivoPropuesto(
        ModoMiniObjetivo modo, CandidatoMiniObjetivo candidato, AccionManual accion, boolean textoConIa) {

    public static MiniObjetivoPropuesto medido(CandidatoMiniObjetivo candidato) {
        return new MiniObjetivoPropuesto(ModoMiniObjetivo.MEDIDO, candidato, null, candidato.textoConIa());
    }

    public static MiniObjetivoPropuesto manual(AccionManual accion, boolean textoConIa) {
        return new MiniObjetivoPropuesto(ModoMiniObjetivo.MANUAL, null, accion, textoConIa);
    }

    public boolean esMedido() {
        return modo == ModoMiniObjetivo.MEDIDO;
    }

    public boolean esManual() {
        return modo == ModoMiniObjetivo.MANUAL;
    }

    /** El mini-objetivo en curso en esa posición, para el objetivo ya guardado. */
    public MiniObjetivo aMiniObjetivo(Objetivo objetivo, int posicion, LocalDate semanaInicio, OffsetDateTime ahora) {
        return esMedido()
                ? MiniObjetivo.nuevo(objetivo, posicion, candidato, semanaInicio, ahora)
                : MiniObjetivo.manual(objetivo, posicion, accion, textoConIa, semanaInicio, ahora);
    }
}

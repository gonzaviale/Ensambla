package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;

import java.util.List;
import java.util.regex.Pattern;

/** Lo que contestó el modelo en una consulta y lo que de eso valió después de validarlo. */
record LecturaDelTurno(ResultadoTallerIa resultado, ValidacionTaller validacion) {

    /** Verbos con los que Sami cuenta un cambio hecho («Armé tu sistema…»). */
    private static final Pattern AFIRMA_CAMBIOS = Pattern.compile(
            "(?iU)\\b(armé|sumé|agregué|renombré|moví|conecté|quité|cambié|cargué|creé|separé|borré|eliminé)\\b");

    boolean afirmaCambiosSinHacerlos() {
        return validacion.acciones().isEmpty() && resultado.texto() != null
                && AFIRMA_CAMBIOS.matcher(resultado.texto()).find();
    }

    /**
     * Si el modelo ya preguntó en su texto sin usar la botonera, no se suma la pregunta del kit: serían dos preguntas
     * distintas a la vez.
     */
    List<OpcionesTaller> preguntas() {
        return preguntoEnElTexto() && !usoLaBotonera() ? List.of() : validacion.preguntas();
    }

    boolean preguntoEnElTexto() {
        return resultado.texto() != null && resultado.texto().contains("?");
    }

    boolean usoLaBotonera() {
        return resultado.llamadas().stream()
                .anyMatch(llamada -> HerramientaTaller.OFRECER_OPCIONES.nombre().equals(llamada.nombre()));
    }
}

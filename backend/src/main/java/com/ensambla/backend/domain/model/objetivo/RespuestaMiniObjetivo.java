package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Lo que la persona contó de un mini-objetivo manual: si lo hizo y qué pasó. Queda guardada con su fecha y no se
 * modifica: un manual se responde una sola vez.
 *
 * @param comentario   lo que quiso agregar, sin espacios en los extremos; vacío si no escribió nada
 * @param semanaInicio el lunes (hora argentina) de la semana en que respondió
 */
public record RespuestaMiniObjetivo(
        RespuestaMiniObjetivoId id,
        NegocioId negocioId,
        ObjetivoId objetivoId,
        MiniObjetivoId miniObjetivoId,
        HizoManual hizo,
        QuePaso quePaso,
        String comentario,
        LocalDate semanaInicio,
        OffsetDateTime respondidaEn) {

    public static final int LARGO_MAXIMO_COMENTARIO = 300;

    /**
     * La respuesta de la semana a un mini-objetivo manual, con su id nuevo.
     *
     * @throws BusinessRuleException si dice que lo hizo pero no tuvo tiempo, o si el comentario es muy largo
     */
    public static RespuestaMiniObjetivo de(
            MiniObjetivo mini, HizoManual hizo, QuePaso quePaso, String comentario, OffsetDateTime ahora) {
        if (hizo == HizoManual.SI && quePaso == QuePaso.NO_TUVE_TIEMPO) {
            throw new BusinessRuleException("Si lo hiciste, no puede ser que no hayas tenido tiempo");
        }
        String limpio = comentario == null || comentario.isBlank() ? null : comentario.strip();
        if (limpio != null && limpio.length() > LARGO_MAXIMO_COMENTARIO) {
            throw new BusinessRuleException(
                    "El comentario no puede tener más de " + LARGO_MAXIMO_COMENTARIO + " caracteres");
        }
        return new RespuestaMiniObjetivo(
                RespuestaMiniObjetivoId.nuevo(),
                mini.getNegocioId(),
                mini.getObjetivoId(),
                mini.getId(),
                hizo,
                quePaso,
                limpio,
                SemanaObjetivo.lunesDe(ZonaNegocio.dia(ahora)),
                ahora);
    }
}

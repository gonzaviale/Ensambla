package com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion;

import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;

import java.time.OffsetDateTime;

/**
 * Código para vincular el número.
 *
 * @param codigo    lo que el usuario escribe después de VINCULAR
 * @param expiraEn  hasta cuándo sirve
 * @param numeroBot número de WhatsApp al que tiene que escribirle; vacío si no se configuró
 */
public record GenerarCodigoVinculacionResponse(String codigo, OffsetDateTime expiraEn, String numeroBot) {

    public static GenerarCodigoVinculacionResponse de(CodigoVinculacion codigo, String numeroBot) {
        return new GenerarCodigoVinculacionResponse(codigo.getCodigo(), codigo.getExpiraEn(), numeroBot);
    }
}

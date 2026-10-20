package com.ensambla.backend.domain.dto.whatsapp;

import java.time.OffsetDateTime;

/**
 * Código para vincular el número.
 *
 * @param codigo    lo que el usuario escribe después de VINCULAR
 * @param expiraEn  hasta cuándo sirve
 * @param numeroBot número de WhatsApp al que tiene que escribirle; vacío si no se configuró
 */
public record CodigoVinculacionItem(String codigo, OffsetDateTime expiraEn, String numeroBot) {
}

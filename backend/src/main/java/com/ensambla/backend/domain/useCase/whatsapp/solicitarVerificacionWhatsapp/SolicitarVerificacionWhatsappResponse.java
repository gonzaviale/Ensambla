package com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;

import java.time.OffsetDateTime;

/**
 * El código se manda por WhatsApp y nunca vuelve en la respuesta.
 *
 * @param telefono      el número al que se mandó, ya normalizado
 * @param expiraEn      hasta cuándo sirve el código
 * @param reenviarDesde desde cuándo se puede pedir otro
 */
public record SolicitarVerificacionWhatsappResponse(String telefono, OffsetDateTime expiraEn,
                                                    OffsetDateTime reenviarDesde) {

    public static SolicitarVerificacionWhatsappResponse de(VerificacionWhatsapp verificacion) {
        return new SolicitarVerificacionWhatsappResponse(
                verificacion.getTelefono(), verificacion.getExpiraEn(), verificacion.reenviarDesde());
    }
}

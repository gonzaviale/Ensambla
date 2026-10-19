package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un mensaje tal como avisa Meta que llegó, antes de saber de qué negocio es.
 *
 * @param waMensajeId id del mensaje en Meta
 * @param telefono    número que lo mandó (código de país y número, sólo dígitos)
 * @param texto       el texto, si es {@link TipoMensajeWhatsapp#TEXTO}
 * @param mediaId     id del archivo de audio en Meta, si es {@link TipoMensajeWhatsapp#AUDIO}
 */
public record MensajeEntranteWhatsapp(
        String waMensajeId,
        String telefono,
        TipoMensajeWhatsapp tipo,
        String texto,
        String mediaId,
        OffsetDateTime recibidoEn
) {

    /** "123456" o "Ensambla 123456", sin importar mayúsculas ni espacios alrededor. */
    private static final Pattern CODIGO = Pattern.compile("^\\s*(?:ensambla\\s+)?(\\d{6})\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** El código de vinculación, si el mensaje es sólo eso. */
    public Optional<String> codigoDeVinculacion() {
        if (tipo != TipoMensajeWhatsapp.TEXTO) {
            return Optional.empty();
        }
        Matcher matcher = CODIGO.matcher(texto);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}

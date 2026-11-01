package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** Número de WhatsApp en formato E.164 sin "+": sólo dígitos, con código de país. */
public record TelefonoWhatsapp(String valor) {

    public TelefonoWhatsapp {
        if (valor == null || !valor.matches("\\d{8,15}")) {
            throw new BusinessRuleException("El teléfono de WhatsApp no es válido");
        }
    }

    /** Acepta el identificador del chat ({@code 5491122334455@s.whatsapp.net}) o un número con "+", espacios o guiones. */
    public static TelefonoWhatsapp de(String identificador) {
        if (identificador == null) {
            throw new BusinessRuleException("El teléfono de WhatsApp no es válido");
        }
        String numero = identificador.split("@", 2)[0].split(":", 2)[0];
        return new TelefonoWhatsapp(numero.replaceAll("\\D", ""));
    }

    /**
     * Número que la persona escribió a mano en la app. WhatsApp identifica los celulares de Argentina como {@code 549…};
     * si escribió {@code +54 11 2233 4455} (sin el 9) se lo agregamos, porque si no el número verificado nunca
     * coincidiría con el remitente de los mensajes.
     */
    public static TelefonoWhatsapp deIngresoManual(String ingresado) {
        if (ingresado == null) {
            throw new BusinessRuleException("El teléfono de WhatsApp no es válido");
        }
        String digitos = ingresado.replaceAll("\\D", "");
        if (digitos.matches("54(?!9)\\d{10}")) {
            digitos = "549" + digitos.substring(2);
        }
        return new TelefonoWhatsapp(digitos);
    }
}

package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.Set;

/** Cómo se lee lo que la persona contesta cuando Sami le pidió confirmar o elegir una tabla. */
public enum RespuestaChat {
    SI,
    NO,
    NUMERO,
    OTRO;

    private static final Set<String> AFIRMATIVAS = Set.of("si", "ok", "dale", "confirmo", "confirmar", "guardar");
    private static final Set<String> NEGATIVAS = Set.of("no", "cancelar", "descartar");

    public static RespuestaChat de(String texto) {
        String limpio = TextoNormalizado.de(texto).replaceAll("[^a-z0-9]", "");
        if (AFIRMATIVAS.contains(limpio)) {
            return SI;
        }
        if (NEGATIVAS.contains(limpio)) {
            return NO;
        }
        return limpio.matches("[0-9]{1,2}") ? NUMERO : OTRO;
    }

    /** El número que escribió; sólo tiene sentido si {@link #de} dio {@link #NUMERO}. */
    public static int numero(String texto) {
        return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
    }
}

package com.ensambla.backend.domain.exception;

import java.util.Map;

public class CampoInvalidoException extends BusinessRuleException {

    private final Map<String, String> errores;

    public CampoInvalidoException(Map<String, String> errores) {
        super("Revisá la configuración del campo");
        this.errores = Map.copyOf(errores);
    }

    /** Mensaje por dato de la configuración ({@code nombre}, {@code tipo}, {@code opciones}, {@code valorInicial}). */
    public Map<String, String> getErrores() {
        return errores;
    }
}

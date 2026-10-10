package com.ensambla.backend.domain.exception;

import java.util.Map;

public class RegistroInvalidoException extends BusinessRuleException {

    private final Map<String, String> errores;

    public RegistroInvalidoException(Map<String, String> errores) {
        super("Revisá los datos marcados");
        this.errores = Map.copyOf(errores);
    }

    /** Mensaje por campo, indexado por el id del campo. */
    public Map<String, String> getErrores() {
        return errores;
    }
}

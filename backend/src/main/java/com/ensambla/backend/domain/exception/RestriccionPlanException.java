package com.ensambla.backend.domain.exception;

import lombok.Getter;

@Getter
public class RestriccionPlanException extends BusinessRuleException {

    private final String codigo;

    public RestriccionPlanException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

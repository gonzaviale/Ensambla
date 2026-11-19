package com.ensambla.backend.domain.model.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Un dato tal como quedó cargado en un registro, que puede faltar o venir con otro tipo. Las reglas lo leen como lo que
 * esperan (un número, un texto, una fecha) y reciben {@code null} cuando el dato no sirve para eso.
 */
public record ValorDeRegistro(Object valor) {

    public static ValorDeRegistro de(Object valor) {
        return new ValorDeRegistro(valor);
    }

    public BigDecimal comoNumero() {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    public BigDecimal comoNumeroPositivo() {
        BigDecimal numero = comoNumero();
        return numero != null && numero.signum() > 0 ? numero : null;
    }

    public String comoTexto() {
        return tieneTexto() ? ((String) valor).trim() : null;
    }

    public String comoTextoO(String siNoHayTexto) {
        return tieneTexto() ? comoTexto() : siNoHayTexto;
    }

    public boolean tieneTexto() {
        return valor instanceof String texto && !texto.isBlank();
    }

    public LocalDate comoFecha() {
        if (!tieneTexto()) {
            return null;
        }
        try {
            return LocalDate.parse(comoTexto());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

package com.ensambla.backend.domain.model.ubicacion;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** Lo que la persona escribió para buscar una dirección, ya sin espacios de más y con un largo razonable. */
public record ConsultaDireccion(String texto) {

    private static final int LARGO_MINIMO = 5;
    private static final int LARGO_MAXIMO = 200;

    public static ConsultaDireccion de(String escrito) {
        String normalizado = escrito.strip().replaceAll("\s+", " ");
        if (normalizado.length() < LARGO_MINIMO || normalizado.length() > LARGO_MAXIMO) {
            throw new BusinessRuleException("Escribí una dirección de entre 5 y 200 caracteres, con localidad.");
        }
        return new ConsultaDireccion(normalizado);
    }
}

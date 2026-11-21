package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** A dónde le transfieren al comerciante: un alias o CVU en texto libre, sin validar su formato. */
public record DestinoDeCobro(String alias, String titular) {

    public static final int LARGO_MAXIMO = 120;

    public static final DestinoDeCobro SIN_CARGAR = new DestinoDeCobro(null, null);

    /** Un alias vacío quita el destino y un titular sin alias no se guarda. */
    public static DestinoDeCobro de(String alias, String titular) {
        String aliasLimpio = recortado(alias);
        if (aliasLimpio == null) {
            return SIN_CARGAR;
        }
        return new DestinoDeCobro(conLargoValido(aliasLimpio, "alias o CVU"),
                conLargoValido(recortado(titular), "titular"));
    }

    public boolean estaCargado() {
        return alias != null;
    }

    private static String recortado(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }

    private static String conLargoValido(String texto, String nombre) {
        if (texto != null && texto.length() > LARGO_MAXIMO) {
            throw new BusinessRuleException(
                    "El " + nombre + " no puede superar los " + LARGO_MAXIMO + " caracteres");
        }
        return texto;
    }
}

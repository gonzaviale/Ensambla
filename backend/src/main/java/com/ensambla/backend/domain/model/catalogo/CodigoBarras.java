package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;

public record CodigoBarras(String valor) {

    public CodigoBarras {
        if (valor == null || !valor.matches("(?:[0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14})")
                || valor.matches("0+")) {
            throw new BusinessRuleException("Ingresá un código GTIN de 8, 12, 13 o 14 dígitos.");
        }
        int suma = 0;
        for (int i = valor.length() - 2, peso = 3; i >= 0; i--, peso = 4 - peso) {
            suma += (valor.charAt(i) - '0') * peso;
        }
        if ((10 - suma % 10) % 10 != valor.charAt(valor.length() - 1) - '0') {
            throw new BusinessRuleException("El dígito de control del código no es válido.");
        }
    }

    public String identidad() {
        return "0".repeat(14 - valor.length()) + valor;
    }
}

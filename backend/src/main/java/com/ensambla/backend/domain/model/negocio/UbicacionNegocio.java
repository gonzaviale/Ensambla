package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Dirección de referencia y punto corregible; nunca incluye datos de otros negocios. */
public record UbicacionNegocio(String direccion, String localidad, BigDecimal latitud, BigDecimal longitud) {

    public UbicacionNegocio {
        direccion = texto(direccion, 500);
        localidad = texto(localidad, 120);
        latitud = coordenada(latitud, 90);
        longitud = coordenada(longitud, 180);
    }

    private static String texto(String valor, int maximo) {
        if (valor == null || valor.isBlank() || valor.length() > maximo) {
            throw new BusinessRuleException("La dirección y la localidad deben estar completas y dentro del límite");
        }
        return valor.strip();
    }

    private static BigDecimal coordenada(BigDecimal valor, int limite) {
        if (valor == null || valor.abs().compareTo(BigDecimal.valueOf(limite)) > 0) {
            throw new BusinessRuleException("Las coordenadas de la ubicación no son válidas");
        }
        return valor.setScale(6, RoundingMode.HALF_UP);
    }
}

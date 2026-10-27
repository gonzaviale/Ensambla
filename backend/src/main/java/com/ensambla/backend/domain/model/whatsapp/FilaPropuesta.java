package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.campo.Campo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Una fila lista para guardar, con cada valor tal como lo escribió la persona o lo leyó la IA.
 *
 * @param valores valor de cada campo, con el id del campo como clave
 * @param dudosos ids de los campos cuyo valor Sami no pudo leer con seguridad
 */
public record FilaPropuesta(Map<String, String> valores, Set<String> dudosos) {

    public FilaPropuesta {
        valores = Map.copyOf(valores);
        dudosos = Set.copyOf(dudosos);
    }

    /**
     * Los valores como se guardan en un registro: números como número y sí/no como booleano. Lo que no se puede
     * convertir queda como texto para que la validación del registro lo marque.
     */
    public Map<String, Object> tipados(List<Campo> campos) {
        Map<String, Object> tipados = new LinkedHashMap<>();
        for (Campo campo : campos) {
            String valor = valores.get(campo.getId().toString());
            if (valor != null) {
                tipados.put(campo.getId().toString(), tipar(campo, valor.trim()));
            }
        }
        return tipados;
    }

    private static Object tipar(Campo campo, String valor) {
        return switch (campo.getTipo()) {
            case NUMERO, MONEDA -> numero(valor);
            case SI_NO -> sino(valor);
            default -> valor;
        };
    }

    private static Object numero(String valor) {
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    private static Object sino(String valor) {
        return switch (valor.toLowerCase(java.util.Locale.ROOT)) {
            case "true", "si", "sí" -> Boolean.TRUE;
            case "false", "no" -> Boolean.FALSE;
            default -> valor;
        };
    }
}

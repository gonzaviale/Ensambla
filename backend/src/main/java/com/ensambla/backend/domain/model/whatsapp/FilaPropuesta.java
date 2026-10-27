package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.campo.Campo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Una fila lista para guardar, con cada valor tal como lo escribió la persona o lo leyó la IA.
 *
 * @param valores         valor de cada campo, con el id del campo como clave
 * @param dudosos         ids de los campos cuyo valor Sami no pudo leer con seguridad
 * @param registroId      id de la fila existente que esta actualiza; null si es una fila nueva
 * @param nombreExistente cómo se llama esa fila existente, para mostrarlo en el resumen
 * @param campoNombre     id del campo que nombra a las filas de la tabla: al actualizar no se cambia, porque lo que
 *                        la persona escribió sólo sirvió para encontrar la fila
 */
public record FilaPropuesta(Map<String, String> valores, Set<String> dudosos, String registroId,
                            String nombreExistente, String campoNombre) {

    public FilaPropuesta {
        valores = Map.copyOf(valores);
        dudosos = Set.copyOf(dudosos);
    }

    /** Una fila nueva. */
    public FilaPropuesta(Map<String, String> valores, Set<String> dudosos) {
        this(valores, dudosos, null, null, null);
    }

    public boolean actualizaUnaFila() {
        return registroId != null;
    }

    /** La misma fila, pero para actualizar la que ya existe con ese id y ese nombre. */
    public FilaPropuesta actualizando(String id, String nombre, String campoDelNombre) {
        return new FilaPropuesta(valores, dudosos, id, nombre, campoDelNombre);
    }

    /** Si hay algo para cambiar además del nombre con el que se encontró la fila. */
    public boolean cambiaAlgoAdemasDelNombre(String campoDelNombre) {
        return valores.keySet().stream().anyMatch(campo -> !campo.equals(campoDelNombre));
    }

    /** Lo que se le cambia a la fila existente: todo menos el nombre, que sólo sirvió para encontrarla. */
    public Map<String, Object> cambiosTipados(List<Campo> campos) {
        Map<String, Object> cambios = tipados(campos);
        cambios.remove(campoNombre);
        return cambios;
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
        return switch (valor.toLowerCase(Locale.ROOT)) {
            case "true", "si", "sí" -> Boolean.TRUE;
            case "false", "no" -> Boolean.FALSE;
            default -> valor;
        };
    }
}

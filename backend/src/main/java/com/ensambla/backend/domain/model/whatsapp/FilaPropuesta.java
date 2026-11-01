package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Una fila lista para guardar, con cada valor tal como lo escribió la persona o lo leyó la IA. Un número con signo
 * («+10», «-3») en una fila que ya existe se suma o se resta a lo que tenía, en lugar de pisarlo.
 *
 * @param valores         valor de cada campo, con el id del campo como clave
 * @param dudosos         ids de los campos cuyo valor Sami no pudo leer con seguridad
 * @param registroId      id de la fila existente que esta actualiza; null si es una fila nueva
 * @param nombreExistente cómo se llama esa fila existente, para mostrarlo en el resumen
 * @param campoNombre     id del campo que nombra a las filas de la tabla: al actualizar no se cambia, porque lo que
 *                        la persona escribió sólo sirvió para encontrar la fila
 * @param antes           lo que tenía la fila existente en los campos que se suman o restan, para el resumen
 * @param vinculos        la fila de otra tabla que nombra cada campo que se vincula, con el id del campo como clave
 */
public record FilaPropuesta(Map<String, String> valores, Set<String> dudosos, String registroId,
                            String nombreExistente, String campoNombre, Map<String, String> antes,
                            Map<String, VinculoPropuesto> vinculos) {

    private static final Set<TipoCampo> NUMERICOS = Set.of(TipoCampo.NUMERO, TipoCampo.MONEDA);
    private static final String RELATIVO = "[+-]\\s*[0-9]+([.,][0-9]+)?";

    public FilaPropuesta {
        valores = Map.copyOf(valores);
        dudosos = Set.copyOf(dudosos);
        antes = antes == null ? Map.of() : Map.copyOf(antes);
        vinculos = vinculos == null ? Map.of() : Map.copyOf(vinculos);
    }

    /** Una fila nueva. */
    public FilaPropuesta(Map<String, String> valores, Set<String> dudosos) {
        this(valores, dudosos, null, null, null, Map.of(), Map.of());
    }

    public boolean actualizaUnaFila() {
        return registroId != null;
    }

    /**
     * La misma fila, pero para actualizar la que ya existe con ese id y ese nombre.
     *
     * @param actuales los valores que tiene hoy esa fila: de ahí se toma lo que había en lo que se suma o se resta
     */
    public FilaPropuesta actualizando(String id, String nombre, String campoDelNombre, Map<String, Object> actuales) {
        Map<String, String> previos = new LinkedHashMap<>();
        valores.forEach((campo, valor) -> {
            if (esRelativo(valor) && actuales.get(campo) != null) {
                previos.put(campo, actuales.get(campo).toString());
            }
        });
        return new FilaPropuesta(valores, dudosos, id, nombre, campoDelNombre, previos, vinculos);
    }

    /** La misma fila, con el campo vinculado a esa fila de otra tabla. */
    public FilaPropuesta vinculando(String campo, VinculoPropuesto vinculo) {
        Map<String, VinculoPropuesto> todos = new LinkedHashMap<>(vinculos);
        todos.put(campo, vinculo);
        return new FilaPropuesta(valores, dudosos, registroId, nombreExistente, campoNombre, antes, todos);
    }

    /** La misma fila sin ese valor: un vínculo que no se puede resolver no se carga. */
    public FilaPropuesta sinValor(String campo) {
        Map<String, String> otros = new LinkedHashMap<>(valores);
        otros.remove(campo);
        return new FilaPropuesta(otros, dudosos, registroId, nombreExistente, campoNombre, antes, vinculos);
    }

    /** Si hay algo para cambiar además del nombre con el que se encontró la fila. */
    public boolean cambiaAlgoAdemasDelNombre(String campoDelNombre) {
        return valores.keySet().stream().anyMatch(campo -> !campo.equals(campoDelNombre));
    }

    /**
     * Lo que se le cambia a la fila existente: todo menos el nombre, que sólo sirvió para encontrarla. Un número con
     * signo se suma a lo que la fila tenía en {@code actuales} (sin bajar de 0).
     */
    public Map<String, Object> cambiosTipados(List<Campo> campos, Map<String, Object> actuales) {
        Map<String, Object> cambios = tipados(campos);
        cambios.remove(campoNombre);
        for (Campo campo : campos) {
            String clave = campo.getId().toString();
            String valor = valores.get(clave);
            if (NUMERICOS.contains(campo.getTipo()) && esRelativo(valor)) {
                cambios.put(clave, sumado(actuales.get(clave), cambio(valor)));
            }
        }
        return cambios;
    }

    /** Lo que había (0 si no era un número) más el cambio, sin bajar de 0. */
    public static BigDecimal sumado(Object habia, BigDecimal cambio) {
        BigDecimal base = habia != null && numero(habia.toString()) instanceof BigDecimal numero
                ? numero : BigDecimal.ZERO;
        return base.add(cambio).max(BigDecimal.ZERO);
    }

    /** «+10» o «-3»: un cambio sobre lo que ya hay, no el valor nuevo. */
    public static boolean esRelativo(String valor) {
        return valor != null && valor.strip().matches(RELATIVO);
    }

    /** El número con signo de un valor relativo. */
    public static BigDecimal cambio(String relativo) {
        return (BigDecimal) numero(relativo.strip());
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
            return new BigDecimal(valor.replace(',', '.').replaceAll("\\s", ""));
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

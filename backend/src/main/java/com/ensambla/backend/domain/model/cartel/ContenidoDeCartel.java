package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.List;

/**
 * Lo que dice el cartel. El protagonista (el precio, el «2×1») y las líneas salen de los datos del negocio; el titular
 * y la bajada los puede escribir la IA o la persona. Todo cabe en los máximos o se rechaza.
 */
public record ContenidoDeCartel(String negocio, String titular, String bajada, String protagonista, String detalle,
                                List<String> lineas) {

    public static final int MAXIMO_TITULAR = 40;
    public static final int MAXIMO_BAJADA = 90;
    public static final int MAXIMO_PROTAGONISTA = 20;
    public static final int MAXIMO_DETALLE = 60;
    public static final int MAXIMO_LINEA = 40;

    public ContenidoDeCartel {
        negocio = recortado(negocio);
        titular = exigirHasta(recortado(titular), MAXIMO_TITULAR, "titular");
        bajada = exigirHasta(recortado(bajada), MAXIMO_BAJADA, "bajada");
        protagonista = exigirHasta(recortado(protagonista), MAXIMO_PROTAGONISTA, "protagonista");
        detalle = exigirHasta(recortado(detalle), MAXIMO_DETALLE, "detalle");
        lineas = lineasValidas(lineas);
    }

    private static List<String> lineasValidas(List<String> lineas) {
        List<String> recortadas = (lineas == null ? List.<String>of() : lineas).stream()
                .map(ContenidoDeCartel::recortado)
                .toList();
        recortadas.forEach(linea -> exigirHasta(linea, MAXIMO_LINEA, "línea"));
        return recortadas;
    }

    private static String recortado(String texto) {
        return texto == null ? "" : texto.strip();
    }

    private static String exigirHasta(String texto, int maximo, String nombre) {
        if (texto.length() > maximo) {
            throw new BusinessRuleException("El " + nombre + " admite hasta " + maximo + " caracteres");
        }
        return texto;
    }
}

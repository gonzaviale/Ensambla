package com.ensambla.backend.domain.dto.procesamiento;

import java.util.List;

/**
 * Una foto suelta para leer contra un bloque. No se guarda nada: la persona revisa lo leído y recién ahí lo incorpora.
 *
 * @param nombreBloque     nombre del bloque al que irían las filas
 * @param campos           campos del bloque que la IA puede leer
 * @param destinoExistente {@code true} si el bloque ya existe y por eso tiene sentido proponer las columnas que le faltan
 */
public record LeerImagenRequest(byte[] contenido, String nombreBloque, List<CampoItem> campos,
                                boolean destinoExistente) {

    /**
     * @param clave identificador que usa quien llama; las filas leídas vuelven indexadas por esta clave
     * @param tipo  nombre de un {@code TipoCampo}
     */
    public record CampoItem(String clave, String nombre, String tipo, List<String> opciones) {
    }
}

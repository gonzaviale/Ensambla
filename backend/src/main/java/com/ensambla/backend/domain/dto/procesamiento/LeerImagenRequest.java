package com.ensambla.backend.domain.dto.procesamiento;

import java.util.List;
import java.util.UUID;

/**
 * Una foto suelta para leer contra un bloque. Se cuenta el consumo, pero las filas se incorporan luego de revisarlas.
 *
 * @param usuarioId        usuario que pide la lectura; su negocio es el que consume la cuota de IA
 * @param nombreBloque     nombre del bloque al que irían las filas
 * @param campos           campos del bloque que la IA puede leer
 * @param destinoExistente {@code true} si el bloque ya existe y por eso tiene sentido proponer las columnas que le faltan
 */
public record LeerImagenRequest(UUID usuarioId, byte[] contenido, String nombreBloque, List<CampoItem> campos,
                                boolean destinoExistente) {

    /**
     * @param clave identificador que usa quien llama; las filas leídas vuelven indexadas por esta clave
     * @param tipo  nombre de un {@code TipoCampo}
     */
    public record CampoItem(String clave, String nombre, String tipo, List<String> opciones) {
    }
}

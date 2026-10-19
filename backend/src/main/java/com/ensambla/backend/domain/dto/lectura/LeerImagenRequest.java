package com.ensambla.backend.domain.dto.lectura;

import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.List;

/**
 * Una foto suelta para leer contra un bloque. No se guarda nada: la persona revisa lo leído y recién ahí lo incorpora.
 *
 * @param nombreBloque    nombre del bloque al que irían las filas
 * @param campos          campos del bloque que la IA puede leer; {@code clave} es el identificador que usa quien llama
 * @param destinoExistente {@code true} si el bloque ya existe y por eso tiene sentido proponer las columnas que le faltan
 */
public record LeerImagenRequest(byte[] contenido, String tipoContenido, String nombreBloque,
                                List<CampoImagen> campos, boolean destinoExistente) {

    public record CampoImagen(String clave, String nombre, TipoCampo tipo, List<String> opciones) {

        public CampoImagen {
            opciones = opciones == null ? List.of() : List.copyOf(opciones);
        }
    }
}

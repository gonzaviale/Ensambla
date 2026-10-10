package com.ensambla.backend.domain.model.lectura;

/**
 * Una página fotografiada lista para mandar a leer.
 *
 * @param numero        orden de la página dentro del lote, empezando en 1
 * @param contenido     bytes de la imagen (ya comprimida y rotada en el cliente)
 * @param tipoContenido media type, por ejemplo {@code image/jpeg}
 */
public record PaginaCuaderno(int numero, byte[] contenido, String tipoContenido) {
}

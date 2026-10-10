package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.archivo.UrlFirmada;

/**
 * Almacenamiento privado de archivos (fotos del cuaderno). Los archivos nunca se exponen directamente: el cliente
 * sube y lee con URLs firmadas que vencen. La implementación vive en {@code infrastructure.almacenamiento}.
 */
public interface AlmacenamientoArchivosPort {

    /**
     * Guarda un archivo que recibió el backend.
     *
     * @param clave         ruta del archivo dentro del almacenamiento, por ejemplo {@code negocios/{id}/fotos/{id}.jpg}
     * @param contenido     bytes del archivo
     * @param tipoContenido media type con el que se va a servir
     */
    void guardar(String clave, byte[] contenido, String tipoContenido);

    /** Borra un archivo; si no existe no hace nada. */
    void eliminar(String clave);

    /**
     * @param clave         ruta del archivo dentro del almacenamiento, por ejemplo {@code negocios/{id}/fotos/{id}.jpg}
     * @param tipoContenido media type que el cliente tiene que mandar en el header {@code Content-Type} al subir
     * @return URL para un {@code PUT} del archivo
     */
    UrlFirmada generarUrlSubida(String clave, String tipoContenido);

    /**
     * @param clave ruta del archivo dentro del almacenamiento
     * @return URL para un {@code GET} del archivo
     */
    UrlFirmada generarUrlLectura(String clave);
}

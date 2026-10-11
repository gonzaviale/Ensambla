package com.ensambla.backend.domain.port;

import java.util.UUID;

/**
 * Archivos privados del cuaderno, por negocio. Los bytes que se leen deben estar comprimidos y rotados por la subida.
 * Las claves tienen la forma {@code {negocioId}/…}; cualquier clave fuera de esa carpeta se rechaza.
 */
public interface ArchivoCuadernoPort {

    byte[] leer(UUID negocioId, String clave);

    void guardar(UUID negocioId, String clave, byte[] contenido);

    void eliminar(UUID negocioId, String clave);
}

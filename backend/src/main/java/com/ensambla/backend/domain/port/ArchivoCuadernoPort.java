package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.negocio.NegocioId;

/**
 * Archivos privados del cuaderno, por negocio. Los bytes que se leen deben estar comprimidos y rotados por la subida.
 * Las claves tienen la forma {@code {negocioId}/…}; cualquier clave fuera de esa carpeta se rechaza.
 */
public interface ArchivoCuadernoPort {

    byte[] leer(NegocioId negocioId, String clave);

    void guardar(NegocioId negocioId, String clave, byte[] contenido);

    void eliminar(NegocioId negocioId, String clave);
}

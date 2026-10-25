package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;

/**
 * Archivos privados del cuaderno, por negocio. Los bytes que se leen deben estar comprimidos y rotados por la subida.
 * Las claves tienen la forma {@code {negocioId}/…}; cualquier clave fuera de esa carpeta se rechaza.
 */
public interface ArchivoCuadernoPort {

    byte[] leer(NegocioId negocioId, String clave);

    void guardar(NegocioId negocioId, String clave, byte[] contenido);

    void eliminar(NegocioId negocioId, String clave);

    /** Una foto cuyo archivo ya no está o no se puede abrir se muestra como no disponible, no como error. */
    default ArchivoFoto leerFoto(NegocioId negocioId, FotoCuaderno foto) {
        try {
            return new ArchivoFoto(leer(negocioId, foto.getClaveAlmacenamiento()), foto.getTipoContenido());
        } catch (LecturaCuadernoException e) {
            throw new ResourceNotFoundException("La foto ya no está disponible");
        }
    }
}

package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;

import java.util.List;

/**
 * Lee páginas de un cuaderno con IA y propone cómo guardarlas. El dominio no sabe qué proveedor ni qué modelo
 * hay detrás: cambiar de proveedor sólo toca la implementación en {@code infrastructure.ia}.
 */
public interface LectorCuadernoPort {

    /**
     * @param paginas           páginas del lote, en orden
     * @param bloquesExistentes bloques del negocio para que la IA pueda sumar las filas a uno de ellos
     * @throws com.ensambla.backend.domain.exception.LecturaCuadernoException si la lectura falla o no es válida
     */
    LecturaCuaderno leer(List<PaginaCuaderno> paginas, List<BloqueConocido> bloquesExistentes);
}

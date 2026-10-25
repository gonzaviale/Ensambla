package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.util.List;

public interface AgregadoZonaRepository {

    List<AgregadoZonaProducto> findByZonaId(ZonaId zonaId);

    /** Comercios de la zona que aceptaron aportar a los datos agregados. */
    long contarComerciosQueComparten(ZonaId zonaId);

    default AnalisisZona analizar(Zona zona) {
        return new AnalisisZona(
                zona.getNombre(), contarComerciosQueComparten(zona.getId()), findByZonaId(zona.getId()));
    }
}

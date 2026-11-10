package com.ensambla.backend.domain.repository.zona;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;

import java.util.List;

public interface DatosDondeAbrirRepository {

    /** Snapshot completo para comparar zonas, cargado en lote para evitar una consulta por zona. */
    List<DatosZonaDondeAbrir> buscar(RubroNegocio rubro);
}

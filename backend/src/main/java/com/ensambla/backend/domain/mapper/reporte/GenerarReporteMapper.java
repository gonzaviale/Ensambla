package com.ensambla.backend.domain.mapper.reporte;

import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.dto.registro.TotalesItem;
import com.ensambla.backend.domain.dto.reporte.GenerarReporteResponse;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;

public class GenerarReporteMapper {

    public GenerarReporteResponse toResponse(ReporteRegistros reporte, int maximoFilas) {
        return new GenerarReporteResponse(
                reporte.filas().stream().map(RegistroItem::de).toList(),
                TotalesItem.de(reporte.totales()),
                reporte.truncado(),
                maximoFilas);
    }
}

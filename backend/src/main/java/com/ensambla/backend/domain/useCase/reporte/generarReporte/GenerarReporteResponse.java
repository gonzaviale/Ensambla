package com.ensambla.backend.domain.useCase.reporte.generarReporte;

import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;
import com.ensambla.backend.domain.useCase.registro.TotalesItem;

import java.util.List;

/** Las primeras filas del reporte, los totales de todas y si hay más filas de las que se muestran. */
public record GenerarReporteResponse(
        List<RegistroItem> filas,
        TotalesItem totales,
        boolean truncado,
        int maximoFilas) {

    public static GenerarReporteResponse de(ReporteRegistros reporte, int maximoFilas) {
        return new GenerarReporteResponse(
                reporte.filas().stream().map(RegistroItem::de).toList(),
                TotalesItem.de(reporte.totales()),
                reporte.truncado(),
                maximoFilas);
    }
}

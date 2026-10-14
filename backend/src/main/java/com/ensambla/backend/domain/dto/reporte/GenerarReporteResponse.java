package com.ensambla.backend.domain.dto.reporte;

import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.dto.registro.TotalesItem;

import java.util.List;

/** Las primeras filas del reporte, los totales de todas y si hay más filas de las que se muestran. */
public record GenerarReporteResponse(
        List<RegistroItem> filas,
        TotalesItem totales,
        boolean truncado,
        int maximoFilas) {
}

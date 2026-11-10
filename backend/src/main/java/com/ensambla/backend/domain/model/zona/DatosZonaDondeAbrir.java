package com.ensambla.backend.domain.model.zona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DatosZonaDondeAbrir(
        ZonaId id,
        String nombre,
        String localidad,
        String partido,
        String provincia,
        BigDecimal latitud,
        BigDecimal longitud,
        Map<FactorDondeAbrir, Medicion> metricas,
        List<Punto> puntos) {

    public record Medicion(BigDecimal valor, String unidad, Fuente fuente, LocalDate observadaEn) {
    }

    public record Fuente(String codigo, String nombre, String tipo, String url, String licencia) {
    }

    public record Punto(String clave, String tipo, String rubro, String nombre, BigDecimal latitud,
                        BigDecimal longitud, Fuente fuente, LocalDate observadaEn) {
    }
}

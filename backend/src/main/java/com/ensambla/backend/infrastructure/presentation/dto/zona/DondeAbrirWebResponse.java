package com.ensambla.backend.infrastructure.presentation.dto.zona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DondeAbrirWebResponse(
        NegocioActualWebResponse negocioActual,
        int evaluadas,
        List<ZonaEvaluadaWebResponse> ranking,
        List<ZonaDescartadaWebResponse> descartadas,
        List<String> avisos) {

    public record NegocioActualWebResponse(String nombre, String rubro, String direccion, BigDecimal latitud,
                                           BigDecimal longitud, String zonaId) {
    }

    public record ZonaEvaluadaWebResponse(
            String id,
            String nombre,
            String localidad,
            String jurisdiccion,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal distanciaKm,
            BigDecimal puntaje,
            String coincidencia,
            List<AporteWebResponse> aportes,
            List<String> porQue,
            List<String> riesgos,
            List<PuntoWebResponse> puntos) {
    }

    public record AporteWebResponse(String factor, String etiqueta, BigDecimal valor, String unidad,
                                    BigDecimal peso, BigDecimal puntaje, BigDecimal aporte,
                                    FuenteWebResponse fuente, LocalDate observadaEn) {
    }

    public record PuntoWebResponse(String clave, String tipo, String rubro, String nombre, BigDecimal latitud,
                                   BigDecimal longitud, FuenteWebResponse fuente, LocalDate observadaEn) {
    }

    public record FuenteWebResponse(String codigo, String nombre, String tipo, String url, String licencia) {
    }

    public record ZonaDescartadaWebResponse(String id, String nombre, String localidad, String jurisdiccion,
                                            BigDecimal latitud, BigDecimal longitud, BigDecimal distanciaKm,
                                            List<String> motivos) {
    }
}

package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExplorarDondeAbrirResponse(
        NegocioActual negocioActual,
        int evaluadas,
        List<ZonaEvaluada> ranking,
        List<ZonaDescartada> descartadas,
        List<String> avisos) {

    public record NegocioActual(String nombre, RubroNegocio rubro, String direccion, BigDecimal latitud,
                                BigDecimal longitud, ZonaId zonaId) {
    }

    public record ZonaEvaluada(
            ZonaId id,
            String nombre,
            String localidad,
            String jurisdiccion,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal distanciaKm,
            BigDecimal puntaje,
            String coincidencia,
            List<Aporte> aportes,
            List<String> porQue,
            List<String> riesgos,
            List<DatosZonaDondeAbrir.Punto> puntos) {
    }

    public record Aporte(FactorDondeAbrir factor, String etiqueta, BigDecimal valor, String unidad,
                         BigDecimal peso, BigDecimal puntaje, BigDecimal aporte,
                         DatosZonaDondeAbrir.Fuente fuente, LocalDate observadaEn) {
    }

    public record ZonaDescartada(ZonaId id, String nombre, String localidad, String jurisdiccion,
                                 BigDecimal latitud, BigDecimal longitud, BigDecimal distanciaKm,
                                 List<String> motivos) {
    }
}

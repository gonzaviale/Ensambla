package com.ensambla.backend.domain.model.zona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Barrio global donde se agrupan los comercios: un centro y un radio. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zona {

    private static final double RADIO_TERRESTRE_METROS = 6_371_000d;

    private UUID id;

    private String nombre;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private Integer radioMetros;

    /** Distancia sobre la superficie terrestre (haversine) entre el centro de la zona y el punto. */
    public double distanciaMetros(BigDecimal latitudPunto, BigDecimal longitudPunto) {
        double lat1 = Math.toRadians(latitud.doubleValue());
        double lat2 = Math.toRadians(latitudPunto.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(longitudPunto.doubleValue() - longitud.doubleValue());
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * RADIO_TERRESTRE_METROS * Math.asin(Math.sqrt(a));
    }

    public boolean contiene(BigDecimal latitudPunto, BigDecimal longitudPunto) {
        return distanciaMetros(latitudPunto, longitudPunto) <= radioMetros;
    }
}

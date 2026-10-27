package com.ensambla.backend.domain.model.zona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Barrio global donde se agrupan los comercios: un centro y un radio. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zona {

    private static final double RADIO_TERRESTRE_METROS = 6_371_000d;

    private ZonaId id;

    private String nombre;

    private String localidad;

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

    /** Zona cuyo radio contiene el punto; si hay varias, la de centro más cercano. */
    public static Optional<Zona> masCercana(List<Zona> zonas, BigDecimal latitudPunto, BigDecimal longitudPunto) {
        return zonas.stream()
                .filter(zona -> zona.contiene(latitudPunto, longitudPunto))
                .min(Comparator.comparingDouble(zona -> zona.distanciaMetros(latitudPunto, longitudPunto)));
    }

    /** El texto escrito nombra a la zona o a su localidad, sin importar mayúsculas ni tildes. */
    public boolean coincideCon(String texto) {
        String buscado = normalizar(texto);
        return !buscado.isEmpty()
                && (normalizar(nombre).contains(buscado) || normalizar(localidad).contains(buscado));
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .strip();
    }
}

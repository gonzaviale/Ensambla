package com.ensambla.backend.infrastructure.geocodificacion;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class NominatimGeocodificadorAdapter implements GeocodificadorPort {

    static final int MAXIMO_CACHE = 256;
    private static final Duration VIGENCIA_CACHE = Duration.ofDays(1);
    private static final Duration INTERVALO_CONSULTAS = Duration.ofMillis(1100);
    private static final String MENSAJE_NO_DISPONIBLE = "No pudimos buscar la dirección. Probá de nuevo en un momento.";
    private static final String MENSAJE_LOCALIDAD_NO_DISPONIBLE =
            "No pudimos verificar la localidad. Probá de nuevo en un momento.";
    // Del más específico al más amplio que sigue siendo una localidad; barrios y partidos no cuentan.
    private static final List<String> CLAVES_LOCALIDAD =
            List.of("city", "town", "village", "municipality", "city_district", "suburb");

    private final RestClient restClient;
    private final LongSupplier tiempoMonotono;
    private final Map<String, Entrada<List<DireccionEncontrada>>> cacheDirecciones = new LinkedHashMap<>();
    private final Map<String, Entrada<LocalidadesDelPunto>> cacheLocalidades = new LinkedHashMap<>();
    private long proximaConsulta = Long.MIN_VALUE;

    @Override
    public synchronized List<DireccionEncontrada> buscar(String consulta, Instant instante) {
        return consultarConCache(cacheDirecciones, consulta.toLowerCase(Locale.ROOT), instante,
                () -> consultarDirecciones(consulta));
    }

    @Override
    public synchronized LocalidadesDelPunto resolverLocalidades(BigDecimal latitud, BigDecimal longitud,
                                                                Instant instante) {
        BigDecimal lat = latitud.setScale(6, RoundingMode.HALF_UP);
        BigDecimal lon = longitud.setScale(6, RoundingMode.HALF_UP);
        return consultarConCache(cacheLocalidades, lat.toPlainString() + "," + lon.toPlainString(), instante,
                () -> consultarLocalidades(lat, lon));
    }

    // Una única puerta de salida por instancia: también limita consultas concurrentes de distintos usuarios.
    private <T> T consultarConCache(Map<String, Entrada<T>> cache, String clave, Instant instante,
                                    Supplier<T> proveedor) {
        Entrada<T> entrada = cache.get(clave);
        if (entrada != null && instante.isBefore(entrada.vence())) {
            return entrada.valor();
        }
        long ahora = tiempoMonotono.getAsLong();
        if (ahora < proximaConsulta) {
            throw new ServicioUbicacionException("Esperá un segundo antes de buscar otra dirección.");
        }
        proximaConsulta = ahora + INTERVALO_CONSULTAS.toNanos();
        T valor = proveedor.get();
        cache.remove(clave);
        if (cache.size() >= MAXIMO_CACHE) {
            cache.remove(cache.keySet().iterator().next());
        }
        cache.put(clave, new Entrada<>(valor, instante.plus(VIGENCIA_CACHE)));
        return valor;
    }

    private List<DireccionEncontrada> consultarDirecciones(String consulta) {
        try {
            ResultadoNominatim[] respuesta = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", "{consulta}")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 5)
                            .queryParam("countrycodes", "ar")
                            .queryParam("accept-language", "es")
                            .queryParam("addressdetails", 1)
                            .build(consulta))
                    .retrieve()
                    .body(ResultadoNominatim[].class);
            return Arrays.stream(Objects.requireNonNull(respuesta)).map(this::toDomain).toList();
        } catch (RestClientException | IllegalArgumentException | NullPointerException exception) {
            // No se registran consultas, coordenadas ni cuerpos del proveedor.
            throw new ServicioUbicacionException(MENSAJE_NO_DISPONIBLE);
        }
    }

    private LocalidadesDelPunto consultarLocalidades(BigDecimal latitud, BigDecimal longitud) {
        try {
            ResultadoReverso respuesta = restClient.get()
                    .uri(uri -> uri.path("/reverse")
                            .queryParam("lat", "{latitud}")
                            .queryParam("lon", "{longitud}")
                            .queryParam("format", "jsonv2")
                            .queryParam("zoom", 14)
                            .queryParam("addressdetails", 1)
                            .queryParam("accept-language", "es")
                            .build(latitud.toPlainString(), longitud.toPlainString()))
                    .retrieve()
                    .body(ResultadoReverso.class);
            return new LocalidadesDelPunto(nombresDeLocalidad(Objects.requireNonNull(respuesta).detalle()));
        } catch (RestClientException | NullPointerException exception) {
            // No se registran coordenadas ni cuerpos del proveedor.
            throw new ServicioUbicacionException(MENSAJE_LOCALIDAD_NO_DISPONIBLE);
        }
    }

    private DireccionEncontrada toDomain(ResultadoNominatim resultado) {
        String direccion = Objects.requireNonNull(resultado.direccion());
        BigDecimal latitud = new BigDecimal(resultado.latitud());
        BigDecimal longitud = new BigDecimal(resultado.longitud());
        if (direccion.isBlank() || latitud.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || longitud.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("Respuesta geográfica inválida");
        }
        String localidad = nombresDeLocalidad(resultado.detalle()).stream().findFirst().orElse(null);
        return new DireccionEncontrada(direccion, latitud, longitud, localidad);
    }

    private List<String> nombresDeLocalidad(Map<String, String> detalle) {
        if (detalle == null) {
            return List.of();
        }
        return CLAVES_LOCALIDAD.stream()
                .map(detalle::get)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .distinct()
                .toList();
    }

    private record Entrada<T>(T valor, Instant vence) {
    }

    private record ResultadoNominatim(
            @JsonProperty("display_name") String direccion,
            @JsonProperty("lat") String latitud,
            @JsonProperty("lon") String longitud,
            @JsonProperty("address") Map<String, String> detalle) {
    }

    // Sin resultado, el proveedor responde {"error": "..."}: no trae "address" y la localidad queda sin reconocer.
    private record ResultadoReverso(@JsonProperty("address") Map<String, String> detalle) {
    }
}

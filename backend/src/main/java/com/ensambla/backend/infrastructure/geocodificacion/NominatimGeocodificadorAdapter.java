package com.ensambla.backend.infrastructure.geocodificacion;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

@RequiredArgsConstructor
public class NominatimGeocodificadorAdapter implements GeocodificadorPort {

    static final int MAXIMO_CACHE = 256;
    private static final Duration VIGENCIA_CACHE = Duration.ofDays(1);
    private static final Duration INTERVALO_CONSULTAS = Duration.ofMillis(1100);
    private static final String MENSAJE_NO_DISPONIBLE = "No pudimos buscar la dirección. Probá de nuevo en un momento.";

    private final RestClient restClient;
    private final LongSupplier tiempoMonotono;
    private final Map<String, EntradaCache> cache = new LinkedHashMap<>();
    private long proximaConsulta = Long.MIN_VALUE;

    // Una única puerta de salida por instancia: también limita consultas concurrentes de distintos usuarios.
    @Override
    public synchronized List<DireccionEncontrada> buscar(String consulta, Instant instante) {
        String clave = consulta.toLowerCase(Locale.ROOT);
        EntradaCache entrada = cache.get(clave);
        if (entrada != null && instante.isBefore(entrada.vence())) {
            return entrada.resultados();
        }
        long ahora = tiempoMonotono.getAsLong();
        if (ahora < proximaConsulta) {
            throw new ServicioUbicacionException("Esperá un segundo antes de buscar otra dirección.");
        }
        proximaConsulta = ahora + INTERVALO_CONSULTAS.toNanos();
        List<DireccionEncontrada> resultados = consultarProveedor(consulta);
        cache.remove(clave);
        if (cache.size() >= MAXIMO_CACHE) {
            cache.remove(cache.keySet().iterator().next());
        }
        cache.put(clave, new EntradaCache(resultados, instante.plus(VIGENCIA_CACHE)));
        return resultados;
    }

    private List<DireccionEncontrada> consultarProveedor(String consulta) {
        try {
            ResultadoNominatim[] respuesta = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", "{consulta}")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 5)
                            .queryParam("countrycodes", "ar")
                            .queryParam("accept-language", "es")
                            .build(consulta))
                    .retrieve()
                    .body(ResultadoNominatim[].class);
            return Arrays.stream(Objects.requireNonNull(respuesta)).map(this::toDomain).toList();
        } catch (RestClientException | IllegalArgumentException | NullPointerException exception) {
            // No se registran consultas, coordenadas ni cuerpos del proveedor.
            throw new ServicioUbicacionException(MENSAJE_NO_DISPONIBLE);
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
        return new DireccionEncontrada(direccion, latitud, longitud);
    }

    private record EntradaCache(List<DireccionEncontrada> resultados, Instant vence) {
    }

    private record ResultadoNominatim(
            @JsonProperty("display_name") String direccion,
            @JsonProperty("lat") String latitud,
            @JsonProperty("lon") String longitud) {
    }
}

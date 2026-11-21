package com.ensambla.backend.infrastructure.entorno;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Fuente;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Punto;
import com.ensambla.backend.domain.port.EntornoDelPuntoPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mide en vivo lo que hay a 500 m de un punto con OpenStreetMap (Overpass). Usa las mismas reglas de clasificación que
 * el snapshot de scripts/importar-datos-zona.ps1, así que un punto elegido y una zona precargada se comparan igual.
 */
@RequiredArgsConstructor
public class OverpassEntornoAdapter implements EntornoDelPuntoPort {

    static final int MAXIMO_CACHE = 128;
    private static final Duration VIGENCIA_CACHE = Duration.ofDays(1);
    private static final int RADIO_METROS = 500;
    private static final int DECIMALES_DE_LA_CLAVE = 4;
    private static final String MENSAJE_NO_DISPONIBLE =
            "No pudimos medir ese lugar ahora. Probá de nuevo en un momento.";
    private static final Fuente OPEN_STREET_MAP = new Fuente(
            "OSM", "OpenStreetMap", "COMUNITARIA", "https://www.openstreetmap.org/", "ODbL 1.0");
    private static final List<String> SELECTORES = List.of(
            "[amenity~\"^(school|kindergarten|college|university|hospital|clinic|doctors|pharmacy|bank|atm)$\"]",
            "[office]",
            "[public_transport]",
            "[highway=bus_stop]",
            "[railway~\"^(station|halt|subway_entrance|tram_stop)$\"]",
            "[leisure~\"^(park|garden|playground)$\"]",
            "[shop~\"^(convenience|supermarket|greengrocer|bakery|deli|kiosk|stationery|books|copyshop)$\"]");

    private final RestClient restClient;
    private final Map<String, Entrada> cache = new LinkedHashMap<>();

    // Una única puerta de salida por instancia, como la geocodificación: cuida el servicio público compartido.
    @Override
    public synchronized List<Punto> consultar(BigDecimal latitud, BigDecimal longitud, Instant instante) {
        String clave = claveDelLugar(latitud, longitud);
        Entrada entrada = cache.get(clave);
        if (entrada != null && instante.isBefore(entrada.vence())) {
            return entrada.puntos();
        }
        List<Punto> puntos = consultarFuente(latitud, longitud, LocalDate.ofInstant(instante, ZoneOffset.UTC));
        guardarEnLaCache(clave, puntos, instante);
        return puntos;
    }

    private String claveDelLugar(BigDecimal latitud, BigDecimal longitud) {
        return latitud.setScale(DECIMALES_DE_LA_CLAVE, RoundingMode.HALF_UP).toPlainString() + ","
                + longitud.setScale(DECIMALES_DE_LA_CLAVE, RoundingMode.HALF_UP).toPlainString();
    }

    private void guardarEnLaCache(String clave, List<Punto> puntos, Instant instante) {
        cache.remove(clave);
        if (cache.size() >= MAXIMO_CACHE) {
            cache.remove(cache.keySet().iterator().next());
        }
        cache.put(clave, new Entrada(puntos, instante.plus(VIGENCIA_CACHE)));
    }

    private List<Punto> consultarFuente(BigDecimal latitud, BigDecimal longitud, LocalDate fecha) {
        try {
            Respuesta respuesta = pedirLosLugares(latitud, longitud);
            return puntos(Objects.requireNonNull(respuesta).elementos(), fecha);
        } catch (RestClientException | NullPointerException exception) {
            // No se registran coordenadas ni cuerpos del proveedor.
            throw new ServicioUbicacionException(MENSAJE_NO_DISPONIBLE);
        }
    }

    private Respuesta pedirLosLugares(BigDecimal latitud, BigDecimal longitud) {
        return restClient.get()
                .uri(uri -> uri.path("/api/interpreter")
                        .queryParam("data", "{consulta}")
                        .build(consulta(latitud, longitud)))
                .retrieve()
                .body(Respuesta.class);
    }

    private static String consulta(BigDecimal latitud, BigDecimal longitud) {
        String alrededor = "(around:" + RADIO_METROS + "," + latitud.toPlainString() + ","
                + longitud.toPlainString() + ")";
        String selectores = SELECTORES.stream()
                .map(selector -> "nwr" + alrededor + selector + ";")
                .collect(Collectors.joining());
        return "[out:json][timeout:20];(" + selectores + ");out center tags;";
    }

    private static List<Punto> puntos(List<Elemento> elementos, LocalDate fecha) {
        return Objects.requireNonNullElse(elementos, List.<Elemento>of()).stream()
                .flatMap(elemento -> puntosDe(elemento, fecha).stream())
                .toList();
    }

    private static List<Punto> puntosDe(Elemento elemento, LocalDate fecha) {
        Optional<Double> latitud = elemento.latitudDelLugar();
        Optional<Double> longitud = elemento.longitudDelLugar();
        if (latitud.isEmpty() || longitud.isEmpty()) {
            return List.of();
        }
        Map<String, String> etiquetas = elemento.etiquetasOVacias();
        String nombre = etiquetas.getOrDefault("name", "Lugar sin nombre");
        return ClasificacionOsm.de(etiquetas).stream()
                .map(clasificacion -> new Punto(elemento.identificador(), clasificacion.categoria(),
                        clasificacion.subcategoria(), nombre, BigDecimal.valueOf(latitud.get()),
                        BigDecimal.valueOf(longitud.get()), OPEN_STREET_MAP, fecha))
                .toList();
    }

    private record Entrada(List<Punto> puntos, Instant vence) {
    }

    private record Respuesta(@JsonProperty("elements") List<Elemento> elementos) {
    }

    private record Elemento(
            @JsonProperty("type") String tipo,
            @JsonProperty("id") long id,
            @JsonProperty("lat") Double latitud,
            @JsonProperty("lon") Double longitud,
            @JsonProperty("center") Centro centro,
            @JsonProperty("tags") Map<String, String> etiquetas) {

        String identificador() {
            return tipo + ":" + id;
        }

        /** La latitud del lugar o, si es un área, la de su centro. */
        Optional<Double> latitudDelLugar() {
            return Optional.ofNullable(latitud).or(() -> Optional.ofNullable(centro).map(Centro::latitud));
        }

        Optional<Double> longitudDelLugar() {
            return Optional.ofNullable(longitud).or(() -> Optional.ofNullable(centro).map(Centro::longitud));
        }

        Map<String, String> etiquetasOVacias() {
            return Objects.requireNonNullElse(etiquetas, Map.of());
        }
    }

    private record Centro(@JsonProperty("lat") Double latitud, @JsonProperty("lon") Double longitud) {
    }
}

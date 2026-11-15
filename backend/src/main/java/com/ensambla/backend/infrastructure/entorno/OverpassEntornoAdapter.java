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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private static final Set<String> ESCUELAS = Set.of("school", "kindergarten", "college", "university");
    private static final Set<String> VIAS_FERREAS = Set.of("station", "halt", "subway_entrance", "tram_stop");
    private static final Set<String> PARQUES = Set.of("park", "garden", "playground");
    private static final Set<String> SALUD = Set.of("hospital", "clinic", "doctors");
    private static final Set<String> COMPLEMENTARIOS_AMENITY = Set.of("pharmacy", "bank", "atm");
    private static final Set<String> ALMACENES = Set.of("convenience", "supermarket", "greengrocer", "bakery", "deli");
    private static final Set<String> KIOSCOS = Set.of("kiosk", "convenience");
    private static final Set<String> PAPELERIAS = Set.of("stationery", "books", "copyshop");

    private final RestClient restClient;
    private final Map<String, Entrada> cache = new LinkedHashMap<>();

    // Una única puerta de salida por instancia, como la geocodificación: cuida el servicio público compartido.
    @Override
    public synchronized List<Punto> consultar(BigDecimal latitud, BigDecimal longitud, Instant instante) {
        String clave = latitud.setScale(DECIMALES_DE_LA_CLAVE, RoundingMode.HALF_UP).toPlainString() + ","
                + longitud.setScale(DECIMALES_DE_LA_CLAVE, RoundingMode.HALF_UP).toPlainString();
        Entrada entrada = cache.get(clave);
        if (entrada != null && instante.isBefore(entrada.vence())) {
            return entrada.puntos();
        }
        List<Punto> puntos = consultarFuente(latitud, longitud, LocalDate.ofInstant(instante, ZoneOffset.UTC));
        cache.remove(clave);
        if (cache.size() >= MAXIMO_CACHE) {
            cache.remove(cache.keySet().iterator().next());
        }
        cache.put(clave, new Entrada(puntos, instante.plus(VIGENCIA_CACHE)));
        return puntos;
    }

    private List<Punto> consultarFuente(BigDecimal latitud, BigDecimal longitud, LocalDate fecha) {
        try {
            Respuesta respuesta = restClient.get()
                    .uri(uri -> uri.path("/api/interpreter")
                            .queryParam("data", "{consulta}")
                            .build(consulta(latitud, longitud)))
                    .retrieve()
                    .body(Respuesta.class);
            return puntos(Objects.requireNonNull(respuesta).elementos(), fecha);
        } catch (RestClientException | NullPointerException exception) {
            // No se registran coordenadas ni cuerpos del proveedor.
            throw new ServicioUbicacionException(MENSAJE_NO_DISPONIBLE);
        }
    }

    private static String consulta(BigDecimal latitud, BigDecimal longitud) {
        String alrededor = "(around:" + RADIO_METROS + "," + latitud.toPlainString() + ","
                + longitud.toPlainString() + ")";
        return "[out:json][timeout:20];("
                + "nwr" + alrededor + "[amenity~\"^(school|kindergarten|college|university|hospital|clinic|doctors"
                + "|pharmacy|bank|atm)$\"];"
                + "nwr" + alrededor + "[office];"
                + "nwr" + alrededor + "[public_transport];"
                + "nwr" + alrededor + "[highway=bus_stop];"
                + "nwr" + alrededor + "[railway~\"^(station|halt|subway_entrance|tram_stop)$\"];"
                + "nwr" + alrededor + "[leisure~\"^(park|garden|playground)$\"];"
                + "nwr" + alrededor + "[shop~\"^(convenience|supermarket|greengrocer|bakery|deli|kiosk"
                + "|stationery|books|copyshop)$\"];"
                + ");out center tags;";
    }

    private static List<Punto> puntos(List<Elemento> elementos, LocalDate fecha) {
        List<Punto> puntos = new ArrayList<>();
        for (Elemento elemento : Objects.requireNonNullElse(elementos, List.<Elemento>of())) {
            Double latitud = elemento.latitud() != null ? elemento.latitud()
                    : elemento.centro() == null ? null : elemento.centro().latitud();
            Double longitud = elemento.longitud() != null ? elemento.longitud()
                    : elemento.centro() == null ? null : elemento.centro().longitud();
            if (latitud == null || longitud == null) {
                continue;
            }
            Map<String, String> etiquetas = Objects.requireNonNullElse(elemento.etiquetas(), Map.of());
            String nombre = etiquetas.getOrDefault("name", "Lugar sin nombre");
            for (String[] clasificacion : clasificaciones(etiquetas)) {
                puntos.add(new Punto(elemento.tipo() + ":" + elemento.id(), clasificacion[0], clasificacion[1],
                        nombre, BigDecimal.valueOf(latitud), BigDecimal.valueOf(longitud), OPEN_STREET_MAP, fecha));
            }
        }
        return List.copyOf(puntos);
    }

    // Un mismo lugar puede contar en varias categorías (un supermercado es comercio que atrae gente y también almacén).
    private static List<String[]> clasificaciones(Map<String, String> etiquetas) {
        String amenity = etiquetas.getOrDefault("amenity", "");
        String shop = etiquetas.getOrDefault("shop", "");
        String leisure = etiquetas.getOrDefault("leisure", "");
        List<String[]> resultado = new ArrayList<>();
        if (ESCUELAS.contains(amenity)) {
            resultado.add(new String[]{"ESCUELA", null});
        }
        if (etiquetas.containsKey("office")) {
            resultado.add(new String[]{"OFICINA", null});
        }
        if (etiquetas.containsKey("public_transport") || "bus_stop".equals(etiquetas.get("highway"))
                || VIAS_FERREAS.contains(etiquetas.getOrDefault("railway", ""))) {
            resultado.add(new String[]{"TRANSPORTE", null});
        }
        if (PARQUES.contains(leisure)) {
            resultado.add(new String[]{"PARQUE", null});
        }
        if (SALUD.contains(amenity)) {
            resultado.add(new String[]{"SALUD", null});
        }
        if (COMPLEMENTARIOS_AMENITY.contains(amenity) || "supermarket".equals(shop)) {
            resultado.add(new String[]{"COMPLEMENTARIO", null});
        }
        if (ALMACENES.contains(shop)) {
            resultado.add(new String[]{"COMPETENCIA", "ALMACEN"});
        }
        if (KIOSCOS.contains(shop)) {
            resultado.add(new String[]{"COMPETENCIA", "KIOSCO"});
        }
        if (PAPELERIAS.contains(shop)) {
            resultado.add(new String[]{"COMPETENCIA", "PAPELERIA"});
        }
        return resultado;
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
    }

    private record Centro(@JsonProperty("lat") Double latitud, @JsonProperty("lon") Double longitud) {
    }
}

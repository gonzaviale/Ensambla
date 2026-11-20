package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Fuente;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Medicion;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest.Limites;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest.PuntoElegido;
import org.junit.jupiter.params.provider.Arguments;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/** Datos de prueba de Dónde abrir: zonas, mediciones, negocios, entornos medidos y pedidos. */
public final class DondeAbrirDePrueba {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    public static final ZonaId ZONA_A = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    public static final ZonaId ZONA_B = ZonaId.de("c0000000-0000-4000-8000-000000000002");
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);
    public static final Fuente OSM = new Fuente(
            "OSM", "OpenStreetMap", "COMUNITARIA", "https://www.openstreetmap.org/", "ODbL 1.0");
    private static final Fuente ENSAMBLA = new Fuente("ENSAMBLA", "Ensambla", "ENSAMBLA", null, null);
    private static final LocalDate HOY = LocalDate.parse("2026-11-10");

    private DondeAbrirDePrueba() {
    }

    // --- negocios ---
    public static Negocio unNegocioConUbicacion() {
        return Negocio.builder().nombre("Almacén actual").rubro(RubroNegocio.ALMACEN).direccion("Arieta 1")
                .latitud(new BigDecimal("-34.6")).longitud(new BigDecimal("-58.4"))
                .zonaId(ZonaId.de(UUID.fromString("c0000000-0000-4000-8000-000000000099"))).build();
    }

    public static Negocio unNegocioSinUbicacion() {
        return Negocio.builder().nombre("Sin ubicación").rubro(RubroNegocio.ALMACEN).build();
    }

    public static Negocio unNegocioConCoordenadasIncompletas() {
        return Negocio.builder().nombre("Coordenadas incompletas").rubro(RubroNegocio.ALMACEN)
                .latitud(new BigDecimal("-34.6")).build();
    }

    // --- mediciones ---
    public static Medicion unaMedicionDe(int valor) {
        return new Medicion(BigDecimal.valueOf(valor), "lugares_500m", OSM, HOY);
    }

    public static Medicion unaMedicionSinFecha() {
        return new Medicion(BigDecimal.ONE, "lugares_500m", OSM, null);
    }

    public static Medicion unaMedicionVieja() {
        return new Medicion(BigDecimal.TEN, "indice", OSM, LocalDate.parse("2026-01-01"));
    }

    public static Map<FactorDondeAbrir, Medicion> medicionesDeTodosLosFactoresEn(int valor) {
        Map<FactorDondeAbrir, Medicion> mediciones = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            mediciones.put(factor, new Medicion(BigDecimal.valueOf(valor), "unidad", fuenteDe(factor), HOY));
        }
        mediciones.remove(FactorDondeAbrir.ALQUILER);
        return mediciones;
    }

    private static Fuente fuenteDe(FactorDondeAbrir factor) {
        return Map.of(FactorDondeAbrir.DEMANDA_ENSAMBLA, ENSAMBLA).getOrDefault(factor, OSM);
    }

    public static Map<FactorDondeAbrir, Medicion> medicionesQueExcedenTodosLosLimites() {
        Map<FactorDondeAbrir, Medicion> mediciones = new EnumMap<>(FactorDondeAbrir.class);
        mediciones.put(FactorDondeAbrir.COMPETENCIA, unaMedicionDe(10));
        mediciones.put(FactorDondeAbrir.ESCUELAS, unaMedicionDe(0));
        mediciones.put(FactorDondeAbrir.PARQUES, unaMedicionDe(0));
        mediciones.put(FactorDondeAbrir.TRANSPORTE, unaMedicionDe(0));
        return mediciones;
    }

    // --- zonas ---
    public static DatosZonaDondeAbrir unaZona(ZonaId id, String nombre, Map<FactorDondeAbrir, Medicion> metricas) {
        return unaZonaEn(id, nombre, "-34.65", "-58.55", metricas);
    }

    public static DatosZonaDondeAbrir unaZonaEn(ZonaId id, String nombre, String latitud, String longitud,
                                                Map<FactorDondeAbrir, Medicion> metricas) {
        return new DatosZonaDondeAbrir(id, nombre, "Localidad", "La Matanza", "Buenos Aires",
                new BigDecimal(latitud), new BigDecimal(longitud), metricas, List.of());
    }

    public static DatosZonaDondeAbrir unaZonaPegadaAlNegocioConUnaFuenteVieja() {
        return new DatosZonaDondeAbrir(ZONA_A, "Cercana", "CABA", "CABA", "CABA",
                new BigDecimal("-34.6001"), new BigDecimal("-58.4001"),
                Map.of(FactorDondeAbrir.ACTIVIDAD_ACCESO, unaMedicionVieja()), List.of(unaEscuelaCercana()));
    }

    private static DatosZonaDondeAbrir.Punto unaEscuelaCercana() {
        return new DatosZonaDondeAbrir.Punto("node:1", "ESCUELA", null, "Escuela", new BigDecimal("-34.6"),
                new BigDecimal("-58.4"), OSM, LocalDate.parse("2026-01-01"));
    }

    // --- entorno medido en vivo ---
    public static List<DatosZonaDondeAbrir.Punto> unEntornoDelPunto() {
        return List.of(
                unLugarDe("ESCUELA", null), unLugarDe("ESCUELA", null), unLugarDe("OFICINA", null),
                unLugarDe("TRANSPORTE", null), unLugarDe("PARQUE", null), unLugarDe("SALUD", null),
                unLugarDe("COMPLEMENTARIO", null), unLugarDe("COMPETENCIA", "ALMACEN"),
                unLugarDe("COMPETENCIA", "KIOSCO"));
    }

    private static DatosZonaDondeAbrir.Punto unLugarDe(String tipo, String rubro) {
        return new DatosZonaDondeAbrir.Punto("node:" + tipo + rubro, tipo, rubro, "Lugar",
                new BigDecimal("-34.651"), new BigDecimal("-58.551"), OSM, HOY);
    }

    // --- importancias ---
    public static Map<FactorDondeAbrir, Integer> importanciaSoloPara(FactorDondeAbrir elegido) {
        Map<FactorDondeAbrir, Integer> importancia = importanciaNinguna();
        importancia.put(elegido, 2);
        return importancia;
    }

    public static Map<FactorDondeAbrir, Integer> importanciaNinguna() {
        Map<FactorDondeAbrir, Integer> importancia = new EnumMap<>(FactorDondeAbrir.class);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            importancia.put(factor, 0);
        }
        return importancia;
    }

    // --- pedidos ---
    public static ExplorarDondeAbrirRequest unPedido(RubroNegocio rubro, PublicoDondeAbrir publico,
                                                     Map<FactorDondeAbrir, Integer> importancia, Limites limites,
                                                     Map<ZonaId, BigDecimal> alquileres) {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, rubro, publico, importancia, limites, alquileres);
    }

    public static ExplorarDondeAbrirRequest unPedidoConMapasNulos() {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                null, null, null);
    }

    public static ExplorarDondeAbrirRequest unPedidoConPunto(PublicoDondeAbrir publico,
                                                             Map<FactorDondeAbrir, Integer> importancia,
                                                             String latitud, String longitud) {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN, publico, importancia, null,
                Map.of(), new PuntoElegido(new BigDecimal(latitud), new BigDecimal(longitud)));
    }

    // --- pedidos inválidos: nombre del caso y pedido ---
    public static Stream<Arguments> pedidosInvalidos() {
        return Stream.of(
                Arguments.of("pedido nulo", null),
                Arguments.of("usuario nulo", unPedidoCon(null, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS)),
                Arguments.of("rubro nulo", unPedidoCon(USUARIO_ID, null, PublicoDondeAbrir.VECINOS)),
                Arguments.of("público nulo", unPedidoCon(USUARIO_ID, RubroNegocio.ALMACEN, null)),
                Arguments.of("rubro no disponible",
                        unPedidoCon(USUARIO_ID, RubroNegocio.OTRO, PublicoDondeAbrir.VECINOS)),
                Arguments.of("importancia menor", conImportancia(Map.of(FactorDondeAbrir.ESCUELAS, -1))),
                Arguments.of("importancia mayor", conImportancia(Map.of(FactorDondeAbrir.ESCUELAS, 3))),
                Arguments.of("importancia nula", conImportancia(importanciaConValorNulo())),
                Arguments.of("zona de alquiler nula", conAlquileres(alquilerConZonaNula())),
                Arguments.of("alquiler nulo", conAlquileres(alquilerConValorNulo())),
                Arguments.of("alquiler cero", conAlquileres(Map.of(ZONA_A, BigDecimal.ZERO))),
                Arguments.of("distancia negativa", conLimites(new BigDecimal("-1"), 0, 0, 0, 0, BigDecimal.ZERO)),
                Arguments.of("competencia negativa", conLimites(BigDecimal.ZERO, -1, 0, 0, 0, BigDecimal.ZERO)),
                Arguments.of("escuelas negativas", conLimites(BigDecimal.ZERO, 0, -1, 0, 0, BigDecimal.ZERO)),
                Arguments.of("parques negativos", conLimites(BigDecimal.ZERO, 0, 0, -1, 0, BigDecimal.ZERO)),
                Arguments.of("transporte negativo", conLimites(BigDecimal.ZERO, 0, 0, 0, -1, BigDecimal.ZERO)),
                Arguments.of("alquiler máximo negativo",
                        conLimites(BigDecimal.ZERO, 0, 0, 0, 0, new BigDecimal("-1"))),
                Arguments.of("punto sin latitud", conPunto(null, BigDecimal.ONE)),
                Arguments.of("punto sin longitud", conPunto(BigDecimal.ONE, null)),
                Arguments.of("latitud fuera de rango", conPunto(new BigDecimal("91"), BigDecimal.ONE)),
                Arguments.of("longitud fuera de rango", conPunto(BigDecimal.ONE, new BigDecimal("-181"))));
    }

    private static ExplorarDondeAbrirRequest unPedidoCon(UsuarioId usuarioId, RubroNegocio rubro,
                                                         PublicoDondeAbrir publico) {
        return new ExplorarDondeAbrirRequest(usuarioId, rubro, publico, Map.of(), null, Map.of());
    }

    private static ExplorarDondeAbrirRequest conImportancia(Map<FactorDondeAbrir, Integer> importancia) {
        return unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, importancia, null, Map.of());
    }

    private static ExplorarDondeAbrirRequest conAlquileres(Map<ZonaId, BigDecimal> alquileres) {
        return unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, Map.of(), null, alquileres);
    }

    private static ExplorarDondeAbrirRequest conLimites(BigDecimal distancia, Integer competencia, Integer escuelas,
                                                        Integer parques, Integer transporte, BigDecimal alquiler) {
        Limites limites = new Limites(distancia, competencia, escuelas, parques, transporte, alquiler);
        return unPedido(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, Map.of(), limites, Map.of());
    }

    private static ExplorarDondeAbrirRequest conPunto(BigDecimal latitud, BigDecimal longitud) {
        return new ExplorarDondeAbrirRequest(USUARIO_ID, RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS,
                Map.of(), null, Map.of(), new PuntoElegido(latitud, longitud));
    }

    private static Map<FactorDondeAbrir, Integer> importanciaConValorNulo() {
        Map<FactorDondeAbrir, Integer> mapa = new EnumMap<>(FactorDondeAbrir.class);
        mapa.put(FactorDondeAbrir.ESCUELAS, null);
        return mapa;
    }

    private static Map<ZonaId, BigDecimal> alquilerConZonaNula() {
        Map<ZonaId, BigDecimal> mapa = new HashMap<>();
        mapa.put(null, BigDecimal.ONE);
        return mapa;
    }

    private static Map<ZonaId, BigDecimal> alquilerConValorNulo() {
        Map<ZonaId, BigDecimal> mapa = new HashMap<>();
        mapa.put(ZONA_A, null);
        return mapa;
    }
}

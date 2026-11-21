package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Medicion;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.EntornoDelPuntoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.DatosDondeAbrirRepository;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest.Limites;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest.PuntoElegido;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse.Aporte;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse.NegocioActual;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse.ZonaDescartada;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse.ZonaEvaluada;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class ExplorarDondeAbrir {

    private static final double RADIO_TERRESTRE_KM = 6_371d;
    private static final double DISTANCIA_CANIBALIZACION_KM = 1d;
    private static final double PENALIZACION_CANIBALIZACION = 1.2d;
    private static final double COBERTURA_MAXIMA_KM = 8d;
    private static final int DECIMALES_DEL_PUNTO = 5;
    private static final DatosZonaDondeAbrir.Fuente OPEN_STREET_MAP = new DatosZonaDondeAbrir.Fuente(
            "OSM", "OpenStreetMap", "COMUNITARIA", "https://www.openstreetmap.org/", "ODbL 1.0");
    private static final double UMBRAL_BUENA = 6d;
    private static final double UMBRAL_INTERMEDIA = 4d;
    private static final Map<RubroNegocio, String> RUBROS_EN_PLURAL = Map.of(
            RubroNegocio.ALMACEN, "almacenes",
            RubroNegocio.PAPELERIA, "papelerías",
            RubroNegocio.KIOSCO, "kioscos");
    private static final Map<FactorDondeAbrir, String> LUGARES_MEDIDOS = Map.of(
            FactorDondeAbrir.ESCUELAS, "ESCUELA",
            FactorDondeAbrir.OFICINAS, "OFICINA",
            FactorDondeAbrir.TRANSPORTE, "TRANSPORTE",
            FactorDondeAbrir.PARQUES, "PARQUE",
            FactorDondeAbrir.SALUD, "SALUD",
            FactorDondeAbrir.COMPLEMENTARIOS, "COMPLEMENTARIO");
    private static final String AVISO_PUNTO_ELEGIDO = "El lugar que elegiste se midió ahora con OpenStreetMap: "
            + "competencia, escuelas, oficinas, paradas, parques, salud y comercios a 500 m. No tiene índice de "
            + "actividad, vecinos, poder de compra ni demanda de Ensambla, así que esos factores no suman a su puntaje";

    private final NegocioRepository negocioRepository;
    private final DatosDondeAbrirRepository datosRepository;
    private final EntornoDelPuntoPort entornoPort;
    private final Clock clock;

    public ExplorarDondeAbrirResponse execute(ExplorarDondeAbrirRequest request) {
        validar(request);
        Negocio negocio = negocioDelUsuario(request);
        List<DatosZonaDondeAbrir> zonas = new ArrayList<>(zonasDelRubro(request));
        Optional<DatosZonaDondeAbrir> puntoElegido = puntoElegido(request, zonas);
        puntoElegido.ifPresent(zonas::add);
        Evaluacion evaluacion = evaluar(zonas, negocio, request);
        List<String> avisos = avisosDeLaConsulta(zonas, negocio, request, puntoElegido);
        return respuesta(negocio, zonas, evaluacion, avisos, puntoElegido);
    }

    private Negocio negocioDelUsuario(ExplorarDondeAbrirRequest request) {
        return negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
    }

    private List<DatosZonaDondeAbrir> zonasDelRubro(ExplorarDondeAbrirRequest request) {
        return datosRepository.buscar(request.rubro());
    }

    private List<DatosZonaDondeAbrir.Punto> lugaresAlrededor(BigDecimal latitud, BigDecimal longitud) {
        return entornoPort.consultar(latitud, longitud, clock.instant());
    }

    private static ExplorarDondeAbrirResponse respuesta(Negocio negocio, List<DatosZonaDondeAbrir> zonas,
                                                        Evaluacion evaluacion, List<String> avisos,
                                                        Optional<DatosZonaDondeAbrir> puntoElegido) {
        return new ExplorarDondeAbrirResponse(
                negocioActual(negocio), zonas.size(), evaluacion.ranking(), evaluacion.descartadas(),
                List.copyOf(avisos), puntoElegido.map(DatosZonaDondeAbrir::id).orElse(null));
    }

    private Evaluacion evaluar(List<DatosZonaDondeAbrir> zonas, Negocio negocio, ExplorarDondeAbrirRequest request) {
        Map<FactorDondeAbrir, Rango> rangos = rangos(zonas, request.alquileres());
        List<ZonaEvaluada> ranking = new ArrayList<>();
        List<ZonaDescartada> descartadas = new ArrayList<>();
        for (DatosZonaDondeAbrir zona : zonas) {
            BigDecimal distancia = distancia(negocio, zona);
            List<String> motivos = motivosDeDescarte(zona, distancia, request);
            if (motivos.isEmpty()) {
                ranking.add(puntuar(zona, distancia, request, rangos));
            } else {
                descartadas.add(descartada(zona, distancia, motivos));
            }
        }
        return new Evaluacion(ordenarRanking(ranking), ordenarDescartadas(descartadas));
    }

    private static ZonaDescartada descartada(DatosZonaDondeAbrir zona, BigDecimal distancia, List<String> motivos) {
        return new ZonaDescartada(zona.id(), zona.nombre(), zona.localidad(), jurisdiccion(zona), zona.latitud(),
                zona.longitud(), distancia, motivos);
    }

    private static List<ZonaEvaluada> ordenarRanking(List<ZonaEvaluada> ranking) {
        return ranking.stream()
                .sorted(Comparator.comparing(ZonaEvaluada::puntaje).reversed()
                        .thenComparing(zona -> distanciaOrdenable(zona.distanciaKm())))
                .toList();
    }

    private static List<ZonaDescartada> ordenarDescartadas(List<ZonaDescartada> descartadas) {
        return descartadas.stream()
                .sorted(Comparator.comparing(zona -> distanciaOrdenable(zona.distanciaKm())))
                .toList();
    }

    private static BigDecimal distanciaOrdenable(BigDecimal distancia) {
        return Optional.ofNullable(distancia).orElse(BigDecimal.valueOf(Double.MAX_VALUE));
    }

    private List<String> avisosDeLaConsulta(List<DatosZonaDondeAbrir> zonas, Negocio negocio,
                                            ExplorarDondeAbrirRequest request,
                                            Optional<DatosZonaDondeAbrir> puntoElegido) {
        List<String> avisos = new ArrayList<>(avisos(zonas, negocio, request));
        if (puntoElegido.isPresent()) {
            avisos.addFirst(AVISO_PUNTO_ELEGIDO);
        }
        return avisos;
    }

    /** Mide ahora el lugar que la persona tocó en el mapa y lo deja listo para competir con las zonas precargadas. */
    private Optional<DatosZonaDondeAbrir> puntoElegido(ExplorarDondeAbrirRequest request,
                                                        List<DatosZonaDondeAbrir> zonas) {
        return Optional.ofNullable(request.punto())
                .map(punto -> medirPunto(punto, request.rubro(), zonas));
    }

    private DatosZonaDondeAbrir medirPunto(PuntoElegido punto, RubroNegocio rubro, List<DatosZonaDondeAbrir> zonas) {
        BigDecimal latitud = punto.latitud().setScale(DECIMALES_DEL_PUNTO, RoundingMode.HALF_UP);
        BigDecimal longitud = punto.longitud().setScale(DECIMALES_DEL_PUNTO, RoundingMode.HALF_UP);
        DatosZonaDondeAbrir cercana = zonaMasCercana(latitud, longitud, zonas);
        List<DatosZonaDondeAbrir.Punto> lugares = lugaresAlrededor(latitud, longitud);
        String clave = "punto:" + latitud.toPlainString() + "," + longitud.toPlainString();
        return new DatosZonaDondeAbrir(ZonaId.deClave(clave),
                "Punto elegido · cerca de " + cercana.nombre(), cercana.localidad(), cercana.partido(),
                cercana.provincia(), latitud, longitud, medicionesDelEntorno(lugares, rubro),
                lugaresVisibles(lugares, rubro));
    }

    private static DatosZonaDondeAbrir zonaMasCercana(BigDecimal latitud, BigDecimal longitud,
                                                       List<DatosZonaDondeAbrir> zonas) {
        return zonas.stream()
                .filter(zona -> kilometros(latitud, longitud, zona) <= COBERTURA_MAXIMA_KM)
                .min(Comparator.comparingDouble(zona -> kilometros(latitud, longitud, zona)))
                .orElseThrow(() -> new BusinessRuleException(
                        "Por ahora medimos puntos de CABA y La Matanza: elegí un lugar dentro de esas zonas"));
    }

    private Map<FactorDondeAbrir, Medicion> medicionesDelEntorno(List<DatosZonaDondeAbrir.Punto> lugares,
                                                                  RubroNegocio rubro) {
        LocalDate hoy = LocalDate.now(clock);
        Map<FactorDondeAbrir, Medicion> mediciones = new EnumMap<>(FactorDondeAbrir.class);
        medir(mediciones, FactorDondeAbrir.COMPETENCIA, "comercios_500m",
                cantidad(lugares, "COMPETENCIA", rubro.name()), hoy);
        LUGARES_MEDIDOS.forEach((factor, tipo) ->
                medir(mediciones, factor, "lugares_500m", cantidad(lugares, tipo, null), hoy));
        return mediciones;
    }

    private static List<DatosZonaDondeAbrir.Punto> lugaresVisibles(List<DatosZonaDondeAbrir.Punto> lugares,
                                                                    RubroNegocio rubro) {
        return lugares.stream()
                .filter(lugar -> !"COMPETENCIA".equals(lugar.tipo()) || rubro.name().equals(lugar.rubro()))
                .toList();
    }

    private static long cantidad(List<DatosZonaDondeAbrir.Punto> lugares, String tipo, String rubro) {
        return lugares.stream()
                .filter(lugar -> tipo.equals(lugar.tipo()) && (rubro == null || rubro.equals(lugar.rubro())))
                .count();
    }

    private static void medir(Map<FactorDondeAbrir, Medicion> metricas, FactorDondeAbrir factor, String unidad,
                              long cantidad, LocalDate hoy) {
        metricas.put(factor, new Medicion(BigDecimal.valueOf(cantidad), unidad, OPEN_STREET_MAP, hoy));
    }

    private static void validar(ExplorarDondeAbrirRequest request) {
        validarObligatorios(request);
        validarRubro(request.rubro());
        validarImportancias(mapa(request.importancia()));
        validarAlquileres(mapa(request.alquileres()));
        validarPunto(request.punto());
        validarLimites(request.limites());
    }

    private static void validarObligatorios(ExplorarDondeAbrirRequest request) {
        if (request == null || request.usuarioId() == null || request.rubro() == null || request.publico() == null) {
            throw new BusinessRuleException("Rubro, público y usuario son obligatorios");
        }
    }

    private static void validarRubro(RubroNegocio rubro) {
        if (!RUBROS_EN_PLURAL.containsKey(rubro)) {
            throw new BusinessRuleException("Dónde abrir está disponible para almacén, papelería o kiosco");
        }
    }

    private static void validarImportancias(Map<FactorDondeAbrir, Integer> importancias) {
        if (importancias.values().stream().anyMatch(valor -> valor == null || valor < 0 || valor > 2)) {
            throw new BusinessRuleException("La importancia de cada factor tiene que ser 0, 1 o 2");
        }
    }

    private static void validarAlquileres(Map<ZonaId, BigDecimal> alquileres) {
        alquileres.forEach((zonaId, valor) -> {
            if (zonaId == null || valor == null || valor.signum() <= 0) {
                throw new BusinessRuleException("Cada alquiler informado tiene que ser mayor que cero");
            }
        });
    }

    private static void validarPunto(PuntoElegido punto) {
        if (punto != null && (punto.latitud() == null || punto.longitud() == null
                || punto.latitud().abs().compareTo(BigDecimal.valueOf(90)) > 0
                || punto.longitud().abs().compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new BusinessRuleException("El punto elegido tiene que tener latitud y longitud válidas");
        }
    }

    private static void validarLimites(Limites limites) {
        if (limites != null && (negativo(limites.distanciaMaxKm()) || negativo(limites.competenciaMax())
                || negativo(limites.escuelasMin()) || negativo(limites.parquesMin())
                || negativo(limites.transporteMin()) || negativo(limites.alquilerMax()))) {
            throw new BusinessRuleException("Los límites no pueden ser negativos");
        }
    }

    private static boolean negativo(BigDecimal valor) {
        return valor != null && valor.signum() < 0;
    }

    private static boolean negativo(Integer valor) {
        return valor != null && valor < 0;
    }

    private ZonaEvaluada puntuar(DatosZonaDondeAbrir zona, BigDecimal distancia, ExplorarDondeAbrirRequest request,
                                 Map<FactorDondeAbrir, Rango> rangos) {
        List<Aporte> aportes = aportes(zona, request, rangos);
        double puntaje = puntajeFinal(aportes, distancia);
        return new ZonaEvaluada(
                zona.id(), zona.nombre(), zona.localidad(), jurisdiccion(zona), zona.latitud(), zona.longitud(),
                distancia, decimal(puntaje, 1), coincidencia(puntaje), aportes,
                porQue(aportes, request.rubro()), riesgos(aportes, distancia, request.rubro()), zona.puntos());
    }

    private static List<Aporte> aportes(DatosZonaDondeAbrir zona, ExplorarDondeAbrirRequest request,
                                        Map<FactorDondeAbrir, Rango> rangos) {
        Map<FactorDondeAbrir, Double> pesos = pesos(request, factoresDisponibles(zona, request.alquileres()));
        return pesos.entrySet().stream()
                .map(peso -> aporte(peso.getKey(), peso.getValue(), medicion(zona, peso.getKey(),
                        request.alquileres()), rangos))
                .sorted(Comparator.comparing(Aporte::aporte).reversed())
                .toList();
    }

    private static Aporte aporte(FactorDondeAbrir factor, double peso, Medicion medicion,
                                 Map<FactorDondeAbrir, Rango> rangos) {
        double subpuntaje = rangos.get(factor).puntuar(medicion.valor().doubleValue(), factor.menosEsMejor());
        return new Aporte(factor, factor.etiqueta(), medicion.valor(), medicion.unidad(), decimal(peso, 4),
                decimal(subpuntaje, 2), decimal(peso * subpuntaje, 3), medicion.fuente(), medicion.observadaEn());
    }

    private static double puntajeFinal(List<Aporte> aportes, BigDecimal distancia) {
        double bruto = aportes.stream().mapToDouble(aporte -> aporte.aporte().doubleValue()).sum();
        double penalizacion = canibaliza(distancia) ? PENALIZACION_CANIBALIZACION : 0d;
        return Math.max(0d, Math.min(10d, bruto - penalizacion));
    }

    private static boolean canibaliza(BigDecimal distancia) {
        return distancia != null && distancia.doubleValue() < DISTANCIA_CANIBALIZACION_KM;
    }

    private static List<String> porQue(List<Aporte> aportes, RubroNegocio rubro) {
        List<String> motivos = aportes.stream()
                .filter(aporte -> aporte.puntaje().doubleValue() >= 6d)
                .limit(3)
                .map(aporte -> fraseAFavor(aporte, rubro))
                .toList();
        return motivos.isEmpty() ? List.of("Ningún factor se destaca: es una zona pareja") : motivos;
    }

    private static List<String> riesgos(List<Aporte> aportes, BigDecimal distancia, RubroNegocio rubro) {
        List<String> riesgos = new ArrayList<>(riesgosDeLosFactores(aportes, rubro));
        if (canibaliza(distancia)) {
            riesgos.addFirst("Queda a " + distancia + " km de tu local actual: podrían quitarse clientes entre sí");
        }
        if (riesgos.isEmpty()) {
            riesgos.add("Son datos estimados: confirmalos caminando la zona antes de decidir");
        }
        return List.copyOf(riesgos);
    }

    private static List<String> riesgosDeLosFactores(List<Aporte> aportes, RubroNegocio rubro) {
        return aportes.stream()
                .filter(aporte -> aporte.puntaje().doubleValue() < 4.5d && aporte.peso().doubleValue() >= .04d)
                .sorted(Comparator.comparingDouble((Aporte aporte) ->
                        aporte.peso().doubleValue() * (10d - aporte.puntaje().doubleValue())).reversed())
                .limit(2)
                .map(aporte -> fraseEnContra(aporte, rubro))
                .toList();
    }

    private static List<String> motivosDeDescarte(DatosZonaDondeAbrir zona, BigDecimal distancia,
                                                   ExplorarDondeAbrirRequest request) {
        Limites limites = request.limites();
        if (limites == null) {
            return List.of();
        }
        List<String> motivos = new ArrayList<>();
        motivoDeDistancia(distancia, limites).ifPresent(motivos::add);
        compararMaximo(zona, FactorDondeAbrir.COMPETENCIA, limites.competenciaMax(), "comercios similares", motivos);
        compararMinimo(zona, FactorDondeAbrir.ESCUELAS, limites.escuelasMin(), "escuelas", motivos);
        compararMinimo(zona, FactorDondeAbrir.PARQUES, limites.parquesMin(), "parques", motivos);
        compararMinimo(zona, FactorDondeAbrir.TRANSPORTE, limites.transporteMin(), "paradas y estaciones", motivos);
        motivoDeAlquiler(mapa(request.alquileres()).get(zona.id()), limites).ifPresent(motivos::add);
        return motivos;
    }

    private static Optional<String> motivoDeDistancia(BigDecimal distancia, Limites limites) {
        boolean excede = limites.distanciaMaxKm() != null && distancia != null
                && distancia.compareTo(limites.distanciaMaxKm()) > 0;
        return excede
                ? Optional.of("Está a " + distancia + " km: pediste hasta " + limites.distanciaMaxKm() + " km")
                : Optional.empty();
    }

    private static Optional<String> motivoDeAlquiler(BigDecimal alquiler, Limites limites) {
        boolean excede = limites.alquilerMax() != null && alquiler != null
                && alquiler.compareTo(limites.alquilerMax()) > 0;
        return excede
                ? Optional.of("El alquiler informado supera el máximo que pediste")
                : Optional.empty();
    }

    private static void compararMaximo(DatosZonaDondeAbrir zona, FactorDondeAbrir factor, Integer maximo,
                                        String nombre, List<String> motivos) {
        if (maximo == null) {
            return;
        }
        Medicion medicion = zona.metricas().get(factor);
        if (medicion == null) {
            motivos.add(sinDatos(nombre));
            return;
        }
        if (medicion.valor().compareTo(BigDecimal.valueOf(maximo)) > 0) {
            motivos.add("Tiene " + cantidadEnTexto(medicion) + " " + nombre + ": pediste hasta " + maximo);
        }
    }

    private static void compararMinimo(DatosZonaDondeAbrir zona, FactorDondeAbrir factor, Integer minimo,
                                        String nombre, List<String> motivos) {
        if (minimo == null || minimo == 0) {
            return;
        }
        Medicion medicion = zona.metricas().get(factor);
        if (medicion == null) {
            motivos.add(sinDatos(nombre));
            return;
        }
        if (medicion.valor().compareTo(BigDecimal.valueOf(minimo)) < 0) {
            motivos.add("Tiene " + cantidadEnTexto(medicion) + " " + nombre + ": pediste al menos " + minimo);
        }
    }

    private static String sinDatos(String nombre) {
        return "No hay datos de " + nombre + " para aplicar el límite";
    }

    private static String cantidadEnTexto(Medicion medicion) {
        return medicion.valor().stripTrailingZeros().toPlainString();
    }

    private static Map<FactorDondeAbrir, Rango> rangos(List<DatosZonaDondeAbrir> zonas,
                                                        Map<ZonaId, BigDecimal> alquileres) {
        Map<FactorDondeAbrir, List<Double>> valores = valoresPorFactor(zonas, alquileres);
        Map<FactorDondeAbrir, Rango> resultado = new EnumMap<>(FactorDondeAbrir.class);
        valores.forEach((factor, lista) -> resultado.put(factor, new Rango(
                lista.stream().mapToDouble(Double::doubleValue).min().orElse(0d),
                lista.stream().mapToDouble(Double::doubleValue).max().orElse(0d))));
        return resultado;
    }

    private static Map<FactorDondeAbrir, List<Double>> valoresPorFactor(List<DatosZonaDondeAbrir> zonas,
                                                                        Map<ZonaId, BigDecimal> alquileres) {
        Map<FactorDondeAbrir, List<Double>> valores = new EnumMap<>(FactorDondeAbrir.class);
        for (DatosZonaDondeAbrir zona : zonas) {
            for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
                Optional.ofNullable(medicion(zona, factor, alquileres)).ifPresent(medicion ->
                        valores.computeIfAbsent(factor, ignorado -> new ArrayList<>())
                                .add(medicion.valor().doubleValue()));
            }
        }
        return valores;
    }

    private static Set<FactorDondeAbrir> factoresDisponibles(DatosZonaDondeAbrir zona,
                                                              Map<ZonaId, BigDecimal> alquileres) {
        Set<FactorDondeAbrir> factores = new LinkedHashSet<>(zona.metricas().keySet());
        if (mapa(alquileres).containsKey(zona.id())) {
            factores.add(FactorDondeAbrir.ALQUILER);
        }
        return factores;
    }

    private static Medicion medicion(DatosZonaDondeAbrir zona, FactorDondeAbrir factor,
                                      Map<ZonaId, BigDecimal> alquileres) {
        if (factor != FactorDondeAbrir.ALQUILER) {
            return zona.metricas().get(factor);
        }
        return Optional.ofNullable(mapa(alquileres).get(zona.id()))
                .map(ExplorarDondeAbrir::medicionDeAlquiler)
                .orElse(null);
    }

    private static Medicion medicionDeAlquiler(BigDecimal alquiler) {
        return new Medicion(alquiler, "ARS_mensuales",
                new DatosZonaDondeAbrir.Fuente("USUARIO", "Dato informado por la persona", "USUARIO", null, null),
                LocalDate.now());
    }

    private static Map<FactorDondeAbrir, Double> pesos(ExplorarDondeAbrirRequest request,
                                                        Set<FactorDondeAbrir> disponibles) {
        Map<FactorDondeAbrir, Double> pesos = PesosDeFactores.base(request.rubro());
        PesosDeFactores.ajustarPorPublico(pesos, request.publico());
        ponderarPorImportancia(pesos, mapa(request.importancia()), disponibles);
        double total = pesos.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0d) {
            return pesosIguales(disponibles);
        }
        pesos.replaceAll((factor, peso) -> peso / total);
        pesos.entrySet().removeIf(entrada -> entrada.getValue() == 0d);
        return pesos;
    }

    private static void ponderarPorImportancia(Map<FactorDondeAbrir, Double> pesos,
                                               Map<FactorDondeAbrir, Integer> importancias,
                                               Set<FactorDondeAbrir> disponibles) {
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            double multiplicador = PesosDeFactores.multiplicadorDeImportancia(importancias.getOrDefault(factor, 1));
            pesos.computeIfPresent(factor, (ignorado, peso) ->
                    disponibles.contains(factor) ? peso * multiplicador : 0d);
        }
    }

    private static Map<FactorDondeAbrir, Double> pesosIguales(Set<FactorDondeAbrir> disponibles) {
        Map<FactorDondeAbrir, Double> iguales = new EnumMap<>(FactorDondeAbrir.class);
        disponibles.forEach(factor -> iguales.put(factor, 1d / disponibles.size()));
        return iguales;
    }

    private List<String> avisos(List<DatosZonaDondeAbrir> zonas, Negocio negocio,
                                ExplorarDondeAbrirRequest request) {
        Set<String> avisos = new LinkedHashSet<>();
        if (negocio.getLatitud() == null || negocio.getLongitud() == null) {
            avisos.add("Configurá la ubicación de tu negocio para calcular distancias y evitar superponer locales");
        }
        if (hayFuentesViejas(zonas)) {
            avisos.add("Hay fuentes con más de 90 días: revisá su fecha antes de decidir");
        }
        avisos.addAll(avisosDeFactoresSinDatos(zonas, request));
        return List.copyOf(avisos);
    }

    private boolean hayFuentesViejas(List<DatosZonaDondeAbrir> zonas) {
        LocalDate hoy = LocalDate.now(clock);
        return zonas.stream().flatMap(zona -> zona.metricas().values().stream())
                .map(Medicion::observadaEn).filter(fecha -> fecha != null)
                .anyMatch(fecha -> ChronoUnit.DAYS.between(fecha, hoy) > 90);
    }

    private static List<String> avisosDeFactoresSinDatos(List<DatosZonaDondeAbrir> zonas,
                                                         ExplorarDondeAbrirRequest request) {
        return Arrays.stream(FactorDondeAbrir.values())
                .filter(factor -> factor != FactorDondeAbrir.ALQUILER)
                .filter(factor -> mapa(request.importancia()).getOrDefault(factor, 1) > 0)
                .filter(factor -> zonas.stream().noneMatch(zona -> zona.metricas().containsKey(factor)))
                .map(factor -> "Todavía no hay datos de " + factor.etiqueta().toLowerCase()
                        + "; su peso se redistribuyó entre los factores disponibles")
                .toList();
    }

    private static NegocioActual negocioActual(Negocio negocio) {
        return new NegocioActual(negocio.getNombre(), negocio.getRubro(),
                negocio.getDireccion(), negocio.getLatitud(), negocio.getLongitud(), negocio.getZonaId());
    }

    private static BigDecimal distancia(Negocio negocio, DatosZonaDondeAbrir zona) {
        if (negocio.getLatitud() == null || negocio.getLongitud() == null) {
            return null;
        }
        return decimal(kilometros(negocio.getLatitud(), negocio.getLongitud(), zona), 1);
    }

    private static double kilometros(BigDecimal latitud, BigDecimal longitud, DatosZonaDondeAbrir zona) {
        double lat1 = Math.toRadians(latitud.doubleValue());
        double lat2 = Math.toRadians(zona.latitud().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(zona.longitud().doubleValue() - longitud.doubleValue());
        double a = Math.pow(Math.sin(deltaLat / 2d), 2d)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2d), 2d);
        return 2d * RADIO_TERRESTRE_KM * Math.asin(Math.sqrt(a));
    }

    private static String coincidencia(double puntaje) {
        if (puntaje >= UMBRAL_BUENA) {
            return "BUENA";
        }
        return puntaje >= UMBRAL_INTERMEDIA ? "INTERMEDIA" : "MALA";
    }

    private static String jurisdiccion(DatosZonaDondeAbrir zona) {
        return "CABA".equals(zona.partido()) ? "CABA" : zona.partido();
    }

    private static String fraseAFavor(Aporte aporte, RubroNegocio rubro) {
        return FrasesDeFactor.aFavor(aporte.factor(), medida(aporte.valor(), aporte.unidad(), rubro));
    }

    private static String fraseEnContra(Aporte aporte, RubroNegocio rubro) {
        return FrasesDeFactor.enContra(aporte.factor(), medida(aporte.valor(), aporte.unidad(), rubro));
    }

    private static FrasesDeFactor.Medida medida(BigDecimal valor, String unidad, RubroNegocio rubro) {
        return new FrasesDeFactor.Medida(valor.stripTrailingZeros().toPlainString(), unidad, plural(rubro));
    }

    private static String plural(RubroNegocio rubro) {
        return RUBROS_EN_PLURAL.getOrDefault(rubro, "comercios");
    }

    private static BigDecimal decimal(double valor, int escala) {
        return BigDecimal.valueOf(valor).setScale(escala, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static <K, V> Map<K, V> mapa(Map<K, V> mapa) {
        return mapa == null ? Map.of() : mapa;
    }

    private record Evaluacion(List<ZonaEvaluada> ranking, List<ZonaDescartada> descartadas) {
    }

    private record Rango(double minimo, double maximo) {
        double puntuar(double valor, boolean menosEsMejor) {
            if (Double.compare(maximo, minimo) == 0) {
                return 5d;
            }
            double relativo = (valor - minimo) / (maximo - minimo);
            return 10d * (menosEsMejor ? 1d - relativo : relativo);
        }
    }
}

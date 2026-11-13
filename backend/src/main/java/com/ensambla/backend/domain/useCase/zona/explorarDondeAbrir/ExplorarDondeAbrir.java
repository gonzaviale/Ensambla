package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Medicion;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.EntornoDelPuntoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.DatosDondeAbrirRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    private final NegocioRepository negocioRepository;
    private final DatosDondeAbrirRepository datosRepository;
    private final EntornoDelPuntoPort entornoPort;
    private final Clock clock;

    public ExplorarDondeAbrirResponse execute(ExplorarDondeAbrirRequest request) {
        validar(request);
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        List<DatosZonaDondeAbrir> zonas = new ArrayList<>(datosRepository.buscar(request.rubro()));
        DatosZonaDondeAbrir puntoElegido = puntoElegido(request, zonas);
        if (puntoElegido != null) {
            zonas.add(puntoElegido);
        }
        Map<FactorDondeAbrir, Rango> rangos = rangos(zonas, request.alquileres());
        List<ExplorarDondeAbrirResponse.ZonaEvaluada> ranking = new ArrayList<>();
        List<ExplorarDondeAbrirResponse.ZonaDescartada> descartadas = new ArrayList<>();

        for (DatosZonaDondeAbrir zona : zonas) {
            BigDecimal distancia = distancia(negocio, zona);
            List<String> motivos = motivosDeDescarte(zona, distancia, request);
            if (!motivos.isEmpty()) {
                descartadas.add(new ExplorarDondeAbrirResponse.ZonaDescartada(
                        zona.id(), zona.nombre(), zona.localidad(), jurisdiccion(zona), zona.latitud(),
                        zona.longitud(), distancia, motivos));
            } else {
                ranking.add(puntuar(zona, distancia, request, rangos));
            }
        }

        ranking.sort(Comparator.comparing(ExplorarDondeAbrirResponse.ZonaEvaluada::puntaje).reversed()
                .thenComparing(zona -> zona.distanciaKm() == null ? BigDecimal.valueOf(Double.MAX_VALUE)
                        : zona.distanciaKm()));
        descartadas.sort(Comparator.comparing(zona -> zona.distanciaKm() == null
                ? BigDecimal.valueOf(Double.MAX_VALUE) : zona.distanciaKm()));
        List<String> avisos = new ArrayList<>(avisos(zonas, negocio, request));
        if (puntoElegido != null) {
            avisos.addFirst("El lugar que elegiste se midió ahora con OpenStreetMap: competencia, escuelas, oficinas, "
                    + "paradas, parques, salud y comercios a 500 m. No tiene índice de actividad, vecinos, poder de "
                    + "compra ni demanda de Ensambla, así que esos factores no suman a su puntaje");
        }
        return new ExplorarDondeAbrirResponse(
                negocioActual(negocio), zonas.size(), List.copyOf(ranking), List.copyOf(descartadas),
                List.copyOf(avisos), puntoElegido == null ? null : puntoElegido.id());
    }

    /** Mide ahora el lugar que la persona tocó en el mapa y lo deja listo para competir con las zonas precargadas. */
    private DatosZonaDondeAbrir puntoElegido(ExplorarDondeAbrirRequest request, List<DatosZonaDondeAbrir> zonas) {
        ExplorarDondeAbrirRequest.PuntoElegido punto = request.punto();
        if (punto == null) {
            return null;
        }
        BigDecimal latitud = punto.latitud().setScale(DECIMALES_DEL_PUNTO, RoundingMode.HALF_UP);
        BigDecimal longitud = punto.longitud().setScale(DECIMALES_DEL_PUNTO, RoundingMode.HALF_UP);
        DatosZonaDondeAbrir cercana = zonas.stream()
                .filter(zona -> kilometros(latitud, longitud, zona) <= COBERTURA_MAXIMA_KM)
                .min(Comparator.comparingDouble(zona -> kilometros(latitud, longitud, zona)))
                .orElseThrow(() -> new BusinessRuleException(
                        "Por ahora medimos puntos de CABA y La Matanza: elegí un lugar dentro de esas zonas"));
        List<DatosZonaDondeAbrir.Punto> lugares = entornoPort.consultar(latitud, longitud, clock.instant());
        LocalDate hoy = LocalDate.now(clock);
        Map<FactorDondeAbrir, Medicion> metricas = new EnumMap<>(FactorDondeAbrir.class);
        medir(metricas, FactorDondeAbrir.COMPETENCIA, "comercios_500m",
                cantidad(lugares, "COMPETENCIA", request.rubro().name()), hoy);
        medir(metricas, FactorDondeAbrir.ESCUELAS, "lugares_500m", cantidad(lugares, "ESCUELA", null), hoy);
        medir(metricas, FactorDondeAbrir.OFICINAS, "lugares_500m", cantidad(lugares, "OFICINA", null), hoy);
        medir(metricas, FactorDondeAbrir.TRANSPORTE, "lugares_500m", cantidad(lugares, "TRANSPORTE", null), hoy);
        medir(metricas, FactorDondeAbrir.PARQUES, "lugares_500m", cantidad(lugares, "PARQUE", null), hoy);
        medir(metricas, FactorDondeAbrir.SALUD, "lugares_500m", cantidad(lugares, "SALUD", null), hoy);
        medir(metricas, FactorDondeAbrir.COMPLEMENTARIOS, "lugares_500m",
                cantidad(lugares, "COMPLEMENTARIO", null), hoy);
        List<DatosZonaDondeAbrir.Punto> visibles = lugares.stream()
                .filter(lugar -> !"COMPETENCIA".equals(lugar.tipo()) || request.rubro().name().equals(lugar.rubro()))
                .toList();
        String clave = "punto:" + latitud.toPlainString() + "," + longitud.toPlainString();
        return new DatosZonaDondeAbrir(ZonaId.deClave(clave),
                "Punto elegido · cerca de " + cercana.nombre(), cercana.localidad(), cercana.partido(),
                cercana.provincia(), latitud, longitud, metricas, visibles);
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
        if (request == null || request.usuarioId() == null || request.rubro() == null || request.publico() == null) {
            throw new BusinessRuleException("Rubro, público y usuario son obligatorios");
        }
        if (!List.of(RubroNegocio.ALMACEN, RubroNegocio.PAPELERIA, RubroNegocio.KIOSCO)
                .contains(request.rubro())) {
            throw new BusinessRuleException("Dónde abrir está disponible para almacén, papelería o kiosco");
        }
        Map<FactorDondeAbrir, Integer> importancia = mapa(request.importancia());
        if (importancia.values().stream().anyMatch(valor -> valor == null || valor < 0 || valor > 2)) {
            throw new BusinessRuleException("La importancia de cada factor tiene que ser 0, 1 o 2");
        }
        mapa(request.alquileres()).forEach((zonaId, valor) -> {
            if (zonaId == null || valor == null || valor.signum() <= 0) {
                throw new BusinessRuleException("Cada alquiler informado tiene que ser mayor que cero");
            }
        });
        ExplorarDondeAbrirRequest.PuntoElegido punto = request.punto();
        if (punto != null && (punto.latitud() == null || punto.longitud() == null
                || punto.latitud().abs().compareTo(BigDecimal.valueOf(90)) > 0
                || punto.longitud().abs().compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new BusinessRuleException("El punto elegido tiene que tener latitud y longitud válidas");
        }
        ExplorarDondeAbrirRequest.Limites limites = request.limites();
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

    private ExplorarDondeAbrirResponse.ZonaEvaluada puntuar(
            DatosZonaDondeAbrir zona,
            BigDecimal distancia,
            ExplorarDondeAbrirRequest request,
            Map<FactorDondeAbrir, Rango> rangos) {
        Map<FactorDondeAbrir, Double> pesos = pesos(request, factoresDisponibles(zona, request.alquileres()));
        List<ExplorarDondeAbrirResponse.Aporte> aportes = new ArrayList<>();
        for (Map.Entry<FactorDondeAbrir, Double> entrada : pesos.entrySet()) {
            FactorDondeAbrir factor = entrada.getKey();
            Medicion medicion = medicion(zona, factor, request.alquileres());
            double subpuntaje = rangos.get(factor).puntuar(medicion.valor().doubleValue(), factor.menosEsMejor());
            double aporte = entrada.getValue() * subpuntaje;
            aportes.add(new ExplorarDondeAbrirResponse.Aporte(
                    factor, factor.etiqueta(), medicion.valor(), medicion.unidad(), decimal(entrada.getValue(), 4),
                    decimal(subpuntaje, 2), decimal(aporte, 3), medicion.fuente(), medicion.observadaEn()));
        }
        aportes.sort(Comparator.comparing(ExplorarDondeAbrirResponse.Aporte::aporte).reversed());
        double bruto = aportes.stream().mapToDouble(aporte -> aporte.aporte().doubleValue()).sum();
        boolean canibaliza = distancia != null && distancia.doubleValue() < DISTANCIA_CANIBALIZACION_KM;
        double puntaje = Math.max(0d, Math.min(10d, bruto - (canibaliza ? PENALIZACION_CANIBALIZACION : 0d)));
        List<String> porQue = aportes.stream()
                .filter(aporte -> aporte.puntaje().doubleValue() >= 6d)
                .limit(3)
                .map(aporte -> frasePositiva(aporte.factor(), aporte.valor(), aporte.unidad(), request.rubro()))
                .toList();
        List<String> riesgos = new ArrayList<>(aportes.stream()
                .filter(aporte -> aporte.puntaje().doubleValue() < 4.5d && aporte.peso().doubleValue() >= .04d)
                .sorted(Comparator.comparingDouble((ExplorarDondeAbrirResponse.Aporte aporte) ->
                        aporte.peso().doubleValue() * (10d - aporte.puntaje().doubleValue())).reversed())
                .limit(2)
                .map(aporte -> fraseRiesgo(aporte.factor(), aporte.valor(), request.rubro()))
                .toList());
        if (canibaliza) {
            riesgos.addFirst("Queda a " + distancia + " km de tu local actual: podrían quitarse clientes entre sí");
        }
        if (riesgos.isEmpty()) {
            riesgos.add("Son datos estimados: confirmalos caminando la zona antes de decidir");
        }
        return new ExplorarDondeAbrirResponse.ZonaEvaluada(
                zona.id(), zona.nombre(), zona.localidad(), jurisdiccion(zona), zona.latitud(), zona.longitud(),
                distancia, decimal(puntaje, 1), coincidencia(puntaje), List.copyOf(aportes),
                porQue.isEmpty() ? List.of("Ningún factor se destaca: es una zona pareja") : porQue,
                List.copyOf(riesgos), zona.puntos());
    }

    private static List<String> motivosDeDescarte(DatosZonaDondeAbrir zona, BigDecimal distancia,
                                                   ExplorarDondeAbrirRequest request) {
        ExplorarDondeAbrirRequest.Limites limites = request.limites();
        if (limites == null) {
            return List.of();
        }
        List<String> motivos = new ArrayList<>();
        if (limites.distanciaMaxKm() != null && distancia != null
                && distancia.compareTo(limites.distanciaMaxKm()) > 0) {
            motivos.add("Está a " + distancia + " km: pediste hasta " + limites.distanciaMaxKm() + " km");
        }
        compararMaximo(zona, FactorDondeAbrir.COMPETENCIA, limites.competenciaMax(), "comercios similares", motivos);
        compararMinimo(zona, FactorDondeAbrir.ESCUELAS, limites.escuelasMin(), "escuelas", motivos);
        compararMinimo(zona, FactorDondeAbrir.PARQUES, limites.parquesMin(), "parques", motivos);
        compararMinimo(zona, FactorDondeAbrir.TRANSPORTE, limites.transporteMin(), "paradas y estaciones", motivos);
        BigDecimal alquiler = mapa(request.alquileres()).get(zona.id());
        if (limites.alquilerMax() != null && alquiler != null && alquiler.compareTo(limites.alquilerMax()) > 0) {
            motivos.add("El alquiler informado supera el máximo que pediste");
        }
        return motivos;
    }

    private static void compararMaximo(DatosZonaDondeAbrir zona, FactorDondeAbrir factor, Integer maximo,
                                        String nombre, List<String> motivos) {
        if (maximo == null) {
            return;
        }
        Medicion medicion = zona.metricas().get(factor);
        if (medicion == null) {
            motivos.add("No hay datos de " + nombre + " para aplicar el límite");
        } else if (medicion.valor().compareTo(BigDecimal.valueOf(maximo)) > 0) {
            motivos.add("Tiene " + medicion.valor().stripTrailingZeros().toPlainString() + " " + nombre
                    + ": pediste hasta " + maximo);
        }
    }

    private static void compararMinimo(DatosZonaDondeAbrir zona, FactorDondeAbrir factor, Integer minimo,
                                        String nombre, List<String> motivos) {
        if (minimo == null || minimo == 0) {
            return;
        }
        Medicion medicion = zona.metricas().get(factor);
        if (medicion == null) {
            motivos.add("No hay datos de " + nombre + " para aplicar el límite");
        } else if (medicion.valor().compareTo(BigDecimal.valueOf(minimo)) < 0) {
            motivos.add("Tiene " + medicion.valor().stripTrailingZeros().toPlainString() + " " + nombre
                    + ": pediste al menos " + minimo);
        }
    }

    private static Map<FactorDondeAbrir, Rango> rangos(List<DatosZonaDondeAbrir> zonas,
                                                        Map<ZonaId, BigDecimal> alquileres) {
        Map<FactorDondeAbrir, List<Double>> valores = new EnumMap<>(FactorDondeAbrir.class);
        for (DatosZonaDondeAbrir zona : zonas) {
            for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
                Medicion medicion = medicion(zona, factor, alquileres);
                if (medicion != null) {
                    valores.computeIfAbsent(factor, ignorado -> new ArrayList<>()).add(medicion.valor().doubleValue());
                }
            }
        }
        Map<FactorDondeAbrir, Rango> resultado = new EnumMap<>(FactorDondeAbrir.class);
        valores.forEach((factor, lista) -> resultado.put(factor, new Rango(
                lista.stream().mapToDouble(Double::doubleValue).min().orElse(0d),
                lista.stream().mapToDouble(Double::doubleValue).max().orElse(0d))));
        return resultado;
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
        BigDecimal alquiler = mapa(alquileres).get(zona.id());
        return alquiler == null ? null : new Medicion(alquiler, "ARS_mensuales",
                new DatosZonaDondeAbrir.Fuente("USUARIO", "Dato informado por la persona", "USUARIO", null, null),
                LocalDate.now());
    }

    private static Map<FactorDondeAbrir, Double> pesos(ExplorarDondeAbrirRequest request,
                                                        Set<FactorDondeAbrir> disponibles) {
        Map<FactorDondeAbrir, Double> pesos = pesosBase(request.rubro());
        ajustarPorPublico(pesos, request.publico());
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            int importancia = mapa(request.importancia()).getOrDefault(factor, 1);
            double multiplicador = switch (importancia) {
                case 0 -> 0d;
                case 2 -> 2.2d;
                default -> 1d;
            };
            pesos.computeIfPresent(factor, (ignorado, peso) ->
                    disponibles.contains(factor) ? peso * multiplicador : 0d);
        }
        double total = pesos.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0d) {
            Map<FactorDondeAbrir, Double> iguales = new EnumMap<>(FactorDondeAbrir.class);
            if (disponibles.isEmpty()) {
                return iguales;
            }
            disponibles.forEach(factor -> iguales.put(factor, 1d / disponibles.size()));
            return iguales;
        }
        pesos.replaceAll((factor, peso) -> peso / total);
        pesos.entrySet().removeIf(entrada -> entrada.getValue() == 0d);
        return pesos;
    }

    private static Map<FactorDondeAbrir, Double> pesosBase(RubroNegocio rubro) {
        Map<FactorDondeAbrir, Double> pesos = new EnumMap<>(FactorDondeAbrir.class);
        if (rubro == RubroNegocio.ALMACEN) {
            poner(pesos, .20, .10, .22, .05, .03, .05, .03, .02, .07, .09, .14, .14);
        } else if (rubro == RubroNegocio.PAPELERIA) {
            poner(pesos, .20, .11, .05, .24, .13, .05, .02, .01, .07, .05, .08, .07);
        } else {
            poner(pesos, .13, .24, .03, .11, .11, .18, .03, .02, .05, .03, .12, .07);
        }
        return pesos;
    }

    private static void poner(Map<FactorDondeAbrir, Double> pesos, double competencia, double actividad,
                              double densidad, double escuelas, double oficinas, double transporte, double parques,
                              double salud, double complementarios, double perfil, double demanda, double alquiler) {
        pesos.put(FactorDondeAbrir.COMPETENCIA, competencia);
        pesos.put(FactorDondeAbrir.ACTIVIDAD_ACCESO, actividad);
        pesos.put(FactorDondeAbrir.DENSIDAD, densidad);
        pesos.put(FactorDondeAbrir.ESCUELAS, escuelas);
        pesos.put(FactorDondeAbrir.OFICINAS, oficinas);
        pesos.put(FactorDondeAbrir.TRANSPORTE, transporte);
        pesos.put(FactorDondeAbrir.PARQUES, parques);
        pesos.put(FactorDondeAbrir.SALUD, salud);
        pesos.put(FactorDondeAbrir.COMPLEMENTARIOS, complementarios);
        pesos.put(FactorDondeAbrir.PERFIL_SOCIOECONOMICO, perfil);
        pesos.put(FactorDondeAbrir.DEMANDA_ENSAMBLA, demanda);
        pesos.put(FactorDondeAbrir.ALQUILER, alquiler);
    }

    private static void ajustarPorPublico(Map<FactorDondeAbrir, Double> pesos, PublicoDondeAbrir publico) {
        if (publico == PublicoDondeAbrir.VECINOS) {
            multiplicar(pesos, FactorDondeAbrir.DENSIDAD, 1.6d);
        } else if (publico == PublicoDondeAbrir.ESTUDIANTES) {
            multiplicar(pesos, FactorDondeAbrir.ESCUELAS, 1.8d);
            multiplicar(pesos, FactorDondeAbrir.PARQUES, 1.2d);
        } else {
            multiplicar(pesos, FactorDondeAbrir.OFICINAS, 1.8d);
            multiplicar(pesos, FactorDondeAbrir.TRANSPORTE, 1.2d);
        }
    }

    private static void multiplicar(Map<FactorDondeAbrir, Double> pesos, FactorDondeAbrir factor, double valor) {
        pesos.computeIfPresent(factor, (ignorado, peso) -> peso * valor);
    }

    private List<String> avisos(List<DatosZonaDondeAbrir> zonas, Negocio negocio,
                                ExplorarDondeAbrirRequest request) {
        Set<String> avisos = new LinkedHashSet<>();
        if (negocio.getLatitud() == null || negocio.getLongitud() == null) {
            avisos.add("Configurá la ubicación de tu negocio para calcular distancias y evitar superponer locales");
        }
        LocalDate hoy = LocalDate.now(clock);
        boolean desactualizado = zonas.stream().flatMap(zona -> zona.metricas().values().stream())
                .map(Medicion::observadaEn).filter(fecha -> fecha != null)
                .anyMatch(fecha -> ChronoUnit.DAYS.between(fecha, hoy) > 90);
        if (desactualizado) {
            avisos.add("Hay fuentes con más de 90 días: revisá su fecha antes de decidir");
        }
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            if (factor != FactorDondeAbrir.ALQUILER
                    && mapa(request.importancia()).getOrDefault(factor, 1) > 0
                    && zonas.stream().noneMatch(zona -> zona.metricas().containsKey(factor))) {
                avisos.add("Todavía no hay datos de " + factor.etiqueta().toLowerCase()
                        + "; su peso se redistribuyó entre los factores disponibles");
            }
        }
        return List.copyOf(avisos);
    }

    private static ExplorarDondeAbrirResponse.NegocioActual negocioActual(Negocio negocio) {
        return new ExplorarDondeAbrirResponse.NegocioActual(negocio.getNombre(), negocio.getRubro(),
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

    private static String frasePositiva(FactorDondeAbrir factor, BigDecimal valor, String unidad,
                                        RubroNegocio rubro) {
        String cantidad = valor.stripTrailingZeros().toPlainString();
        return switch (factor) {
            case COMPETENCIA -> "Solo " + cantidad + " " + plural(rubro) + " a menos de 500 m";
            case ACTIVIDAD_ACCESO -> "Buena actividad y acceso estimados (" + cantidad + " de 100)";
            case DENSIDAD -> "Zona con muchos vecinos (" + cantidad + " personas por km²)";
            case ESCUELAS -> cantidad + " escuelas a menos de 500 m";
            case OFICINAS -> cantidad + " oficinas a menos de 500 m";
            case TRANSPORTE -> cantidad + " paradas y estaciones a menos de 500 m";
            case PARQUES -> cantidad + " parques y plazas a menos de 500 m";
            case SALUD -> cantidad + " centros de salud cerca";
            case COMPLEMENTARIOS -> cantidad + " comercios que atraen gente";
            case PERFIL_SOCIOECONOMICO -> "Buen perfil de poder de compra";
            case DEMANDA_ENSAMBLA -> "Demanda observada en comercios de Ensambla (" + cantidad + " " + unidad + ")";
            case ALQUILER -> "Cotización de alquiler competitiva entre las zonas informadas";
        };
    }

    private static String fraseRiesgo(FactorDondeAbrir factor, BigDecimal valor, RubroNegocio rubro) {
        return switch (factor) {
            case COMPETENCIA -> "Hay " + valor.stripTrailingZeros().toPlainString() + " " + plural(rubro)
                    + " a menos de 500 m: la competencia es fuerte";
            case ACTIVIDAD_ACCESO -> "La actividad y el acceso estimados son bajos";
            case DENSIDAD -> "Hay pocos vecinos viviendo cerca";
            case ESCUELAS -> "Casi no hay escuelas cerca";
            case OFICINAS -> "Casi no hay oficinas cerca";
            case TRANSPORTE -> "Hay pocas paradas y estaciones cerca";
            case PARQUES -> "Casi no hay parques ni plazas cerca";
            case SALUD -> "Hay pocos centros de salud cerca";
            case COMPLEMENTARIOS -> "Hay pocos comercios que atraigan gente";
            case PERFIL_SOCIOECONOMICO -> "El poder de compra estimado es más bajo que en otras zonas";
            case DEMANDA_ENSAMBLA -> "Ensambla observa menos demanda que en otras zonas con datos suficientes";
            case ALQUILER -> "La cotización de alquiler es alta frente a las otras que cargaste";
        };
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

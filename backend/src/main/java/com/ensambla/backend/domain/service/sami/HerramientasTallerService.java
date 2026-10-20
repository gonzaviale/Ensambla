package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoNuevo;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConexionKit;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Valida lo que la IA pidió en un turno del taller. El modelo nunca escribe el sistema: pide herramientas con
 * vocabulario cerrado ({@link CatalogoComponentes}, {@link CatalogoTaller}, {@link KitsRubro}) y acá se descarta todo
 * lo que no exista, esté repetido o se pase de los topes. Las llamadas se validan en orden sobre una copia del estado,
 * así una herramienta puede apoyarse en una pieza que se sumó un instante antes en el mismo turno.
 * <p>
 * Una pieza se busca por id, después por tipo y por último por nombre (sin importar mayúsculas, tildes ni plural).
 * En las acciones se nombra por su id o, si se
 * sumó en este turno, por su tipo (la tabla a medida, que se repite, por su nombre). {@code usar_kit} no llega nunca
 * al taller: se expande acá en {@code agregar_pieza} y {@code conectar}.
 */
@Service
public class HerramientasTallerService {

    private interface Manejador extends BiFunction<Map<String, Object>, List<PiezaTaller>, Optional<AccionTaller>> {
    }

    /** Lo que se va acumulando en un turno. */
    private static final class Turno {

        private final List<PiezaTaller> piezas;
        private final List<AccionTaller> acciones = new ArrayList<>();
        private OpcionesTaller opciones;
        /** La pregunta del kit que se armó, por si el modelo no preguntó nada. */
        private OpcionesTaller preguntaDelKit;
        private int descartadas;
        private int delModelo;

        private Turno(List<PiezaTaller> piezas) {
            this.piezas = new ArrayList<>(piezas);
        }

        private boolean admiteOtraDelModelo() {
            return delModelo < CatalogoTaller.MAX_ACCIONES && acciones.size() < CatalogoTaller.MAX_ACCIONES_TOTALES;
        }

        private int lugar() {
            return CatalogoTaller.MAX_ACCIONES_TOTALES - acciones.size();
        }

        /** Una llamada del modelo cuenta una vez aunque se haya expandido en varias acciones. */
        private void sumarDelModelo(List<AccionTaller> hechas) {
            if (hechas.isEmpty()) {
                descartadas++;
            } else {
                delModelo++;
                acciones.addAll(hechas);
            }
        }
    }

    private final CatalogoComponentes catalogoComponentes;
    private final KitsRubro kitsRubro;
    private final Map<HerramientaTaller, Manejador> manejadores = new EnumMap<>(HerramientaTaller.class);

    public HerramientasTallerService(CatalogoComponentes catalogoComponentes, KitsRubro kitsRubro) {
        this.catalogoComponentes = catalogoComponentes;
        this.kitsRubro = kitsRubro;
        manejadores.put(HerramientaTaller.AGREGAR_PIEZA, this::agregarPieza);
        manejadores.put(HerramientaTaller.QUITAR_PIEZA, this::quitarPieza);
        manejadores.put(HerramientaTaller.RENOMBRAR_PIEZA, this::renombrarPieza);
        manejadores.put(HerramientaTaller.RENOMBRAR_SISTEMA, this::renombrarSistema);
        manejadores.put(HerramientaTaller.AGREGAR_CAMPO, this::agregarCampo);
        manejadores.put(HerramientaTaller.QUITAR_CAMPO, this::quitarCampo);
        manejadores.put(HerramientaTaller.CARGAR_EJEMPLOS, this::cargarEjemplos);
        manejadores.put(HerramientaTaller.CONECTAR, this::conectar);
        manejadores.put(HerramientaTaller.SEPARAR_EN_SECCION, this::separarEnSeccion);
        manejadores.put(HerramientaTaller.CAMBIAR_DISTRIBUCION, this::cambiarDistribucion);
    }

    public ValidacionTaller validar(List<LlamadaHerramienta> llamadas, EstadoTaller estado) {
        Turno turno = new Turno(estado.piezas());
        for (LlamadaHerramienta llamada : llamadas) {
            Optional<HerramientaTaller> herramienta = HerramientaTaller.deNombre(llamada.nombre());
            if (herramienta.isEmpty()) {
                turno.descartadas++;
            } else if (herramienta.get() == HerramientaTaller.OFRECER_OPCIONES) {
                Optional<OpcionesTaller> pregunta = opciones(llamada.argumentos());
                turno.opciones = pregunta.orElse(turno.opciones);
                turno.descartadas += pregunta.isPresent() ? 0 : 1;
            } else {
                turno.sumarDelModelo(turno.admiteOtraDelModelo()
                        ? aplicar(herramienta.get(), llamada.argumentos(), turno)
                        : List.of());
            }
        }
        // Al armar con un kit Sami siempre propone cómo seguir: si el modelo no preguntó, va la pregunta del kit.
        OpcionesTaller opciones = turno.opciones != null ? turno.opciones : turno.preguntaDelKit;
        return new ValidacionTaller(List.copyOf(turno.acciones), opciones, turno.descartadas);
    }

    private List<AccionTaller> aplicar(HerramientaTaller herramienta, Map<String, Object> args, Turno turno) {
        if (herramienta == HerramientaTaller.USAR_KIT) {
            return usarKit(args, turno);
        }
        return manejadores.get(herramienta).apply(args, turno.piezas).stream().toList();
    }

    /**
     * Suma las piezas del kit que el sistema todavía no tiene (un tipo que no se repite y ya está, o una tabla a
     * medida con el mismo nombre, se saltean) y después sus conexiones, sin pasarse del lugar que queda en el turno.
     * Si sumó algo, guarda la pregunta del kit.
     */
    private List<AccionTaller> usarKit(Map<String, Object> args, Turno turno) {
        List<AccionTaller> acciones = new ArrayList<>();
        int lugar = turno.lugar();
        clave(args.get("rubro")).flatMap(kitsRubro::buscar).ifPresent(kit -> {
            kit.piezas().forEach(pieza -> sumarSiHayLugar(
                    acciones, lugar, () -> agregarPieza(pieza.comoArgumentos(), turno.piezas)));
            kit.conexiones().forEach(conexion -> sumarSiHayLugar(
                    acciones, lugar, () -> conectar(argumentosDe(kit, conexion), turno.piezas)));
            if (!acciones.isEmpty()) {
                turno.preguntaDelKit = kit.pregunta();
            }
        });
        return acciones;
    }

    private void sumarSiHayLugar(List<AccionTaller> acciones, int lugar, Supplier<Optional<AccionTaller>> accion) {
        if (acciones.size() < lugar) {
            accion.get().ifPresent(acciones::add);
        }
    }

    private Map<String, Object> argumentosDe(KitRubro kit, ConexionKit conexion) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("desde", kit.referenciaDe(conexion.desde()));
        argumentos.put("hacia", kit.referenciaDe(conexion.hacia()));
        argumentos.put("etiqueta", conexion.etiqueta());
        return argumentos;
    }

    private Optional<AccionTaller> agregarPieza(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> nombre = texto(args.get("nombre"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return clave(args.get("tipo"))
                .flatMap(catalogoComponentes::buscar)
                .filter(componente -> componente.repetible() ? nombre.isPresent() : noEsta(componente, piezas))
                .filter(componente -> !nombreUsado(nombre.orElse(componente.nombre()), piezas))
                .map(componente -> sumarPieza(componente, nombre, args, piezas));
    }

    /** El resumen y la nota no llevan datos: a esos componentes no se les suman datos extra ni ejemplos. */
    private AccionTaller sumarPieza(ComponenteCatalogo componente, Optional<String> nombre, Map<String, Object> args,
                                    List<PiezaTaller> piezas) {
        List<CampoTaller> campos = new ArrayList<>(componente.camposTaller());
        List<Map<String, Object>> extras = campos.isEmpty() ? List.of() : camposExtra(args.get("campos"), campos);
        List<Map<String, Object>> ejemplos = filas(args.get("ejemplos"), campos);
        Optional<String> seccion = texto(args.get("seccion"), CatalogoTaller.MAX_LARGO_NOMBRE);
        piezas.add(new PiezaTaller(
                null, componente.tipo(), nombre.orElse(componente.nombre()), seccion.orElse(null), campos));
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("tipo", componente.tipo());
        nombre.ifPresent(valor -> argumentos.put("nombre", valor));
        if (!extras.isEmpty()) {
            argumentos.put("campos", extras);
        }
        if (!ejemplos.isEmpty()) {
            argumentos.put("ejemplos", ejemplos);
        }
        seccion.ifPresent(valor -> argumentos.put("seccion", valor));
        return new AccionTaller(HerramientaTaller.AGREGAR_PIEZA.nombre(), argumentos);
    }

    /** Los datos extra válidos, que además quedan sumados a {@code campos}. */
    private List<Map<String, Object>> camposExtra(Object valor, List<CampoTaller> campos) {
        List<Map<String, Object>> extras = new ArrayList<>();
        for (Map<String, Object> pedido : mapas(valor)) {
            boolean hayLugar = extras.size() < CatalogoTaller.MAX_CAMPOS_EXTRA
                    && campos.size() < CatalogoTaller.MAX_CAMPOS_POR_PIEZA;
            Optional<CampoNuevo> campo = hayLugar ? campoNuevo(pedido, campos) : Optional.empty();
            campo.ifPresent(nuevo -> {
                campos.add(nuevo.comoCampo());
                extras.add(nuevo.comoArgumentos());
            });
        }
        return extras;
    }

    private Optional<CampoNuevo> campoNuevo(Map<String, Object> args, List<CampoTaller> campos) {
        Optional<String> etiqueta = texto(args.get("etiqueta"), CatalogoTaller.MAX_LARGO_NOMBRE)
                .filter(nueva -> etiquetaDe(nueva, campos).isEmpty());
        Optional<String> tipo = clave(args.get("tipo")).filter(CatalogoTaller.TIPOS_DE_CAMPO::contains);
        boolean esLista = tipo.filter(CatalogoTaller.TIPO_LISTA::equals).isPresent();
        List<String> opciones = esLista
                ? textos(args.get("opciones")).stream().limit(CatalogoTaller.MAX_OPCIONES).toList()
                : List.of();
        if (etiqueta.isEmpty() || tipo.isEmpty() || esLista && opciones.size() < CatalogoTaller.MIN_OPCIONES) {
            return Optional.empty();
        }
        return Optional.of(new CampoNuevo(etiqueta.get(), tipo.get(), opciones));
    }

    private Optional<AccionTaller> quitarPieza(Map<String, Object> args, List<PiezaTaller> piezas) {
        return indiceDe(args.get("pieza"), piezas).map(indice -> {
            String referencia = referencia(piezas.remove((int) indice));
            return accion(HerramientaTaller.QUITAR_PIEZA, "pieza", referencia);
        });
    }

    private Optional<AccionTaller> renombrarPieza(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> nombre = texto(args.get("nombre"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> nombre.map(nuevo -> {
            PiezaTaller pieza = piezas.get(indice);
            piezas.set(indice, new PiezaTaller(pieza.id(), pieza.tipo(), nuevo, pieza.seccion(), pieza.campos()));
            return accionConPieza(HerramientaTaller.RENOMBRAR_PIEZA, pieza, "nombre", nuevo);
        }));
    }

    private Optional<AccionTaller> renombrarSistema(Map<String, Object> args, List<PiezaTaller> piezas) {
        return texto(args.get("nombre"), CatalogoTaller.MAX_LARGO_NOMBRE)
                .map(nombre -> accion(HerramientaTaller.RENOMBRAR_SISTEMA, "nombre", nombre));
    }

    private Optional<AccionTaller> agregarCampo(Map<String, Object> args, List<PiezaTaller> piezas) {
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            return campoNuevo(args, pieza.campos())
                    .filter(nuevo -> hayLugar(pieza))
                    .map(nuevo -> {
                        piezas.set(indice, conCampo(pieza, nuevo.comoCampo()));
                        Map<String, Object> argumentos = new LinkedHashMap<>();
                        argumentos.put("pieza", referencia(pieza));
                        argumentos.putAll(nuevo.comoArgumentos());
                        return new AccionTaller(HerramientaTaller.AGREGAR_CAMPO.nombre(), argumentos);
                    });
        });
    }

    /** Los vínculos y los datos obligatorios del componente no se quitan (el taller tampoco lo deja). */
    private Optional<AccionTaller> quitarCampo(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> buscado = texto(args.get("campo"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            return buscado.flatMap(etiqueta -> campoDe(etiqueta, pieza.campos()))
                    .filter(campo -> sePuedeQuitar(pieza, campo))
                    .map(campo -> {
                        List<CampoTaller> campos = new ArrayList<>(pieza.campos());
                        campos.remove(campo);
                        piezas.set(indice, conCampos(pieza, campos));
                        return accionConPieza(HerramientaTaller.QUITAR_CAMPO, pieza, "campo", campo.etiqueta());
                    });
        });
    }

    private boolean sePuedeQuitar(PiezaTaller pieza, CampoTaller campo) {
        boolean obligatorio = catalogoComponentes.buscar(pieza.tipo()).stream()
                .flatMap(componente -> componente.campos().stream())
                .anyMatch(base -> base.obligatorio() && base.etiqueta().equalsIgnoreCase(campo.etiqueta()));
        return !CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) && !obligatorio;
    }

    private Optional<AccionTaller> cargarEjemplos(Map<String, Object> args, List<PiezaTaller> piezas) {
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            return Optional.of(filas(args.get("filas"), pieza.campos()))
                    .filter(filas -> !filas.isEmpty())
                    .map(filas -> {
                        Map<String, Object> argumentos = new LinkedHashMap<>();
                        argumentos.put("pieza", referencia(pieza));
                        argumentos.put("filas", filas);
                        return new AccionTaller(HerramientaTaller.CARGAR_EJEMPLOS.nombre(), argumentos);
                    });
        });
    }

    /**
     * Suma en {@code desde} un dato vínculo que apunta a {@code hacia}. Sin etiqueta usa el singular del componente
     * ({@code Cliente}) o, si {@code hacia} es una tabla a medida, su nombre. Como en el taller, si {@code desde} ya
     * tiene un dato común con esa etiqueta (que no sea el primero) ese dato pasa a ser el vínculo, y las piezas sin
     * datos (resumen, nota) no se conectan. Lo que el taller ya vincula solo ({@link CatalogoTaller#seVinculaSolo}) se
     * descarta para no duplicar el vínculo.
     */
    private Optional<AccionTaller> conectar(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<Integer> desde = indiceDe(args.get("desde"), piezas);
        Optional<Integer> hacia = indiceDe(args.get("hacia"), piezas);
        if (desde.isEmpty() || hacia.isEmpty() || desde.get().equals(hacia.get())) {
            return Optional.empty();
        }
        PiezaTaller origen = piezas.get(desde.get());
        PiezaTaller destino = piezas.get(hacia.get());
        if (origen.campos().isEmpty() || destino.campos().isEmpty()
                || CatalogoTaller.seVinculaSolo(origen.tipo(), destino.tipo())) {
            return Optional.empty();
        }
        String etiqueta = texto(args.get("etiqueta"), CatalogoTaller.MAX_LARGO_NOMBRE)
                .orElseGet(() -> etiquetaDeVinculo(destino));
        Optional<CampoTaller> existente = campoDe(etiqueta, origen.campos());
        boolean sePuede = existente.map(campo -> sePuedeVolverVinculo(origen, campo)).orElseGet(() -> hayLugar(origen));
        if (!sePuede) {
            return Optional.empty();
        }
        CampoTaller vinculo = new CampoTaller(
                existente.map(CampoTaller::etiqueta).orElse(etiqueta), CatalogoTaller.TIPO_VINCULO);
        List<CampoTaller> campos = new ArrayList<>(origen.campos());
        existente.ifPresentOrElse(campo -> campos.set(campos.indexOf(campo), vinculo), () -> campos.add(vinculo));
        piezas.set(desde.get(), conCampos(origen, campos));
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("desde", referencia(origen));
        argumentos.put("hacia", referencia(destino));
        argumentos.put("etiqueta", vinculo.etiqueta());
        return Optional.of(new AccionTaller(HerramientaTaller.CONECTAR.nombre(), argumentos));
    }

    private boolean sePuedeVolverVinculo(PiezaTaller origen, CampoTaller campo) {
        return !CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) && !campo.equals(origen.campos().getFirst());
    }

    private String etiquetaDeVinculo(PiezaTaller destino) {
        return catalogoComponentes.buscar(destino.tipo())
                .filter(componente -> !componente.repetible())
                .map(ComponenteCatalogo::etiquetaDeVinculo)
                .orElse(destino.nombre());
    }

    private Optional<AccionTaller> separarEnSeccion(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> seccion = texto(args.get("seccion"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> seccion.map(nueva -> {
            PiezaTaller pieza = piezas.get(indice);
            piezas.set(indice, new PiezaTaller(pieza.id(), pieza.tipo(), pieza.nombre(), nueva, pieza.campos()));
            return accionConPieza(HerramientaTaller.SEPARAR_EN_SECCION, pieza, "seccion", nueva);
        }));
    }

    private Optional<AccionTaller> cambiarDistribucion(Map<String, Object> args, List<PiezaTaller> piezas) {
        return clave(args.get("layout"))
                .filter(CatalogoTaller.DISTRIBUCIONES::contains)
                .map(layout -> accion(HerramientaTaller.CAMBIAR_DISTRIBUCION, "layout", layout));
    }

    private Optional<OpcionesTaller> opciones(Map<String, Object> args) {
        List<String> opciones = textos(args.get("opciones")).stream()
                .limit(CatalogoTaller.MAX_OPCIONES)
                .toList();
        return texto(args.get("pregunta"), CatalogoTaller.MAX_LARGO_PREGUNTA)
                .filter(pregunta -> opciones.size() >= CatalogoTaller.MIN_OPCIONES)
                .map(pregunta -> new OpcionesTaller(pregunta, opciones));
    }

    /** Filas de ejemplo con etiquetas de {@code campos} y valores simples; las celdas y filas inválidas se caen. */
    private List<Map<String, Object>> filas(Object valor, List<CampoTaller> campos) {
        return mapas(valor).stream()
                .map(pedida -> fila(pedida, campos))
                .filter(fila -> !fila.isEmpty())
                .limit(CatalogoTaller.MAX_FILAS_EJEMPLO)
                .toList();
    }

    private Map<String, Object> fila(Map<String, Object> pedida, List<CampoTaller> campos) {
        Map<String, Object> fila = new LinkedHashMap<>();
        pedida.forEach((etiqueta, valor) -> etiquetaDe(etiqueta, campos)
                .ifPresent(existente -> valorDeEjemplo(valor).ifPresent(dato -> fila.put(existente, dato))));
        return fila;
    }

    private Optional<Object> valorDeEjemplo(Object valor) {
        if (valor instanceof Boolean || valor instanceof Number) {
            return Optional.of(valor);
        }
        return texto(valor, CatalogoTaller.MAX_LARGO_VALOR).map(Object.class::cast);
    }

    /** Los elementos de una lista que son objetos JSON; cualquier otra cosa se ignora. */
    private List<Map<String, Object>> mapas(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        List<Map<String, Object>> mapas = new ArrayList<>();
        for (Object elemento : lista) {
            if (elemento instanceof Map<?, ?> mapa) {
                Map<String, Object> copia = new LinkedHashMap<>();
                mapa.forEach((clave, contenido) -> copia.put(String.valueOf(clave), contenido));
                mapas.add(copia);
            }
        }
        return mapas;
    }

    private Optional<Integer> indiceDe(Object referencia, List<PiezaTaller> piezas) {
        return texto(referencia, CatalogoTaller.MAX_LARGO_NOMBRE).flatMap(buscada ->
                primera(piezas, pieza -> buscada.equals(pieza.id()))
                        .or(() -> primera(piezas, pieza -> buscada.toLowerCase(Locale.ROOT).equals(pieza.tipo())))
                        .or(() -> primera(piezas, pieza -> mismoNombre(pieza.nombre(), buscada))));
    }

    private Optional<Integer> primera(List<PiezaTaller> piezas, Predicate<PiezaTaller> criterio) {
        return IntStream.range(0, piezas.size())
                .boxed()
                .filter(indice -> criterio.test(piezas.get(indice)))
                .findFirst();
    }

    /** Sin importar mayúsculas, tildes ni plural: «Quien me debe» encuentra a «Quién me debe». */
    private boolean mismoNombre(String nombre, String buscado) {
        return NormalizadorTexto.clave(Objects.requireNonNullElse(nombre, ""))
                .equals(NormalizadorTexto.clave(buscado));
    }

    private boolean nombreUsado(String nombre, List<PiezaTaller> piezas) {
        return piezas.stream().anyMatch(pieza -> mismoNombre(pieza.nombre(), nombre));
    }

    private boolean noEsta(ComponenteCatalogo componente, List<PiezaTaller> piezas) {
        return piezas.stream().noneMatch(pieza -> pieza.tipo().equals(componente.tipo()));
    }

    private boolean hayLugar(PiezaTaller pieza) {
        return pieza.campos().size() < CatalogoTaller.MAX_CAMPOS_POR_PIEZA;
    }

    /** El dato de la pieza con esa etiqueta, sin importar mayúsculas. */
    private Optional<CampoTaller> campoDe(String etiqueta, List<CampoTaller> campos) {
        return campos.stream().filter(campo -> campo.etiqueta().equalsIgnoreCase(etiqueta.strip())).findFirst();
    }

    /** La etiqueta tal como está en la pieza, si alguna coincide sin importar mayúsculas. */
    private Optional<String> etiquetaDe(String buscada, List<CampoTaller> campos) {
        return campoDe(buscada, campos).map(CampoTaller::etiqueta);
    }

    private PiezaTaller conCampo(PiezaTaller pieza, CampoTaller campo) {
        List<CampoTaller> campos = new ArrayList<>(pieza.campos());
        campos.add(campo);
        return conCampos(pieza, campos);
    }

    private PiezaTaller conCampos(PiezaTaller pieza, List<CampoTaller> campos) {
        return new PiezaTaller(pieza.id(), pieza.tipo(), pieza.nombre(), pieza.seccion(), campos);
    }

    private String referencia(PiezaTaller pieza) {
        if (pieza.id() != null) {
            return pieza.id();
        }
        return CatalogoComponentes.TIPO_A_MEDIDA.equals(pieza.tipo()) ? pieza.nombre() : pieza.tipo();
    }

    private AccionTaller accion(HerramientaTaller herramienta, String argumento, String valor) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put(argumento, valor);
        return new AccionTaller(herramienta.nombre(), argumentos);
    }

    private AccionTaller accionConPieza(HerramientaTaller herramienta, PiezaTaller pieza, String argumento,
                                        String valor) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("pieza", referencia(pieza));
        argumentos.put(argumento, valor);
        return new AccionTaller(herramienta.nombre(), argumentos);
    }

    private Optional<String> texto(Object valor, int maximo) {
        return Optional.ofNullable(valor)
                .filter(String.class::isInstance)
                .map(texto -> ((String) texto).strip())
                .filter(texto -> !texto.isEmpty() && texto.length() <= maximo);
    }

    private Optional<String> clave(Object valor) {
        return texto(valor, CatalogoTaller.MAX_LARGO_NOMBRE).map(texto -> texto.toLowerCase(Locale.ROOT));
    }

    private List<String> textos(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        return lista.stream()
                .flatMap(elemento -> texto(elemento, CatalogoTaller.MAX_LARGO_NOMBRE).stream())
                .distinct()
                .toList();
    }
}

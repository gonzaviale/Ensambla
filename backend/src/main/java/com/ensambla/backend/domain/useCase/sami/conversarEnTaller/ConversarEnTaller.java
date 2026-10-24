package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoNuevo;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConexionKit;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Un turno de la charla con Sami en el taller: se reconoce el rubro (sin IA) para mandarle al modelo sólo el kit que
 * importa, el modelo conversa y pide herramientas, el dominio valida lo que pidió y devuelve acciones para que el
 * taller las aplique. Reserva la consulta del día del negocio antes de llamar (un rechazo por límite no cuesta nada)
 * y registra los tokens gastados. El día del límite es UTC.
 * <p>
 * El rubro de la persona se reconoce sin gastar IA: se buscan los términos de cada kit en el mensaje, la charla reciente y el
 * nombre del sistema (con {@link TextoNormalizado}, así «Velas» y «vela» dan lo mismo). Un término de varias palabras
 * pesa más que uno suelto y lo que dice el mensaje pesa el doble que el resto; ante un empate gana el kit que figura
 * primero. Así a la IA sólo le llega el kit que importa.
 * <p>
 * Lo que la IA pidió en el turno se valida acá. El modelo nunca escribe el sistema: pide herramientas con
 * vocabulario cerrado ({@link CatalogoComponentes}, {@link CatalogoTaller}, {@link KitsRubro}) y acá se descarta todo
 * lo que no exista, esté repetido o se pase de los topes. Las llamadas se validan en orden sobre una copia del estado,
 * así una herramienta puede apoyarse en una pieza que se sumó un instante antes en el mismo turno.
 * <p>
 * Una pieza se busca por id, después por tipo y por último por nombre (sin importar mayúsculas, tildes ni plural).
 * En las acciones se nombra por su id o, si se
 * sumó en este turno, por su tipo (la tabla a medida, que se repite, por su nombre). {@code usar_kit} no llega nunca
 * al taller: se expande acá en {@code agregar_pieza} y {@code conectar}.
 */
public class ConversarEnTaller {

    static final String LIMITE_ALCANZADO =
            "Hoy ya usaste todas las consultas con IA. Probá mañana o seguí armándolo a mano.";
    static final String IA_NO_DISPONIBLE = "Sami no pudo pensar la respuesta ahora. Probá de nuevo en un rato.";
    static final String SIN_CAMBIOS = "No pude hacer eso con lo que tengo. ¿Me lo contás de otra forma?";
    static final String RECORDATORIO = "\n\n(Aviso del sistema: en tu respuesta anterior dijiste que hiciste cambios, pero"
            + " no invocaste ninguna herramienta. Hacelos ahora invocando las herramientas.)";
    /** El segundo pedido de un turno con foto, cuando el primero armó las piezas pero no cargó lo que se lee. */
    static final String PEDIDO_REGISTROS = "Ya armé las piezas que ves. Tu respuesta tiene que llevar DOS"
            + " invocaciones juntas: (1) cargar_registros con todas las filas que se leen en la foto, en la pieza que"
            + " corresponda (los pedidos de clientes van en Ventas o Pedidos con su Cliente, no en Compras) y con las"
            + " etiquetas de sus datos, SIN saltearte ningún renglón; además cargá en Productos (inventario) los"
            + " productos DISTINTOS que se leen, con su nombre y su precio de lista si se ve (sin costos ni cantidades de"
            + " stock que no estén en la foto); (2) ofrecer_opciones UNA sola vez, con en `preguntas` TODAS las dudas"
            + " importantes de la foto (hasta 3; por ejemplo qué significan los tildes, si un número es costo o lo que"
            + " debe, cuál de dos totales es el correcto), cada una con `tema` corto, `pregunta` y 2 a 4 respuestas"
            + " cortas. No cambies nada más.";
    /** El pedido extra de un turno con foto en el que el modelo no dejó ninguna duda. */
    static final String PEDIDO_DUDAS = "Ya armé y cargué lo que se lee en la foto. Tu respuesta tiene que ser UNA sola"
            + " invocación de ofrecer_opciones con en `preguntas` las dudas importantes de la foto (hasta 3; por ejemplo"
            + " qué significan los tildes, si un número es costo o lo que deben, cuál de dos totales es el correcto),"
            + " cada una con `tema` corto, `pregunta` y 2 a 4 respuestas cortas. No cambies nada más.";
    /** El pedido extra cuando Sami borró registros para recargarlos (como pidió la persona) y no los cargó. */
    static final String PEDIDO_RECARGA = "Ya borraste los registros que la persona pidió. Tu respuesta tiene que ser UNA"
            + " invocación de cargar_registros con las filas corregidas que la persona te indicó, en la pieza que"
            + " corresponda y con las etiquetas de sus datos. No cambies nada más. Lo que pidió la persona: ";
    private static final Pattern PIDE_CARGAR = Pattern.compile("(?iU)\\b(re)?carg\\w*");
    /** Verbos con los que Sami cuenta un cambio hecho («Armé tu sistema…»). */
    private static final Pattern AFIRMA_CAMBIOS = Pattern.compile(
            "(?iU)\\b(armé|sumé|agregué|renombré|moví|conecté|quité|cambié|cargué|creé|separé|borré|eliminé)\\b");

    /** Describe una acción; recibe sus argumentos y el nombre de cada pieza según cómo la nombran las acciones. */
    private interface Descripcion extends BiFunction<Map<String, Object>, Map<String, String>, String> {
    }

    private final Map<HerramientaTaller, Descripcion> descripciones = new EnumMap<>(HerramientaTaller.class);

    /**
     * @param imagen       la foto que adjuntó la persona; {@code null} si no hay. Va al modelo en el mismo turno y
     *                     cuenta como una consulta más, igual que un mensaje sin foto
     * @param limiteDiario consultas con IA por negocio y por día (se comparte el contador con el asistente de
     *                     plantillas)
     * @throws BusinessRuleException si se agotó el límite o la IA falló
     */
    private RespuestaTaller conversar(NegocioId negocioId, String mensaje, ImagenTaller imagen, EstadoTaller estado,
                                     List<MensajeTaller> historial, OffsetDateTime ahora, int limiteDiario) {
        LocalDate fecha = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!consumoSamiRepository.reservar(negocioId, fecha, limiteDiario, ahora)) {
            throw new BusinessRuleException(LIMITE_ALCANZADO);
        }
        KitRubro kit = detectar(mensaje, estado, historial).orElse(null);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(mensaje, estado, historial, kit, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        ValidacionTaller validacion = validar(resultado.llamadas(), estado);
        if (afirmaCambiosSinHacerlos(resultado, validacion)) {
            // Sami nunca dice que hizo algo que no hizo: se le pide una vez que lo haga con las herramientas. El reintento
            // no reserva otra consulta del día, pero sus tokens se registran.
            resultado = consultar(new ConsultaTaller(mensaje + RECORDATORIO, estado, historial, kit, imagen));
            consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
            validacion = validar(resultado.llamadas(), estado);
            if (afirmaCambiosSinHacerlos(resultado, validacion)) {
                return new RespuestaTaller(SIN_CAMBIOS, List.of(), validacion.preguntas());
            }
        }
        String texto = texto(resultado.texto(), validacion, estado);
        // Los ejemplos del kit sólo sirven para imaginar un sistema nuevo: con una foto, o ajustando uno armado, no.
        boolean sinDatosDeEjemplo = imagen != null || !estado.piezas().isEmpty();
        List<AccionTaller> acciones = sinDatosDeEjemplo ? sinEjemplos(validacion.acciones()) : validacion.acciones();
        List<OpcionesTaller> preguntas = preguntas(resultado, validacion);
        int sinCargar = validacion.registrosDescartados();
        if (imagen != null && sinRegistros(acciones)) {
            // Con una foto, el modelo a veces arma las piezas y se olvida de cargar lo que lee. Se le pide una vez que
            // cargue las filas, viendo ya las piezas armadas (y sus datos) para que use las etiquetas correctas. Si el
            // modelo no había preguntado nada, la duda de este segundo pedido reemplaza a la pregunta genérica del kit.
            ValidacionTaller segunda = registrosDeLaFoto(negocioId, fecha, ahora, imagen, estado, historial,
                    validacion.piezas());
            sinCargar += segunda.registrosDescartados();
            List<AccionTaller> registros = soloRegistros(segunda);
            if (!registros.isEmpty()) {
                acciones = new ArrayList<>(acciones);
                acciones.addAll(registros);
                texto = texto + " " + describirTodas(registros, validacion.piezas()) + ".";
            }
            if (!usoLaBotonera(resultado)) {
                preguntas = segunda.preguntas();
            }
        }
        if (borraSinRecargar(acciones, mensaje)) {
            // Sami borró lo que había para cargarlo de nuevo, como pidió la persona, pero se quedó a mitad de camino.
            // Se le pide una vez que cargue las filas corregidas, viendo ya el estado después de borrar.
            acciones = new ArrayList<>(acciones);
            List<AccionTaller> recarga = recarga(negocioId, fecha, ahora, imagen, estado, historial, mensaje,
                    validacion.piezas());
            acciones.addAll(recarga);
            if (!recarga.isEmpty()) {
                texto = texto + " " + describirTodas(recarga, validacion.piezas()) + ".";
            }
        }
        if (imagen != null && preguntas.isEmpty() && !preguntoEnElTexto(resultado)) {
            // Con una foto siempre hay algo para confirmar (los tildes, un número, un total): si el modelo no lo
            // preguntó, se le pide una vez, viendo ya lo armado, que junte sus dudas en la botonera.
            preguntas = dudasDeLaFoto(negocioId, fecha, ahora, imagen, estado, historial, validacion.piezas());
        }
        if (sinCargar > 0) {
            // Sami nunca afirma que cargó todo si parte de lo que leyó no se pudo cargar.
            texto = texto + " " + (sinCargar == 1 ? "No pude cargar 1 renglón de la foto"
                    : "No pude cargar " + sinCargar + " renglones de la foto") + ": faltan en la tabla, revisalos.";
        }
        return new RespuestaTaller(texto, List.copyOf(acciones), preguntas);
    }

    /**
     * Con una foto, lo que se lee son los datos de la persona: las filas de ejemplo (las del kit o las que invente el
     * modelo) no se suman a las piezas, para que no se mezclen con lo leído como si fueran suyas.
     */
    private List<AccionTaller> sinEjemplos(List<AccionTaller> acciones) {
        return acciones.stream().map(accion -> {
            if (!accion.argumentos().containsKey("ejemplos")) {
                return accion;
            }
            Map<String, Object> argumentos = new HashMap<>(accion.argumentos());
            argumentos.remove("ejemplos");
            return new AccionTaller(accion.herramienta(), argumentos);
        }).toList();
    }

    private boolean sinRegistros(List<AccionTaller> acciones) {
        return acciones.stream()
                .noneMatch(accion -> HerramientaTaller.CARGAR_REGISTROS.nombre().equals(accion.herramienta()));
    }

    private ValidacionTaller registrosDeLaFoto(NegocioId negocioId, LocalDate fecha, OffsetDateTime ahora,
                                               ImagenTaller imagen, EstadoTaller estado,
                                               List<MensajeTaller> historial, List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_REGISTROS, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return validar(resultado.llamadas(), armado);
    }

    private List<OpcionesTaller> dudasDeLaFoto(NegocioId negocioId, LocalDate fecha, OffsetDateTime ahora,
                                                ImagenTaller imagen, EstadoTaller estado,
                                                List<MensajeTaller> historial, List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_DUDAS, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return validar(resultado.llamadas(), armado).preguntas();
    }

    private boolean borraSinRecargar(List<AccionTaller> acciones, String mensaje) {
        return acciones.stream().anyMatch(accion -> HerramientaTaller.BORRAR_REGISTROS.nombre().equals(accion.herramienta()))
                && sinRegistros(acciones) && PIDE_CARGAR.matcher(mensaje).find();
    }

    private List<AccionTaller> recarga(NegocioId negocioId, LocalDate fecha, OffsetDateTime ahora, ImagenTaller imagen,
                                       EstadoTaller estado, List<MensajeTaller> historial, String mensaje,
                                       List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_RECARGA + mensaje, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return soloRegistros(validar(resultado.llamadas(), armado));
    }

    private List<AccionTaller> soloRegistros(ValidacionTaller validacion) {
        return validacion.acciones().stream()
                .filter(accion -> HerramientaTaller.CARGAR_REGISTROS.nombre().equals(accion.herramienta()))
                .toList();
    }

    private boolean afirmaCambiosSinHacerlos(ResultadoTallerIa resultado, ValidacionTaller validacion) {
        return validacion.acciones().isEmpty() && resultado.texto() != null
                && AFIRMA_CAMBIOS.matcher(resultado.texto()).find();
    }

    /**
     * Si el modelo ya preguntó en su texto sin usar la botonera, no se suma la pregunta del kit: serían dos preguntas
     * distintas a la vez.
     */
    private List<OpcionesTaller> preguntas(ResultadoTallerIa resultado, ValidacionTaller validacion) {
        return preguntoEnElTexto(resultado) && !usoLaBotonera(resultado) ? List.of() : validacion.preguntas();
    }

    private boolean preguntoEnElTexto(ResultadoTallerIa resultado) {
        return resultado.texto() != null && resultado.texto().contains("?");
    }

    private boolean usoLaBotonera(ResultadoTallerIa resultado) {
        return resultado.llamadas().stream()
                .anyMatch(llamada -> HerramientaTaller.OFRECER_OPCIONES.nombre().equals(llamada.nombre()));
    }

    private ResultadoTallerIa consultar(ConsultaTaller consulta) {
        try {
            return tallerIaPort.conversar(consulta);
        } catch (RuntimeException e) {
            throw new BusinessRuleException(IA_NO_DISPONIBLE);
        }
    }

    /**
     * Si el modelo no escribió nada (a veces sólo invoca herramientas), Sami cuenta lo que quedó hecho y, si hay
     * botonera, hace su pregunta. La charla muestra texto plano: se sacan las negritas de markdown que a veces escribe
     * el modelo ({@code **Turnos**}).
     */
    private String texto(String texto, ValidacionTaller validacion, EstadoTaller estado) {
        if (texto != null && !texto.isBlank()) {
            return texto.replace("**", "").strip();
        }
        String pregunta = validacion.preguntas().isEmpty() ? null : validacion.preguntas().getFirst().pregunta();
        if (validacion.acciones().isEmpty()) {
            return pregunta == null ? SIN_CAMBIOS : pregunta;
        }
        Map<String, String> nombres = new HashMap<>();
        estado.piezas().forEach(pieza -> nombres.put(pieza.id(), pieza.nombre()));
        String hechas = validacion.acciones().stream()
                .map(accion -> describir(accion, nombres))
                .collect(Collectors.joining(". "));
        return hechas + ". Mirá cómo queda." + (pregunta == null ? "" : " " + pregunta);
    }

    /** Las piezas que se suman en el turno se anotan con su nombre, así las acciones siguientes las nombran bien. */
    private String describir(AccionTaller accion, Map<String, String> nombres) {
        HerramientaTaller herramienta = HerramientaTaller.deNombre(accion.herramienta()).orElseThrow();
        if (herramienta == HerramientaTaller.AGREGAR_PIEZA) {
            String tipo = accion.argumentos().get("tipo").toString();
            Object nombre = accion.argumentos().get("nombre");
            nombres.put(referenciaNueva(accion.argumentos()), nombre == null
                    ? catalogoComponentes.buscar(tipo).map(ComponenteCatalogo::nombre).orElse(tipo)
                    : nombre.toString());
        }
        return descripciones.get(herramienta).apply(accion.argumentos(), nombres);
    }

    /** Cómo nombran las acciones a una pieza recién sumada: por su tipo, o por su nombre si es una tabla a medida. */
    private String referenciaNueva(Map<String, Object> argumentos) {
        String tipo = argumentos.get("tipo").toString();
        return CatalogoComponentes.TIPO_A_MEDIDA.equals(tipo) ? argumentos.get("nombre").toString() : tipo;
    }

    /** Describe acciones sobre piezas que pueden venir por id, tipo o nombre (las sumadas en el turno no tienen id). */
    private String describirTodas(List<AccionTaller> acciones, List<PiezaTaller> piezas) {
        Map<String, String> nombres = new HashMap<>();
        piezas.forEach(pieza -> {
            nombres.put(String.valueOf(pieza.id()), pieza.nombre());
            nombres.put(pieza.tipo(), pieza.nombre());
            nombres.put(pieza.nombre(), pieza.nombre());
        });
        return acciones.stream().map(accion -> describir(accion, nombres)).collect(Collectors.joining(". "));
    }

    /** Los criterios de un borrado en palabras: {@code Cliente es Ana y Fecha es 2026-10-18, o …}. */
    private String criterios(Object donde) {
        return ((List<?>) donde).stream()
                .map(criterio -> ((Map<?, ?>) criterio).entrySet().stream()
                        .map(dato -> dato.getKey() + " es " + dato.getValue())
                        .collect(Collectors.joining(" y ")))
                .collect(Collectors.joining(", o "));
    }

    private String registros(int cantidad) {
        return cantidad == 1 ? "Cargué 1 registro" : "Cargué " + cantidad + " registros";
    }

    private String pieza(Object referencia, Map<String, String> nombres) {
        return nombres.getOrDefault(String.valueOf(referencia), String.valueOf(referencia)).toLowerCase(Locale.ROOT);
    }

    private final NegocioService negocioService;
    private final TallerIaPort tallerIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final CatalogoComponentes catalogoComponentes;
    private final KitsRubro kitsRubro;
    private final List<TerminosDeKit> kits;
    private final Clock clock;
    private final int limiteDiario;

    public ConversarEnTaller(NegocioService negocioService, TallerIaPort tallerIaPort,
                             ConsumoSamiRepository consumoSamiRepository, CatalogoComponentes catalogoComponentes,
                             KitsRubro kitsRubro, Clock clock, int limiteDiario) {
        this.negocioService = negocioService;
        this.tallerIaPort = tallerIaPort;
        this.consumoSamiRepository = consumoSamiRepository;
        this.catalogoComponentes = catalogoComponentes;
        this.kitsRubro = kitsRubro;
        this.clock = clock;
        this.limiteDiario = limiteDiario;
        descripciones.put(HerramientaTaller.AGREGAR_PIEZA,
                (args, nombres) -> "Sumé " + pieza(referenciaNueva(args), nombres));
        descripciones.put(HerramientaTaller.QUITAR_PIEZA,
                (args, nombres) -> "Quité " + pieza(args.get("pieza"), nombres));
        descripciones.put(HerramientaTaller.RENOMBRAR_PIEZA,
                (args, nombres) -> "Renombré " + pieza(args.get("pieza"), nombres) + " a " + args.get("nombre"));
        descripciones.put(HerramientaTaller.RENOMBRAR_SISTEMA,
                (args, nombres) -> "Llamé al sistema " + args.get("nombre"));
        descripciones.put(HerramientaTaller.AGREGAR_CAMPO,
                (args, nombres) -> "Sumé " + args.get("etiqueta") + " a " + pieza(args.get("pieza"), nombres));
        descripciones.put(HerramientaTaller.QUITAR_CAMPO,
                (args, nombres) -> "Quité " + args.get("campo") + " de " + pieza(args.get("pieza"), nombres));
        descripciones.put(HerramientaTaller.EDITAR_CAMPO,
                (args, nombres) -> "Cambié " + args.get("campo") + " en " + pieza(args.get("pieza"), nombres));
        descripciones.put(HerramientaTaller.CARGAR_REGISTROS,
                (args, nombres) -> registros(((List<?>) args.get("filas")).size())
                        + " en " + pieza(args.get("pieza"), nombres));
        descripciones.put(HerramientaTaller.BORRAR_REGISTROS,
                (args, nombres) -> "Borré los registros de " + pieza(args.get("pieza"), nombres)
                        + " donde " + criterios(args.get("donde")));
        descripciones.put(HerramientaTaller.CONECTAR,
                (args, nombres) -> "Conecté " + pieza(args.get("desde"), nombres)
                        + " con " + pieza(args.get("hacia"), nombres));
        descripciones.put(HerramientaTaller.SEPARAR_EN_SECCION,
                (args, nombres) -> "Pasé " + pieza(args.get("pieza"), nombres)
                        + " a la sección " + args.get("seccion"));
        descripciones.put(HerramientaTaller.CAMBIAR_DISTRIBUCION,
                (args, nombres) -> "Cambié la distribución");
        manejadores.put(HerramientaTaller.AGREGAR_PIEZA, this::agregarPieza);
        manejadores.put(HerramientaTaller.QUITAR_PIEZA, this::quitarPieza);
        manejadores.put(HerramientaTaller.RENOMBRAR_PIEZA, this::renombrarPieza);
        manejadores.put(HerramientaTaller.RENOMBRAR_SISTEMA, this::renombrarSistema);
        manejadores.put(HerramientaTaller.AGREGAR_CAMPO, this::agregarCampo);
        manejadores.put(HerramientaTaller.QUITAR_CAMPO, this::quitarCampo);
        manejadores.put(HerramientaTaller.EDITAR_CAMPO, this::editarCampo);
        manejadores.put(HerramientaTaller.CARGAR_REGISTROS, this::cargarRegistros);
        manejadores.put(HerramientaTaller.BORRAR_REGISTROS, this::borrarRegistros);
        manejadores.put(HerramientaTaller.CONECTAR, this::conectar);
        manejadores.put(HerramientaTaller.SEPARAR_EN_SECCION, this::separarEnSeccion);
        manejadores.put(HerramientaTaller.CAMBIAR_DISTRIBUCION, this::cambiarDistribucion);
        this.kits = kitsRubro.kits().stream()
                .map(kit -> new TerminosDeKit(kit, kit.terminos().stream().map(TextoNormalizado::palabras).toList()))
                .toList();
    }

    public ConversarEnTallerResponse execute(ConversarEnTallerRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        RespuestaTaller respuesta = conversar(
                negocioId,
                request.mensaje(),
                request.imagenTaller(),
                request.estadoTaller(),
                request.historialTaller(),
                OffsetDateTime.now(clock),
                limiteDiario);
        return ConversarEnTallerResponse.de(respuesta);
    }

    // --- rubro ---
    static final int PESO_DEL_MENSAJE = 2;

    /** Un kit con sus términos ya normalizados. */
    private record TerminosDeKit(KitRubro kit, List<List<String>> frases) {
    }

    private Optional<KitRubro> detectar(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
        List<String> delMensaje = TextoNormalizado.palabras(Objects.requireNonNullElse(mensaje, ""));
        String resto = historial.stream().map(MensajeTaller::texto).collect(Collectors.joining(" "))
                + " " + Objects.requireNonNullElse(estado.nombre(), "");
        List<String> delResto = TextoNormalizado.palabras(resto);
        KitRubro elegido = null;
        int mejor = 0;
        for (TerminosDeKit kit : kits) {
            int puntaje = aciertos(kit.frases(), delMensaje) * PESO_DEL_MENSAJE + aciertos(kit.frases(), delResto);
            if (puntaje > mejor) {
                mejor = puntaje;
                elegido = kit.kit();
            }
        }
        return Optional.ofNullable(elegido);
    }

    /** Cada término encontrado suma sus palabras: «almacen natural» le gana a «almacen». */
    private int aciertos(List<List<String>> frases, List<String> palabras) {
        return frases.stream()
                .filter(frase -> TextoNormalizado.contiene(palabras, frase))
                .mapToInt(List::size)
                .sum();
    }

    // --- herramientas ---
    private static final Logger log = LoggerFactory.getLogger(ConversarEnTaller.class);

    private static final String TIPO_CLIENTES = "clients";
    /** En ventas el dato Cliente es el quinto; en pedidos ya es el segundo. */
    private static final List<String> TIPOS_CON_CLIENTE = List.of("sales");
    /** Etiqueta del dato cliente de ventas y pedidos en el catálogo de componentes. */
    private static final String CAMPO_CLIENTE = "Cliente";

    private interface Manejador extends BiFunction<Map<String, Object>, List<PiezaTaller>, Optional<AccionTaller>> {
    }

    /** Lo que se va acumulando en un turno. */
    private static final class Turno {

        private final List<PiezaTaller> piezas;
        private final List<AccionTaller> acciones = new ArrayList<>();
        private final List<OpcionesTaller> preguntas = new ArrayList<>();
        /** La pregunta del kit que se armó, por si el modelo no preguntó nada. */
        private OpcionesTaller preguntaDelKit;
        private int registrosDescartados;
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

        /** Las dudas válidas se juntan hasta el tope del turno; una repetida no se suma. */
        private void sumarPreguntas(List<OpcionesTaller> nuevas) {
            nuevas.stream()
                    .filter(nueva -> preguntas.stream().noneMatch(pregunta -> pregunta.pregunta().equals(nueva.pregunta())))
                    .forEach(nueva -> {
                        if (preguntas.size() < CatalogoTaller.MAX_PREGUNTAS) {
                            preguntas.add(nueva);
                        }
                    });
        }

        /** Una llamada del modelo cuenta una vez aunque se haya expandido en varias acciones. Las inválidas no cuentan. */
        private void sumarDelModelo(List<AccionTaller> hechas) {
            if (!hechas.isEmpty()) {
                delModelo++;
                acciones.addAll(hechas);
            }
        }
    }

    /** Lo que cambia un {@code editar_campo}, en el orden en que viaja; si una parte llegó inválida, no vale nada. */
    private static final class Cambios {

        private final Map<String, Object> argumentos = new LinkedHashMap<>();
        private boolean invalido;

        private void exigir(boolean valido) {
            invalido |= !valido;
        }

        private void anotar(String clave, Object valor) {
            argumentos.put(clave, valor);
        }

        private Optional<Map<String, Object>> validos() {
            return invalido || argumentos.isEmpty() ? Optional.empty() : Optional.of(argumentos);
        }
    }

    private final Map<HerramientaTaller, Manejador> manejadores = new EnumMap<>(HerramientaTaller.class);

    private ValidacionTaller validar(List<LlamadaHerramienta> llamadas, EstadoTaller estado) {
        Turno turno = new Turno(estado.piezas());
        for (LlamadaHerramienta llamada : llamadas) {
            HerramientaTaller herramienta = HerramientaTaller.deNombre(llamada.nombre()).orElse(null);
            if (herramienta == HerramientaTaller.OFRECER_OPCIONES) {
                List<OpcionesTaller> dudas = preguntas(llamada.argumentos());
                turno.sumarPreguntas(dudas);
                if (dudas.isEmpty()) {
                    log.warn("ofrecer_opciones descartada por inválida: {}", llamada.argumentos());
                }
            } else if (herramienta != null) {
                turno.sumarDelModelo(turno.admiteOtraDelModelo()
                        ? aplicar(herramienta, llamada.argumentos(), turno)
                        : List.of());
            }
        }
        // Al armar con un kit Sami siempre propone cómo seguir: si el modelo no preguntó, va la pregunta del kit.
        List<OpcionesTaller> preguntas = turno.preguntas.isEmpty() && turno.preguntaDelKit != null
                ? List.of(turno.preguntaDelKit)
                : List.copyOf(turno.preguntas);
        return new ValidacionTaller(
                List.copyOf(turno.acciones), preguntas, turno.registrosDescartados,
                List.copyOf(turno.piezas));
    }

    private List<AccionTaller> aplicar(HerramientaTaller herramienta, Map<String, Object> args, Turno turno) {
        if (herramienta == HerramientaTaller.USAR_KIT) {
            return usarKit(args, turno);
        }
        List<AccionTaller> hechas = manejadores.get(herramienta)
                .apply(sinEjemplosDelModelo(args), turno.piezas).stream().toList();
        if (herramienta == HerramientaTaller.CARGAR_REGISTROS) {
            contarRegistrosDescartados(args, hechas, turno);
        }
        return hechas;
    }

    /**
     * Suma las piezas del kit que el sistema todavía no tiene (un tipo que no se repite y ya está, o una tabla a
     * medida con el mismo nombre, se saltean) y después sus conexiones, sin pasarse del lugar que queda en el turno.
     * Si sumó algo, guarda la pregunta del kit.
     */
    /** Las filas pedidas que no llegaron a cargarse se cuentan (y se loguean) para que Sami no diga que cargó todo. */
    private void contarRegistrosDescartados(Map<String, Object> args, List<AccionTaller> hechas, Turno turno) {
        int cargadas = hechas.stream().mapToInt(accion -> ((List<?>) accion.argumentos().get("filas")).size()).sum();
        int descartadas = (args.get("filas") instanceof List<?> pedidas ? pedidas.size() : 0) - cargadas;
        if (descartadas > 0) {
            log.warn("cargar_registros: se descartaron {} filas de {} pedidas (sin el dato principal, con datos que"
                    + " no son de la pieza o pasadas del tope de {})", descartadas, descartadas + cargadas,
                    CatalogoTaller.MAX_REGISTROS);
            turno.registrosDescartados += descartadas;
        }
    }

    /**
     * Las filas que inventa el modelo («ejemplos» de una pieza) no pasan nunca: se mostrarían como si fueran datos de
     * la persona. Los únicos ejemplos válidos son los del kit del catálogo, que se suman sin pasar por acá.
     */
    private Map<String, Object> sinEjemplosDelModelo(Map<String, Object> args) {
        Map<String, Object> limpios = new LinkedHashMap<>(args);
        limpios.remove("ejemplos");
        return limpios;
    }

    private List<AccionTaller> usarKit(Map<String, Object> args, Turno turno) {
        List<AccionTaller> acciones = new ArrayList<>();
        int lugar = turno.lugar();
        clave(args.get("rubro")).flatMap(kitsRubro::buscar).ifPresent(kit -> {
            kit.piezas().forEach(pieza -> sumarSiHayLugar(
                    acciones, lugar, () -> agregarPieza(pieza.comoArgumentos(), turno.piezas)));
            kit.conexiones().forEach(conexion -> sumarSiHayLugar(
                    acciones, lugar, () -> conectar(argumentosDe(kit, conexion), turno.piezas)));
            clienteALaVista(kit).forEach(edicion -> sumarSiHayLugar(
                    acciones, lugar, () -> editarCampo(edicion, turno.piezas)));
            if (!acciones.isEmpty()) {
                turno.preguntaDelKit = kit.pregunta();
            }
        });
        return acciones;
    }

    /**
     * Si el kit trae clientes, en sus ventas el dato Cliente va al lugar 2: las tablas muestran sólo los
     * primeros datos, y lo que más se busca es qué se le vendió a quién.
     */
    private List<Map<String, Object>> clienteALaVista(KitRubro kit) {
        boolean conClientes = kit.piezas().stream().anyMatch(pieza -> TIPO_CLIENTES.equals(pieza.tipo()));
        if (!conClientes) {
            return List.of();
        }
        return kit.piezas().stream()
                .map(PiezaKit::tipo)
                .filter(TIPOS_CON_CLIENTE::contains)
                .<Map<String, Object>>map(tipo -> Map.of("pieza", tipo, "campo", CAMPO_CLIENTE, "posicion", 2))
                .toList();
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
        List<Map<String, Object>> ejemplos = filas(args.get("ejemplos"), campos, CatalogoTaller.MAX_FILAS_EJEMPLO);
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
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            // Renombrar al mismo nombre no es un cambio: el modelo a veces lo pide de paso.
            return nombre.filter(nuevo -> !nuevo.equals(pieza.nombre())).map(nuevo -> {
                piezas.set(indice, new PiezaTaller(pieza.id(), pieza.tipo(), nuevo, pieza.seccion(), pieza.campos()));
                return accionConPieza(HerramientaTaller.RENOMBRAR_PIEZA, pieza, "nombre", nuevo);
            });
        });
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

    /**
     * Edita un dato que ya existe. Cada parte que llega tiene que valer (si no, se descarta la llamada), salvo el tipo
     * de un vínculo o del primer dato, que no se cambia y se ignora. En la acción viajan sólo las partes que cambian y
     * {@code campo} con la etiqueta exacta que tenía; si no cambia nada, se descarta.
     */
    private Optional<AccionTaller> editarCampo(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> buscado = texto(args.get("campo"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            return buscado.flatMap(etiqueta -> campoDe(etiqueta, pieza.campos())).flatMap(campo ->
                    cambiosDe(args, pieza.campos(), campo).map(cambios -> {
                        piezas.set(indice, conCampos(pieza, camposEditados(pieza.campos(), campo, cambios)));
                        Map<String, Object> argumentos = new LinkedHashMap<>();
                        argumentos.put("pieza", referencia(pieza));
                        argumentos.put("campo", campo.etiqueta());
                        argumentos.putAll(cambios);
                        return new AccionTaller(HerramientaTaller.EDITAR_CAMPO.nombre(), argumentos);
                    }));
        });
    }

    private Optional<Map<String, Object>> cambiosDe(Map<String, Object> args, List<CampoTaller> campos,
                                                    CampoTaller campo) {
        Cambios cambios = new Cambios();
        etiquetaEditada(args.get("etiqueta"), campos, campo, cambios);
        String tipo = tipoEditado(args.get("tipo"), campos, campo, cambios);
        opcionesEditadas(args.get("opciones"), tipo, campo, cambios);
        if (llego(args.get("obligatorio"))) {
            cambios.exigir(args.get("obligatorio") instanceof Boolean);
            cambios.anotar("obligatorio", args.get("obligatorio"));
        }
        posicionEditada(args.get("posicion"), campos, campo, cambios);
        return cambios.validos();
    }

    /** La etiqueta nueva no puede repetir la de otro dato de la pieza; la misma de antes no es un cambio. */
    private void etiquetaEditada(Object valor, List<CampoTaller> campos, CampoTaller campo, Cambios cambios) {
        if (llego(valor)) {
            Optional<String> etiqueta = texto(valor, CatalogoTaller.MAX_LARGO_NOMBRE)
                    .filter(nueva -> campoDe(nueva, campos).filter(otro -> !otro.equals(campo)).isEmpty());
            cambios.exigir(etiqueta.isPresent());
            etiqueta.filter(nueva -> !nueva.equals(campo.etiqueta()))
                    .ifPresent(nueva -> cambios.anotar("etiqueta", nueva));
        }
    }

    /** El tipo con el que queda el dato. Un vínculo o el primer dato (el que nombra cada fila) lo conservan. */
    private String tipoEditado(Object valor, List<CampoTaller> campos, CampoTaller campo, Cambios cambios) {
        boolean fijo = CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) || campo.equals(campos.getFirst());
        if (fijo || !llego(valor)) {
            return campo.tipo();
        }
        Optional<String> tipo = clave(valor).filter(CatalogoTaller.TIPOS_DE_CAMPO::contains);
        cambios.exigir(tipo.isPresent());
        tipo.filter(nuevo -> !nuevo.equals(campo.tipo())).ifPresent(nuevo -> cambios.anotar("tipo", nuevo));
        return tipo.orElse(campo.tipo());
    }

    /**
     * Las opciones sólo valen si el dato queda como lista: son obligatorias si recién pasa a serlo y, si ya lo era,
     * reemplazan a las que tenía. En otro tipo se ignoran.
     */
    private void opcionesEditadas(Object valor, String tipo, CampoTaller campo, Cambios cambios) {
        boolean esLista = CatalogoTaller.TIPO_LISTA.equals(tipo);
        if (esLista && (llego(valor) || !tipo.equals(campo.tipo()))) {
            List<String> opciones = textos(valor).stream().limit(CatalogoTaller.MAX_OPCIONES).toList();
            cambios.exigir(opciones.size() >= CatalogoTaller.MIN_OPCIONES);
            cambios.anotar("opciones", opciones);
        }
    }

    /** La posición empieza en 1 y se acota a la cantidad de datos de la pieza. */
    private void posicionEditada(Object valor, List<CampoTaller> campos, CampoTaller campo, Cambios cambios) {
        if (llego(valor)) {
            Optional<Long> entera = Optional.of(valor)
                    .filter(Number.class::isInstance)
                    .map(numero -> ((Number) numero).doubleValue())
                    .filter(numero -> numero == Math.rint(numero))
                    .map(Math::round);
            cambios.exigir(entera.isPresent());
            entera.map(posicion -> Math.clamp(posicion, 1, campos.size()))
                    .filter(posicion -> posicion != campos.indexOf(campo) + 1)
                    .ifPresent(posicion -> cambios.anotar("posicion", posicion));
        }
    }

    /** Los datos de la pieza con el dato editado, en su lugar nuevo, para las llamadas siguientes del turno. */
    private List<CampoTaller> camposEditados(List<CampoTaller> campos, CampoTaller campo, Map<String, Object> cambios) {
        List<CampoTaller> editados = new ArrayList<>(campos);
        int lugar = editados.indexOf(campo);
        editados.remove(lugar);
        CampoTaller editado = new CampoTaller(
                (String) cambios.getOrDefault("etiqueta", campo.etiqueta()),
                (String) cambios.getOrDefault("tipo", campo.tipo()));
        editados.add((int) cambios.getOrDefault("posicion", lugar + 1) - 1, editado);
        return editados;
    }

    /** Si el modelo mandó algo en un argumento optativo: un nulo, un texto en blanco o una lista vacía no cuentan. */
    private boolean llego(Object valor) {
        return valor != null
                && !(valor instanceof String texto && texto.isBlank())
                && !(valor instanceof List<?> lista && lista.isEmpty());
    }

    /** {@code cargar_registros}: registros reales (leídos de una foto, por ejemplo) que se suman a los que haya. */
    private Optional<AccionTaller> cargarRegistros(Map<String, Object> args, List<PiezaTaller> piezas) {
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            return Optional.of(filas(args.get("filas"), pieza.campos(), CatalogoTaller.MAX_REGISTROS))
                    .filter(filas -> !filas.isEmpty())
                    .map(filas -> {
                        Map<String, Object> argumentos = new LinkedHashMap<>();
                        argumentos.put("pieza", referencia(pieza));
                        argumentos.put("filas", filas);
                        return new AccionTaller(HerramientaTaller.CARGAR_REGISTROS.nombre(), argumentos);
                    });
        });
    }

    /**
     * {@code borrar_registros} quita de una pieza los registros que coinciden con lo que se indica en {@code donde}: un
     * criterio ({@code {Cliente: "Anto Solari"}}, todos sus datos tienen que coincidir) o una lista de criterios (basta
     * con que coincida uno). Nunca borra "todo": un criterio vacío, con un dato que la pieza no tiene o con un valor
     * que no es simple se descarta entero, porque ignorarlo ampliaría lo que se borra. Sami no conoce los ids de las
     * filas, así que las identifica por sus datos; las filas coincidentes las busca el taller.
     */
    private Optional<AccionTaller> borrarRegistros(Map<String, Object> args, List<PiezaTaller> piezas) {
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> {
            PiezaTaller pieza = piezas.get(indice);
            Object pedidos = args.get("donde") instanceof Map<?, ?> unico ? List.of(unico) : args.get("donde");
            List<Map<String, Object>> criterios = mapas(pedidos).stream()
                    .map(pedido -> criterioDeBorrado(pedido, pieza.campos()))
                    .flatMap(Optional::stream)
                    .limit(CatalogoTaller.MAX_CRITERIOS_BORRADO)
                    .toList();
            return Optional.of(criterios).filter(validos -> !validos.isEmpty()).map(validos -> {
                Map<String, Object> argumentos = new LinkedHashMap<>();
                argumentos.put("pieza", referencia(pieza));
                argumentos.put("donde", validos);
                return new AccionTaller(HerramientaTaller.BORRAR_REGISTROS.nombre(), argumentos);
            });
        });
    }

    /** El criterio con las etiquetas de la pieza; si algún dato no vale, el criterio entero no vale. */
    private Optional<Map<String, Object>> criterioDeBorrado(Map<String, Object> pedido, List<CampoTaller> campos) {
        Map<String, Object> criterio = fila(pedido, campos);
        return Optional.of(criterio).filter(valido -> !pedido.isEmpty() && valido.size() == pedido.size());
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

    /**
     * Las dudas de una llamada a {@code ofrecer_opciones}: una sola ({@code pregunta} y {@code opciones} en los
     * argumentos) o varias ({@code preguntas}: una lista de ellas). Las que no valen se descartan.
     */
    private List<OpcionesTaller> preguntas(Map<String, Object> args) {
        return Stream.concat(mapas(args.get("preguntas")).stream(), Stream.of(args))
                .flatMap(duda -> opciones(duda).stream())
                .toList();
    }

    private Optional<OpcionesTaller> opciones(Map<String, Object> args) {
        List<String> opciones = textos(args.get("opciones")).stream()
                .limit(CatalogoTaller.MAX_OPCIONES)
                .toList();
        return texto(args.get("pregunta"), CatalogoTaller.MAX_LARGO_PREGUNTA)
                .filter(pregunta -> opciones.size() >= CatalogoTaller.MIN_OPCIONES)
                .map(pregunta -> new OpcionesTaller(
                        texto(args.get("tema"), CatalogoTaller.MAX_LARGO_TEMA).orElse(null), pregunta, opciones));
    }

    /**
     * Filas (de ejemplo o registros) con etiquetas de {@code campos} y valores simples; las celdas inválidas se caen.
     * Una fila sin el dato principal de la pieza (el primero: Producto, Nombre, Pedido…) también se cae: en la tabla se
     * vería vacía. Quedan hasta {@code maximo}.
     */
    private List<Map<String, Object>> filas(Object valor, List<CampoTaller> campos, int maximo) {
        Optional<String> principal = campos.stream().findFirst().map(CampoTaller::etiqueta);
        return mapas(valor).stream()
                .map(pedida -> fila(pedida, campos))
                .filter(fila -> principal.map(fila::containsKey).orElse(false))
                .limit(maximo)
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
        return TextoNormalizado.clave(Objects.requireNonNullElse(nombre, ""))
                .equals(TextoNormalizado.clave(buscado));
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

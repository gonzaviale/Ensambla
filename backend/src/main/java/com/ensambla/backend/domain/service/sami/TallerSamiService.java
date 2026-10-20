package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Un turno de la charla con Sami en el taller: se reconoce el rubro (sin IA) para mandarle al modelo sólo el kit que
 * importa, el modelo conversa y pide herramientas, el dominio valida lo que pidió y devuelve acciones para que el
 * taller las aplique. Reserva la consulta del día del negocio antes de llamar (un rechazo por límite no cuesta nada)
 * y registra los tokens gastados. El día del límite es UTC.
 */
@Service
public class TallerSamiService {

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

    private final TallerIaPort tallerIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final HerramientasTallerService herramientasTallerService;
    private final RubroTallerService rubroTallerService;
    private final CatalogoComponentes catalogoComponentes;
    private final Map<HerramientaTaller, Descripcion> descripciones = new EnumMap<>(HerramientaTaller.class);

    public TallerSamiService(TallerIaPort tallerIaPort, ConsumoSamiRepository consumoSamiRepository,
                             HerramientasTallerService herramientasTallerService,
                             RubroTallerService rubroTallerService, CatalogoComponentes catalogoComponentes) {
        this.tallerIaPort = tallerIaPort;
        this.consumoSamiRepository = consumoSamiRepository;
        this.herramientasTallerService = herramientasTallerService;
        this.rubroTallerService = rubroTallerService;
        this.catalogoComponentes = catalogoComponentes;
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
    }

    /**
     * @param imagen       la foto que adjuntó la persona; {@code null} si no hay. Va al modelo en el mismo turno y
     *                     cuenta como una consulta más, igual que un mensaje sin foto
     * @param limiteDiario consultas con IA por negocio y por día (se comparte el contador con el asistente de
     *                     plantillas)
     * @throws BusinessRuleException si se agotó el límite o la IA falló
     */
    public RespuestaTaller conversar(UUID negocioId, String mensaje, ImagenTaller imagen, EstadoTaller estado,
                                     List<MensajeTaller> historial, OffsetDateTime ahora, int limiteDiario) {
        LocalDate fecha = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!consumoSamiRepository.reservar(negocioId, fecha, limiteDiario, ahora)) {
            throw new BusinessRuleException(LIMITE_ALCANZADO);
        }
        KitRubro kit = rubroTallerService.detectar(mensaje, estado, historial).orElse(null);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(mensaje, estado, historial, kit, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        ValidacionTaller validacion = herramientasTallerService.validar(resultado.llamadas(), estado);
        if (afirmaCambiosSinHacerlos(resultado, validacion)) {
            // Sami nunca dice que hizo algo que no hizo: se le pide una vez que lo haga con las herramientas. El reintento
            // no reserva otra consulta del día, pero sus tokens se registran.
            resultado = consultar(new ConsultaTaller(mensaje + RECORDATORIO, estado, historial, kit, imagen));
            consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
            validacion = herramientasTallerService.validar(resultado.llamadas(), estado);
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

    private ValidacionTaller registrosDeLaFoto(UUID negocioId, LocalDate fecha, OffsetDateTime ahora,
                                               ImagenTaller imagen, EstadoTaller estado,
                                               List<MensajeTaller> historial, List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_REGISTROS, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return herramientasTallerService.validar(resultado.llamadas(), armado);
    }

    private List<OpcionesTaller> dudasDeLaFoto(UUID negocioId, LocalDate fecha, OffsetDateTime ahora,
                                                ImagenTaller imagen, EstadoTaller estado,
                                                List<MensajeTaller> historial, List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_DUDAS, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return herramientasTallerService.validar(resultado.llamadas(), armado).preguntas();
    }

    private boolean borraSinRecargar(List<AccionTaller> acciones, String mensaje) {
        return acciones.stream().anyMatch(accion -> HerramientaTaller.BORRAR_REGISTROS.nombre().equals(accion.herramienta()))
                && sinRegistros(acciones) && PIDE_CARGAR.matcher(mensaje).find();
    }

    private List<AccionTaller> recarga(UUID negocioId, LocalDate fecha, OffsetDateTime ahora, ImagenTaller imagen,
                                       EstadoTaller estado, List<MensajeTaller> historial, String mensaje,
                                       List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_RECARGA + mensaje, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return soloRegistros(herramientasTallerService.validar(resultado.llamadas(), armado));
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
}

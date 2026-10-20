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
    static final String PEDIDO_REGISTROS = "Ya armé las piezas que ves. Ahora invocá cargar_registros con todas las"
            + " filas que se leen en la foto, en la pieza que corresponda y con las etiquetas de sus datos. No cambies"
            + " nada más.";
    /** Verbos con los que Sami cuenta un cambio hecho («Armé tu sistema…»). */
    private static final Pattern AFIRMA_CAMBIOS = Pattern.compile(
            "(?iU)\\b(armé|sumé|agregué|renombré|moví|conecté|quité|cambié|cargué|creé|separé)\\b");

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
        descripciones.put(HerramientaTaller.CARGAR_EJEMPLOS,
                (args, nombres) -> "Cargué ejemplos en " + pieza(args.get("pieza"), nombres));
        descripciones.put(HerramientaTaller.CARGAR_REGISTROS,
                (args, nombres) -> registros(((List<?>) args.get("filas")).size())
                        + " en " + pieza(args.get("pieza"), nombres));
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
                return new RespuestaTaller(SIN_CAMBIOS, List.of(), validacion.opciones());
            }
        }
        String texto = texto(resultado.texto(), validacion, estado);
        List<AccionTaller> acciones = validacion.acciones();
        if (imagen != null && sinRegistros(acciones)) {
            // Con una foto, el modelo a veces arma las piezas y se olvida de cargar lo que lee. Se le pide una vez que
            // cargue las filas, viendo ya las piezas armadas (y sus datos) para que use las etiquetas correctas.
            List<AccionTaller> registros = registrosDeLaFoto(negocioId, fecha, ahora, imagen, estado, historial,
                    validacion.piezas());
            if (!registros.isEmpty()) {
                acciones = new ArrayList<>(acciones);
                acciones.addAll(registros);
                texto = texto + " " + describirTodas(registros, validacion.piezas()) + ".";
            }
        }
        return new RespuestaTaller(texto, List.copyOf(acciones), opciones(resultado, validacion));
    }

    private boolean sinRegistros(List<AccionTaller> acciones) {
        return acciones.stream()
                .noneMatch(accion -> HerramientaTaller.CARGAR_REGISTROS.nombre().equals(accion.herramienta()));
    }

    private List<AccionTaller> registrosDeLaFoto(UUID negocioId, LocalDate fecha, OffsetDateTime ahora,
                                                 ImagenTaller imagen, EstadoTaller estado,
                                                 List<MensajeTaller> historial, List<PiezaTaller> piezas) {
        EstadoTaller armado = new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(PEDIDO_REGISTROS, armado, historial, null, imagen));
        consumoSamiRepository.registrar(negocioId, fecha, resultado.consumo(), ahora);
        return herramientasTallerService.validar(resultado.llamadas(), armado).acciones().stream()
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
    private OpcionesTaller opciones(ResultadoTallerIa resultado, ValidacionTaller validacion) {
        boolean preguntoEnElTexto = resultado.texto() != null && resultado.texto().contains("?");
        boolean usoLaBotonera = resultado.llamadas().stream()
                .anyMatch(llamada -> HerramientaTaller.OFRECER_OPCIONES.nombre().equals(llamada.nombre()));
        return preguntoEnElTexto && !usoLaBotonera ? null : validacion.opciones();
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
        String pregunta = validacion.opciones() == null ? null : validacion.opciones().pregunta();
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

    private String registros(int cantidad) {
        return cantidad == 1 ? "Cargué 1 registro" : "Cargué " + cantidad + " registros";
    }

    private String pieza(Object referencia, Map<String, String> nombres) {
        return nombres.getOrDefault(String.valueOf(referencia), String.valueOf(referencia)).toLowerCase(Locale.ROOT);
    }
}

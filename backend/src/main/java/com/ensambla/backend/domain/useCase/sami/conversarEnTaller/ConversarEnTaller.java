package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.DetectorDeRubro;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.model.sami.taller.herramienta.RepertorioDeHerramientas;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Un turno de la charla con Sami en el taller: se reconoce el rubro (sin IA) para mandarle al modelo sólo el kit que
 * importa, el modelo conversa y pide herramientas, el dominio valida lo que pidió y devuelve acciones para que el
 * taller las aplique. Reserva la consulta del día del negocio antes de llamar (un rechazo por límite no cuesta nada) y
 * registra los tokens gastados. El día del límite es el argentino ({@link ZonaNegocio}).
 * <p>
 * El modelo nunca escribe el sistema: cada herramienta que pide ({@link RepertorioDeHerramientas}) valida con
 * vocabulario cerrado y descarta lo que no exista, esté repetido o se pase de los topes. Las llamadas se validan en
 * orden sobre una copia del estado, así una herramienta puede apoyarse en una pieza que se sumó un instante antes.
 * Si se agotó el límite del día o la IA falló, el turno termina con una {@link BusinessRuleException}.
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
            + " etiquetas de sus datos, SIN saltearte ningún renglón; en Cliente y Producto usá el nombre EXACTO de los"
            + " existentes si es el mismo y, si no está, como se lee (no cargues productos ni clientes: el taller le"
            + " pregunta a la persona si los suma); (2) ofrecer_opciones UNA sola vez, con en `preguntas` TODAS las dudas"
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

    private static final Logger log = LoggerFactory.getLogger(ConversarEnTaller.class);

    private final NegocioRepository negocioRepository;
    private final TallerIaPort tallerIaPort;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final ControlPlanService controlPlanService;
    private final Clock clock;
    private final int limiteDiario;
    private final int limiteDiarioFotos;
    private final RepertorioDeHerramientas herramientas;
    private final DetectorDeRubro detectorDeRubro;
    private final RedaccionDelTurno redaccion;

    public ConversarEnTaller(NegocioRepository negocioRepository, TallerIaPort tallerIaPort,
                             ConsumoSamiRepository consumoSamiRepository, CatalogoComponentes catalogoComponentes,
                             KitsRubro kitsRubro, ControlPlanService controlPlanService, Clock clock,
                             int limiteDiario, int limiteDiarioFotos) {
        this.negocioRepository = negocioRepository;
        this.tallerIaPort = tallerIaPort;
        this.consumoSamiRepository = consumoSamiRepository;
        this.controlPlanService = controlPlanService;
        this.clock = clock;
        this.limiteDiario = limiteDiario;
        this.limiteDiarioFotos = limiteDiarioFotos;
        this.herramientas = new RepertorioDeHerramientas(catalogoComponentes, kitsRubro);
        this.detectorDeRubro = new DetectorDeRubro(kitsRubro);
        this.redaccion = new RedaccionDelTurno(herramientas);
    }

    public ConversarEnTallerResponse execute(ConversarEnTallerRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        RespuestaTaller respuesta = conversar(pedidoDe(negocioId, request));
        return ConversarEnTallerResponse.de(respuesta);
    }

    private RespuestaTaller conversar(PedidoDelTurno pedido) {
        reservarConsultas(pedido);
        KitRubro kit = detectorDeRubro.detectar(pedido.mensaje(), pedido.estado(), pedido.historial()).orElse(null);
        LecturaDelTurno lectura = consultarSinAfirmarDeMas(pedido, kit);
        if (lectura.afirmaCambiosSinHacerlos()) {
            return new RespuestaTaller(SIN_CAMBIOS, List.of(), lectura.validacion().preguntas());
        }
        return armarRespuesta(pedido, lectura);
    }

    private PedidoDelTurno pedidoDe(NegocioId negocioId, ConversarEnTallerRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return new PedidoDelTurno(negocioId, ZonaNegocio.dia(ahora), ahora, request.mensaje(), request.imagenTaller(),
                request.estadoTaller(), request.historialTaller());
    }

    /** Un rechazo por límite no cuesta nada: las consultas se reservan antes de llamar al modelo. */
    private void reservarConsultas(PedidoDelTurno pedido) {
        exigirIa(pedido);
        reservarConsultaDelDia(pedido);
        if (pedido.tieneImagen()) {
            reservarAnalisisDeFoto(pedido);
        }
    }

    /**
     * Sami nunca dice que hizo algo que no hizo: si lo afirma sin invocar herramientas, se le pide una vez que lo haga.
     * El reintento no reserva otra consulta del día, pero sus tokens se registran.
     */
    private LecturaDelTurno consultarSinAfirmarDeMas(PedidoDelTurno pedido, KitRubro kit) {
        LecturaDelTurno lectura = consultarYValidar(pedido, pedido.mensaje(), kit);
        return lectura.afirmaCambiosSinHacerlos()
                ? consultarYValidar(pedido, pedido.mensaje() + RECORDATORIO, kit)
                : lectura;
    }

    private RespuestaTaller armarRespuesta(PedidoDelTurno pedido, LecturaDelTurno lectura) {
        BorradorDeRespuesta borrador = borradorDe(pedido, lectura);
        cargarLoQueSeLeeEnLaFoto(pedido, lectura, borrador);
        recargarLoQueSeBorro(pedido, lectura, borrador);
        preguntarLasDudasDeLaFoto(pedido, lectura, borrador);
        return borrador.comoRespuesta();
    }

    private BorradorDeRespuesta borradorDe(PedidoDelTurno pedido, LecturaDelTurno lectura) {
        ValidacionTaller validacion = lectura.validacion();
        String texto = redaccion.texto(lectura.resultado().texto(), validacion, pedido.estado());
        return new BorradorDeRespuesta(texto, accionesDelTurno(pedido, validacion), lectura.preguntas(),
                validacion.registrosDescartados());
    }

    private List<AccionTaller> accionesDelTurno(PedidoDelTurno pedido, ValidacionTaller validacion) {
        return pedido.sinDatosDeEjemplo()
                ? validacion.acciones().stream().map(AccionTaller::sinEjemplos).toList()
                : validacion.acciones();
    }

    /**
     * Con una foto, el modelo a veces arma las piezas y se olvida de cargar lo que lee. Se le pide una vez que cargue
     * las filas, viendo ya las piezas armadas (y sus datos) para que use las etiquetas correctas. Si el modelo no había
     * preguntado nada, la duda de este segundo pedido reemplaza a la pregunta genérica del kit.
     */
    private void cargarLoQueSeLeeEnLaFoto(PedidoDelTurno pedido, LecturaDelTurno lectura, BorradorDeRespuesta borrador) {
        if (!pedido.tieneImagen() || !borrador.sinRegistros()) {
            return;
        }
        List<PiezaTaller> piezas = lectura.validacion().piezas();
        ValidacionTaller segunda = consultarConLasPiezasArmadas(pedido, PEDIDO_REGISTROS, piezas);
        borrador.contarSinCargar(segunda.registrosDescartados());
        sumarAcciones(borrador, soloRegistros(segunda), piezas);
        if (!lectura.usoLaBotonera()) {
            borrador.reemplazarPreguntas(segunda.preguntas());
        }
    }

    /**
     * Sami borró lo que había para cargarlo de nuevo, como pidió la persona, pero se quedó a mitad de camino: se le pide
     * una vez que cargue las filas corregidas, viendo ya el estado después de borrar.
     */
    private void recargarLoQueSeBorro(PedidoDelTurno pedido, LecturaDelTurno lectura, BorradorDeRespuesta borrador) {
        if (!borrador.borraRegistros() || !borrador.sinRegistros() || !pedido.pideCargar()) {
            return;
        }
        List<PiezaTaller> piezas = lectura.validacion().piezas();
        ValidacionTaller recarga = consultarConLasPiezasArmadas(pedido, PEDIDO_RECARGA + pedido.mensaje(), piezas);
        sumarAcciones(borrador, soloRegistros(recarga), piezas);
    }

    /**
     * Con una foto siempre hay algo para confirmar (los tildes, un número, un total): si el modelo no lo preguntó, se
     * le pide una vez, viendo ya lo armado, que junte sus dudas en la botonera.
     */
    private void preguntarLasDudasDeLaFoto(PedidoDelTurno pedido, LecturaDelTurno lectura,
                                           BorradorDeRespuesta borrador) {
        if (pedido.tieneImagen() && borrador.sinPreguntas() && !lectura.preguntoEnElTexto()) {
            ValidacionTaller dudas = consultarConLasPiezasArmadas(pedido, PEDIDO_DUDAS, lectura.validacion().piezas());
            borrador.reemplazarPreguntas(dudas.preguntas());
        }
    }

    private void sumarAcciones(BorradorDeRespuesta borrador, List<AccionTaller> nuevas, List<PiezaTaller> piezas) {
        if (!nuevas.isEmpty()) {
            borrador.sumar(nuevas, redaccion.describirTodas(nuevas, piezas));
        }
    }

    private List<AccionTaller> soloRegistros(ValidacionTaller validacion) {
        return validacion.acciones().stream()
                .filter(accion -> accion.es(HerramientaTaller.CARGAR_REGISTROS))
                .toList();
    }

    private LecturaDelTurno consultarYValidar(PedidoDelTurno pedido, String mensaje, KitRubro kit) {
        EstadoTaller estado = pedido.estado();
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(
                mensaje, estado, pedido.historial(), kit, pedido.imagen()));
        registrarConsumo(pedido, resultado);
        return new LecturaDelTurno(resultado, validar(resultado.llamadas(), estado));
    }

    private ValidacionTaller consultarConLasPiezasArmadas(PedidoDelTurno pedido, String mensaje,
                                                          List<PiezaTaller> piezas) {
        EstadoTaller armado = pedido.estadoConPiezas(piezas);
        ResultadoTallerIa resultado = consultar(new ConsultaTaller(
                mensaje, armado, pedido.historial(), null, pedido.imagen()));
        registrarConsumo(pedido, resultado);
        return validar(resultado.llamadas(), armado);
    }

    private ValidacionTaller validar(List<LlamadaHerramienta> llamadas, EstadoTaller estado) {
        TurnoDelTaller turno = new TurnoDelTaller(estado.piezas());
        llamadas.forEach(llamada -> herramientas.de(llamada.nombre())
                .ifPresent(herramienta -> herramienta.aplicarEn(turno, new ArgumentosDeHerramienta(llamada.argumentos()))));
        return turno.validacion();
    }

    private ResultadoTallerIa consultar(ConsultaTaller consulta) {
        try {
            return tallerIaPort.conversar(consulta);
        } catch (RuntimeException e) {
            log.warn("La IA del taller falló", e);
            throw new BusinessRuleException(IA_NO_DISPONIBLE);
        }
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirIa(PedidoDelTurno pedido) {
        controlPlanService.exigirIa(pedido.negocioId());
    }

    private void reservarConsultaDelDia(PedidoDelTurno pedido) {
        if (!consumoSamiRepository.reservar(pedido.negocioId(), pedido.fecha(), limiteDiario, pedido.ahora())) {
            throw new BusinessRuleException(LIMITE_ALCANZADO);
        }
    }

    /** Una foto es un análisis de foto del plan, sin importar cuántas veces el modelo se consulte en el turno. */
    private void reservarAnalisisDeFoto(PedidoDelTurno pedido) {
        controlPlanService.reservarPaginaIaInmediata(pedido.negocioId(), limiteDiarioFotos, pedido.ahora());
    }

    private void registrarConsumo(PedidoDelTurno pedido, ResultadoTallerIa resultado) {
        consumoSamiRepository.registrar(pedido.negocioId(), pedido.fecha(), resultado.consumo(), pedido.ahora());
    }
}

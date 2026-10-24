package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTaller;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.CampoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem.PiezaTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ImagenTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.MensajeTallerItem;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Corre turnos de {@link ConversarEnTaller} con objetos del dominio, como los arma el controller. Con
 * {@link #validar} se ve lo que el taller hace con las herramientas que pidió el modelo: el modelo contesta un texto
 * fijo y las llamadas dadas, sin foto ni charla, y la respuesta se lee como una {@link ValidacionTaller}.
 */
public final class TallerDePrueba {

    public static final UUID USUARIO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    public static final UUID NEGOCIO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    private static final String TEXTO_DEL_MODELO = "Listo.";
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 1, 1);
    private static final Pattern RENGLONES_SIN_CARGAR = Pattern.compile("No pude cargar (\\d+) rengl");

    private final TallerIaPort tallerIaPort = mock(TallerIaPort.class);
    private final ConversarEnTaller conversacion;

    public TallerDePrueba() {
        this(SamiPruebas.kitsReales());
    }

    public TallerDePrueba(KitsRubro kitsRubro) {
        ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
        when(consumoSamiRepository.reservar(any(), any(), anyInt(), any())).thenReturn(true);
        conversacion = conversacion(tallerIaPort, consumoSamiRepository, kitsRubro, Clock.systemUTC(), 1);
    }

    /** El caso de uso con el catálogo real, para el negocio {@link #NEGOCIO_ID} del usuario {@link #USUARIO_ID}. */
    public static ConversarEnTaller conversacion(TallerIaPort tallerIaPort, ConsumoSamiRepository consumoSamiRepository,
                                                 KitsRubro kitsRubro, Clock clock, int limiteDiario) {
        NegocioService negocioService = mock(NegocioService.class);
        lenient().when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        return new ConversarEnTaller(negocioService, tallerIaPort, consumoSamiRepository, SamiPruebas.catalogoReal(),
                kitsRubro, clock, limiteDiario);
    }

    /** Un turno del usuario {@link #USUARIO_ID}, con su respuesta leída como objetos del dominio. */
    public static RespuestaTaller conversar(ConversarEnTaller conversacion, String mensaje, ImagenTaller imagen,
                                            EstadoTaller estado, List<MensajeTaller> historial) {
        ConversarEnTallerResponse respuesta = conversacion.execute(new ConversarEnTallerRequest(USUARIO_ID, mensaje,
                estado(estado), historial.stream().map(m -> new MensajeTallerItem(m.quien(), m.texto())).toList(),
                imagen == null ? null : new ImagenTallerItem(imagen.tipo(), imagen.datos())));
        return new RespuestaTaller(respuesta.respuesta(),
                respuesta.acciones().stream()
                        .map(accion -> new AccionTaller(accion.herramienta(), accion.argumentos()))
                        .toList(),
                respuesta.preguntas().stream()
                        .map(duda -> new OpcionesTaller(duda.tema(), duda.pregunta(), duda.opciones()))
                        .toList());
    }

    /**
     * Lo que queda de {@code llamadas} después de validarlas sobre {@code estado}. Las filas que no se pudieron cargar
     * se leen del aviso que Sami suma al texto; las piezas no viajan en la respuesta y quedan vacías.
     */
    public ValidacionTaller validar(List<LlamadaHerramienta> llamadas, EstadoTaller estado) {
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(TEXTO_DEL_MODELO, llamadas, CONSUMO));
        RespuestaTaller respuesta = conversar(conversacion, "", null, estado, List.of());
        return new ValidacionTaller(respuesta.acciones(), respuesta.preguntas(),
                renglonesSinCargar(respuesta.texto()), List.of());
    }

    /** El kit del rubro que el taller le pasa a la IA para ese mensaje; vacío si no reconoce ninguno. */
    public Optional<KitRubro> rubro(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(TEXTO_DEL_MODELO, List.of(), CONSUMO));
        conversar(conversacion, mensaje, null, estado, historial);
        ArgumentCaptor<ConsultaTaller> consulta = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, atLeastOnce()).conversar(consulta.capture());
        return Optional.ofNullable(consulta.getValue().kit());
    }

    private static int renglonesSinCargar(String texto) {
        Matcher aviso = RENGLONES_SIN_CARGAR.matcher(texto);
        return aviso.find() ? Integer.parseInt(aviso.group(1)) : 0;
    }

    private static EstadoTallerItem estado(EstadoTaller estado) {
        return new EstadoTallerItem(estado.nombre(), estado.distribucion(), estado.piezas().stream()
                .map(pieza -> new PiezaTallerItem(pieza.id(), pieza.tipo(), pieza.nombre(), pieza.seccion(),
                        pieza.campos().stream()
                                .map(campo -> new CampoTallerItem(campo.etiqueta(), campo.tipo()))
                                .toList()))
                .toList());
    }
}

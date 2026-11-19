package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.RespuestaChat;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Arma, con los puertos del caso de uso, el despacho que atiende cada tipo de mensaje y las reacciones a cada
 * respuesta de la persona. Es el único lugar que conoce todas las piezas.
 */
@RequiredArgsConstructor
final class ArmadoDelDespacho {

    private final PropuestaWhatsappRepository propuestaRepository;
    private final ProcesamientoRepository procesamientoRepository;
    private final CampoRepository campoRepository;
    private final BloqueRepository bloqueRepository;
    private final RegistroRepository registroRepository;
    private final ControlPlanService controlPlanService;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final InterpreteRegistrosIaPort interpreteIa;
    private final LectorCuadernoPort lectorCuadernoPort;
    private final TransaccionPort transaccionPort;
    private final PoliticaAtencionWhatsapp politica;
    private final Conversacion conversacion;

    private PropuestasDelChat propuestas;
    private TablasDelNegocio tablas;
    private FilasDeLaIa filasDeLaIa;
    private EmparejamientoDeFilas emparejamiento;
    private ConsultaASami consulta;
    private PreguntarTabla preguntarTabla;
    private InterpretarTexto interpretarTexto;

    DespachoDeMensaje armar() {
        armarLoQueComparten();
        armarLaInterpretacionDeTexto();
        return new DespachoDeMensaje(propuestas, conversacion, manejadores());
    }

    private void armarLoQueComparten() {
        propuestas = new PropuestasDelChat(propuestaRepository, conversacion);
        tablas = new TablasDelNegocio(procesamientoRepository);
        filasDeLaIa = new FilasDeLaIa(politica.umbralConfianza());
        emparejamiento = new EmparejamientoDeFilas(registroRepository);
        preguntarTabla = new PreguntarTabla(propuestas, conversacion);
    }

    private void armarLaInterpretacionDeTexto() {
        consulta = new ConsultaASami(controlPlanService, consumoSamiRepository, interpreteIa, emparejamiento,
                politica);
        interpretarTexto = new InterpretarTexto(tablas, consulta, filasDeLaIa, emparejamiento, propuestas,
                preguntarTabla, conversacion, politica);
    }

    private Map<TipoMensajeWhatsapp, ManejadorDeMensaje> manejadores() {
        return Map.of(
                TipoMensajeWhatsapp.TEXTO, new ManejadorDeTexto(reacciones(), interpretarTexto),
                TipoMensajeWhatsapp.IMAGEN, manejadorDeImagen(),
                TipoMensajeWhatsapp.AUDIO, new ManejadorDeAudio(conversacion));
    }

    private ManejadorDeImagen manejadorDeImagen() {
        return new ManejadorDeImagen(tablas, conversacion, propuestas, controlPlanService, lectorCuadernoPort,
                filasDeLaIa, emparejamiento, preguntarTabla, politica);
    }

    private Map<RespuestaChat, ReaccionAUnaPropuesta> reacciones() {
        ReaccionAUnaPropuesta reemplazar = new ReemplazarPropuesta(propuestas, interpretarTexto);
        return Map.of(
                RespuestaChat.SI, confirmar(),
                RespuestaChat.NO, new DescartarPropuesta(propuestas),
                RespuestaChat.NUMERO, elegirTabla(reemplazar),
                RespuestaChat.OTRO, reemplazar);
    }

    private ConfirmarPropuesta confirmar() {
        return new ConfirmarPropuesta(transaccionPort, guardado(), conversacion);
    }

    private GuardadoDePropuesta guardado() {
        VinculosDeLaPropuesta vinculos = new VinculosDeLaPropuesta(campoRepository, registroRepository,
                controlPlanService);
        return new GuardadoDePropuesta(bloqueRepository, campoRepository, registroRepository, controlPlanService,
                propuestas, vinculos, new StockDeLaPropuesta(campoRepository, registroRepository));
    }

    private ElegirTabla elegirTabla(ReaccionAUnaPropuesta sinListaPendiente) {
        Map<OrigenPropuestaWhatsapp, ReinterpretarPropuesta> reinterpretaciones = Map.of(
                OrigenPropuestaWhatsapp.TEXTO, new ReinterpretarTexto(consulta, filasDeLaIa),
                OrigenPropuestaWhatsapp.FOTO, new ReasignarFoto(filasDeLaIa));
        return new ElegirTabla(propuestas, conversacion, tablas, emparejamiento, reinterpretaciones,
                sinListaPendiente);
    }
}

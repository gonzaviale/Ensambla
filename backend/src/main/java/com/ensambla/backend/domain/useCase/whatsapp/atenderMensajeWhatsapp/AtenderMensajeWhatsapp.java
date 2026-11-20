package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Atiende, de a uno y en orden, los mensajes que llegaron por WhatsApp. Un texto o una foto se convierte en una
 * propuesta de filas que se muestra por el chat y recién se guarda cuando la persona responde que sí. Los mismos
 * límites que en la app: las fotos descuentan del cupo de páginas del plan y los textos de las consultas diarias de
 * Sami. Tiene muchas dependencias a propósito: ningún caso de uso llama a otro, así que los pasos viven en las piezas
 * de este paquete y la lógica pura en el modelo.
 */
public class AtenderMensajeWhatsapp {

    private static final String MOTIVO_PERDIDO = "Se perdió mientras se atendía";

    private final MensajeWhatsappRepository mensajeRepository;
    private final VinculoWhatsappRepository vinculoRepository;
    private final Conversacion conversacion;
    private final DespachoDeMensaje despacho;
    private final Clock clock;
    private final PoliticaAtencionWhatsapp politica;

    @SuppressWarnings("java:S107")
    public AtenderMensajeWhatsapp(MensajeWhatsappRepository mensajeRepository,
                                  VinculoWhatsappRepository vinculoRepository,
                                  PropuestaWhatsappRepository propuestaRepository,
                                  ProcesamientoRepository procesamientoRepository, CampoRepository campoRepository,
                                  BloqueRepository bloqueRepository, RegistroRepository registroRepository,
                                  ControlPlanService controlPlanService, ConsumoSamiRepository consumoSamiRepository,
                                  InterpreteRegistrosIaPort interpreteIa, LectorCuadernoPort lectorCuadernoPort,
                                  MensajeriaWhatsappPort mensajeria, TransaccionPort transaccionPort, Clock clock,
                                  PoliticaAtencionWhatsapp politica) {
        this.mensajeRepository = mensajeRepository;
        this.vinculoRepository = vinculoRepository;
        this.conversacion = new Conversacion(mensajeria);
        this.despacho = new ArmadoDelDespacho(propuestaRepository, procesamientoRepository, campoRepository,
                bloqueRepository, registroRepository, controlPlanService, consumoSamiRepository, interpreteIa,
                lectorCuadernoPort, transaccionPort, politica, conversacion).armar();
        this.clock = clock;
        this.politica = politica;
    }

    /** @return si había un mensaje esperando; quien lo llama insiste mientras haya */
    public boolean execute() {
        OffsetDateTime ahora = ahora();
        recuperarMensajesPerdidos(ahora);
        Optional<MensajeWhatsapp> reclamado = reclamarElSiguiente(ahora);
        reclamado.ifPresent(this::atender);
        return reclamado.isPresent();
    }

    /** Un mensaje reclamado que nunca terminó (se cayó el proceso): no se reintenta, para no cobrar dos veces. */
    private void recuperarMensajesPerdidos(OffsetDateTime ahora) {
        for (MensajeWhatsapp perdido : buscarPerdidos(ahora)) {
            perdido.fallar(MOTIVO_PERDIDO, ahora);
            guardarMensaje(perdido);
            conversacion.responder(perdido, TextosWhatsapp.ERROR_TEMPORAL);
        }
    }

    private void atender(MensajeWhatsapp mensaje) {
        OffsetDateTime ahora = ahora();
        try {
            atenderYDarPorAtendido(mensaje, ahora);
        } catch (BusinessRuleException e) {
            explicarYDarPorAtendido(mensaje, e, ahora);
        } catch (RuntimeException e) {
            avisarDelErrorYFallar(mensaje, e, ahora);
        }
        guardarMensaje(mensaje);
    }

    private void atenderYDarPorAtendido(MensajeWhatsapp mensaje, OffsetDateTime ahora) {
        buscarVinculo(mensaje).ifPresentOrElse(
                vinculo -> despacho.despachar(new Atencion(mensaje, vinculo.getNegocioId(), ahora)),
                () -> conversacion.responder(mensaje, TextosWhatsapp.NO_VINCULADO));
        mensaje.atender(ahora);
    }

    private void explicarYDarPorAtendido(MensajeWhatsapp mensaje, BusinessRuleException motivo,
                                         OffsetDateTime ahora) {
        conversacion.responder(mensaje, motivo.getMessage());
        mensaje.atender(ahora);
    }

    private void avisarDelErrorYFallar(MensajeWhatsapp mensaje, RuntimeException error, OffsetDateTime ahora) {
        conversacion.responder(mensaje, TextosWhatsapp.ERROR_TEMPORAL);
        mensaje.fallar(error.getClass().getSimpleName() + ": " + error.getMessage(), ahora);
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(clock);
    }

    private List<MensajeWhatsapp> buscarPerdidos(OffsetDateTime ahora) {
        return mensajeRepository.vencidos(ahora);
    }

    private Optional<MensajeWhatsapp> reclamarElSiguiente(OffsetDateTime ahora) {
        return mensajeRepository.reclamarSiguiente(ahora, politica.vigenciaReclamo());
    }

    private Optional<VinculoWhatsapp> buscarVinculo(MensajeWhatsapp mensaje) {
        return vinculoRepository.findByTelefono(mensaje.getTelefono());
    }

    private void guardarMensaje(MensajeWhatsapp mensaje) {
        mensajeRepository.save(mensaje);
    }
}

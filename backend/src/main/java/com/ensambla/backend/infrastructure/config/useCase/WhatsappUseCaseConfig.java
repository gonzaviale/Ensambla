package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp.AtenderMensajeWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp.ConfirmarVerificacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacion;
import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.ProcesarMensajeEntrante;
import com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp.SolicitarVerificacionWhatsapp;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.ia.LectorCuadernoTimeoutAdapter;
import com.ensambla.backend.infrastructure.ia.SamiProperties;
import com.ensambla.backend.infrastructure.whatsapp.WhatsappProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.concurrent.ExecutorService;

@Configuration
public class WhatsappUseCaseConfig {

    @Bean
    public VinculacionWhatsappService vinculacionWhatsappService(
            VinculoWhatsappRepository vinculoRepository,
            VerificacionWhatsappRepository verificacionRepository) {
        return new VinculacionWhatsappService(vinculoRepository, verificacionRepository);
    }

    @Bean
    public GenerarCodigoVinculacion generarCodigoVinculacion(
            NegocioRepository negocioRepository,
            CodigoVinculacionRepository codigoRepository,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new GenerarCodigoVinculacion(
                negocioRepository,
                codigoRepository,
                mensajeria,
                transaccionPort,
                clock);
    }

    @Bean
    public SolicitarVerificacionWhatsapp solicitarVerificacionWhatsapp(
            NegocioRepository negocioRepository,
            VinculoWhatsappRepository vinculoRepository,
            VerificacionWhatsappRepository verificacionRepository,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new SolicitarVerificacionWhatsapp(
                negocioRepository,
                vinculoRepository,
                verificacionRepository,
                mensajeria,
                transaccionPort,
                clock);
    }

    @Bean
    public ConfirmarVerificacionWhatsapp confirmarVerificacionWhatsapp(
            NegocioRepository negocioRepository,
            VerificacionWhatsappRepository verificacionRepository,
            VinculacionWhatsappService vinculacionService,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ConfirmarVerificacionWhatsapp(
                negocioRepository,
                verificacionRepository,
                vinculacionService,
                mensajeria,
                transaccionPort,
                clock);
    }

    @Bean
    public ConsultarVinculacionWhatsapp consultarVinculacionWhatsapp(
            VinculoWhatsappRepository vinculoRepository,
            MensajeriaWhatsappPort mensajeria) {
        return new ConsultarVinculacionWhatsapp(vinculoRepository, mensajeria);
    }

    @Bean
    public DesvincularWhatsapp desvincularWhatsapp(
            VinculoWhatsappRepository vinculoRepository,
            TransaccionPort transaccionPort) {
        return new DesvincularWhatsapp(
                vinculoRepository,
                transaccionPort);
    }

    @Bean
    public ProcesarMensajeEntrante procesarMensajeEntrante(
            NegocioRepository negocioRepository,
            CodigoVinculacionRepository codigoRepository,
            VinculoWhatsappRepository vinculoRepository,
            MensajeWhatsappRepository mensajeRepository,
            VinculacionWhatsappService vinculacionService,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new ProcesarMensajeEntrante(
                negocioRepository,
                codigoRepository,
                vinculoRepository,
                mensajeRepository,
                vinculacionService,
                mensajeria,
                transaccionPort,
                clock);
    }

    /** La lectura de la foto es la misma de la app: con el mismo tope de espera por llamada a la IA. */
    @Bean
    public AtenderMensajeWhatsapp atenderMensajeWhatsapp(
            MensajeWhatsappRepository mensajeRepository,
            VinculoWhatsappRepository vinculoRepository,
            PropuestaWhatsappRepository propuestaRepository,
            ProcesamientoRepository procesamientoRepository,
            CampoRepository campoRepository,
            BloqueRepository bloqueRepository,
            RegistroRepository registroRepository,
            ControlPlanService controlPlanService,
            ConsumoSamiRepository consumoSamiRepository,
            InterpreteRegistrosIaPort interpreteIa,
            LectorCuadernoPort lectorCuadernoPort,
            ExecutorService executorLecturaImagen,
            IaProperties iaProperties,
            MensajeriaWhatsappPort mensajeria,
            TransaccionPort transaccionPort,
            Clock clock,
            SamiProperties samiProperties,
            PoliticaProcesamiento politicaProcesamiento,
            WhatsappProperties whatsappProperties) {
        WhatsappProperties.Atencion atencion = whatsappProperties.atencion();
        return new AtenderMensajeWhatsapp(
                mensajeRepository,
                vinculoRepository,
                propuestaRepository,
                procesamientoRepository,
                campoRepository,
                bloqueRepository,
                registroRepository,
                controlPlanService,
                consumoSamiRepository,
                interpreteIa,
                new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, executorLecturaImagen, iaProperties.timeout()),
                mensajeria,
                transaccionPort,
                clock,
                new PoliticaAtencionWhatsapp(samiProperties.limiteDiarioTaller(), politicaProcesamiento.limiteDiario(),
                        atencion.umbralConfianza(), atencion.vigenciaReclamo()));
    }
}

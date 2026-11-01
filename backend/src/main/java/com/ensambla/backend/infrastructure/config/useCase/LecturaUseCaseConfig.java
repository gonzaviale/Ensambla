package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLectura;
import com.ensambla.backend.domain.useCase.procesamiento.consultarConsumo.ConsultarConsumo;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagen;
import com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura.ObtenerLectura;
import com.ensambla.backend.domain.useCase.procesamiento.procesarLote.ProcesarLote;
import com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura.ReintentarLectura;
import com.ensambla.backend.infrastructure.ia.ArchivoCuadernoLocalAdapter;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.ia.LectorCuadernoTimeoutAdapter;
import com.ensambla.backend.infrastructure.ia.ProcesamientoIaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ProcesamientoIaProperties.class)
public class LecturaUseCaseConfig {

    @Bean
    public PoliticaProcesamiento politicaProcesamiento(ProcesamientoIaProperties properties, IaProperties ia) {
        if (ia.timeout().isZero() || ia.timeout().isNegative()
                || properties.vigenciaReclamo().compareTo(ia.timeout()) <= 0
                || properties.demoraReintento().isNegative()) {
            throw new IllegalArgumentException("Timeout positivo, reclamo mayor al timeout y demora no negativa");
        }
        return new PoliticaProcesamiento(properties.maxIntentos(), ia.maxPaginasPorLectura(),
                properties.limiteDiario(), properties.vigenciaReclamo(), properties.demoraReintento(),
                properties.precioEntradaMillon(), properties.precioSalidaMillon());
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService executorLecturaIa() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
    }

    /** Aparte de {@link #executorLecturaIa()}: la lectura de una foto es inmediata y no puede esperar al worker. */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService executorLecturaImagen() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public LeerImagen leerImagen(LectorCuadernoPort lectorCuadernoPort,
                                 ExecutorService executorLecturaImagen,
                                 IaProperties iaProperties,
                                 NegocioRepository negocioRepository,
                                 ControlPlanService controlPlanService,
                                 PoliticaProcesamiento politicaProcesamiento,
                                 Clock clock) {
        return new LeerImagen(
                new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, executorLecturaImagen, iaProperties.timeout()),
                iaProperties.umbralConfianza(), negocioRepository, controlPlanService,
                politicaProcesamiento, clock);
    }

    @Bean
    @ConditionalOnMissingBean(ArchivoCuadernoPort.class)
    public ArchivoCuadernoPort archivoCuadernoPort(ProcesamientoIaProperties properties) {
        return new ArchivoCuadernoLocalAdapter(Path.of(properties.archivosRaiz()), 20 * 1024 * 1024);
    }


    @Bean
    public ProcesarLote procesarLote(ProcesamientoRepository procesamientoRepository,
                                     ArchivoCuadernoPort archivoCuadernoPort,
                                     LectorCuadernoPort lectorCuadernoPort,
                                     CampoRepository campoRepository,
                                     RegistroDatosService registroDatosService,
                                     ControlPlanService controlPlanService,
                                     PoliticaProcesamiento politicaProcesamiento,
                                     ExecutorService executorLecturaIa,
                                     IaProperties iaProperties,
                                     Clock clock) {
        return new ProcesarLote(procesamientoRepository, archivoCuadernoPort,
                new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, executorLecturaIa, iaProperties.timeout()),
                campoRepository, registroDatosService, controlPlanService,
                politicaProcesamiento, clock);
    }

    @Bean
    public ObtenerLectura obtenerLectura(NegocioRepository negocioRepository,
                                         ProcesamientoRepository procesamientoRepository) {
        return new ObtenerLectura(negocioRepository, procesamientoRepository);
    }

    @Bean
    public ReintentarLectura reintentarLectura(NegocioRepository negocioRepository,
                                               ControlPlanService controlPlanService,
                                               ProcesamientoRepository procesamientoRepository,
                                               PoliticaProcesamiento politicaProcesamiento,
                                               Clock clock) {
        return new ReintentarLectura(negocioRepository, controlPlanService,
                procesamientoRepository, politicaProcesamiento, clock);
    }

    @Bean
    public ConfirmarLectura confirmarLectura(NegocioRepository negocioRepository,
                                             ProyectoRepository proyectoRepository,
                                             BloqueRepository bloqueRepository,
                                             CampoRepository campoRepository,
                                             RegistroDatosService registroDatosService,
                                             ControlPlanService controlPlanService,
                                             RegistroRepository registroRepository,
                                             LoteSubidaRepository loteSubidaRepository,
                                             TransaccionPort transaccionPort,
                                             Clock clock) {
        return new ConfirmarLectura(negocioRepository, proyectoRepository, bloqueRepository, campoRepository,
                registroDatosService, controlPlanService, registroRepository, loteSubidaRepository, transaccionPort,
                clock);
    }

    @Bean
    public ConsultarConsumo consultarConsumo(NegocioRepository negocioRepository,
                                             ProcesamientoRepository procesamientoRepository,
                                             PoliticaProcesamiento politicaProcesamiento, Clock clock) {
        return new ConsultarConsumo(negocioRepository, procesamientoRepository,
                politicaProcesamiento, clock);
    }
}

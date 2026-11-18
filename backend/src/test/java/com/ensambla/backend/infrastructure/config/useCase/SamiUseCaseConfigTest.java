package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ImagenTallerItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.ia.SamiProperties;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.SamiPruebas;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SamiUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.nuevo();
    private static final NegocioId NEGOCIO_DEL_TALLER = NegocioId.nuevo();
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("simulado", "rapido", 1, 1);

    private final SamiUseCaseConfig config = new SamiUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final SamiIaPort samiIaPort = mock(SamiIaPort.class);
    private final TallerIaPort tallerIaPort = mock(TallerIaPort.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private ConversarConSamiResponse conversacion;
    private ProyectoItem proyecto;
    private ConversarEnTallerResponse taller;

    @Test
    @DisplayName("El wiring de la conversación usa el umbral y el máximo de preguntas configurados")
    void laConversacionUsaElMaximoDePreguntasConfigurado() {
        dadasLasPlantillasActivas();

        cuandoSeConversaConMaximoDePreguntas(3);

        entoncesSamiPregunta();
    }

    @Test
    @DisplayName("Con el máximo de preguntas en cero consulta a la IA con el límite diario configurado")
    void sinPreguntasConsultaALaIaConElLimiteDiarioConfigurado() {
        dadasLasPlantillasActivas();
        dadoQueLaIaProponeConLimiteDiario(7);

        cuandoSeConversaConMaximoDePreguntas(0);

        entoncesLaPropuestaVieneDeLaIa();
    }

    @Test
    @DisplayName("El wiring de crear proyecto conecta negocio, plantilla y proyecto con el mapper")
    void crearProyectoConectaNegocioPlantillaYProyecto() {
        dadaUnaPlantillaQueCreaElProyecto();

        cuandoSeCreaElProyectoDesdeSami();

        entoncesSeCreoElProyecto();
    }

    @Test
    @DisplayName("El wiring del taller usa el límite diario propio del taller y el de análisis de foto del plan")
    void elTallerUsaSusLimitesDiarioYDeFoto() {
        dadoQueElTallerRespondeConLimiteDiario(11);

        cuandoSeConversaEnElTallerConLimiteDeFoto(13);

        entoncesElTallerSaludaYReservaLaFotoConLimite(13);
    }

    private void dadasLasPlantillasActivas() {
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(SamiPruebas.plantillas());
    }

    private void dadoQueLaIaProponeConLimiteDiario(int limite) {
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami("Ventas", List.of(), 0.9, CONSUMO));
        when(consumoSamiRepository.reservar(any(), any(), eq(limite), any())).thenReturn(true);
    }

    private void dadaUnaPlantillaQueCreaElProyecto() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(ProyectoTestData.NEGOCIO_ID);
        when(plantillaRepository.obtenerParaProyecto(ProyectoTestData.PLANTILLA_ID)).thenReturn(ProyectoTestData.plantilla());
        when(proyectoRepository.saveConEstructura(any())).thenReturn(ProyectoTestData.proyecto());
    }

    private void dadoQueElTallerRespondeConLimiteDiario(int limite) {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_DEL_TALLER);
        when(consumoSamiRepository.reservar(any(), any(), eq(limite), any())).thenReturn(true);
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola, ¿qué querés armar?", List.of(), CONSUMO));
    }

    private void cuandoSeConversaConMaximoDePreguntas(int maxPreguntas) {
        conversacion = config.conversarConSami(negocioRepository, plantillaRepository, SamiPruebas.diccionarioReal(),
                        samiIaPort, consumoSamiRepository, controlPlanService, Clock.systemUTC(), propiedades(maxPreguntas))
                .execute(new ConversarConSamiRequest(UsuarioId.nuevo(), "vendo velas", List.of()));
    }

    private void cuandoSeCreaElProyectoDesdeSami() {
        proyecto = config.crearProyectoDesdeSami(negocioRepository, plantillaRepository, proyectoRepository,
                        controlPlanService, new TransaccionDirecta(), Clock.systemUTC())
                .execute(new CrearProyectoDesdeSamiRequest(USUARIO_ID, ProyectoTestData.PLANTILLA_ID, null, List.of()));
    }

    private void cuandoSeConversaEnElTallerConLimiteDeFoto(int limiteDeFoto) {
        taller = config.conversarEnTaller(negocioRepository, tallerIaPort, consumoSamiRepository,
                        SamiPruebas.catalogoReal(), SamiPruebas.kitsReales(), controlPlanService,
                        politicaProcesamiento(limiteDeFoto), Clock.systemUTC(), propiedades(3))
                .execute(new ConversarEnTallerRequest(USUARIO_ID, "hola", new EstadoTallerItem("", "mosaic", List.of()),
                        List.of(), new ImagenTallerItem("image/png", new byte[]{1, 2, 3})));
    }

    private void entoncesSamiPregunta() {
        assertThat(conversacion.tipo()).isEqualTo("PREGUNTA");
    }

    private void entoncesLaPropuestaVieneDeLaIa() {
        assertThat(conversacion.propuesta().origen()).isEqualTo("IA");
    }

    private void entoncesSeCreoElProyecto() {
        assertThat(proyecto.id()).isEqualTo(ProyectoTestData.PROYECTO_ID);
    }

    private void entoncesElTallerSaludaYReservaLaFotoConLimite(int limiteDeFoto) {
        assertThat(taller.respuesta()).startsWith("Hola");
        verify(controlPlanService).reservarPaginaIaInmediata(eq(NEGOCIO_DEL_TALLER), eq(limiteDeFoto), any());
    }

    private static PoliticaProcesamiento politicaProcesamiento(int limiteDiario) {
        return new PoliticaProcesamiento(3, 10, limiteDiario, Duration.ofMinutes(5), Duration.ofSeconds(30),
                BigDecimal.ONE, BigDecimal.ONE);
    }

    private static SamiProperties propiedades(int maxPreguntas) {
        return new SamiProperties(0.7, maxPreguntas, 7, 11, new SamiProperties.Ia(
                "simulado", "us-east-1", null, "rapido", "avanzado", Duration.ofSeconds(30), 700, 0.6, 400));
    }
}
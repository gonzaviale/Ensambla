package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.ia.SamiProperties;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamiUseCaseConfigTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("simulado", "rapido", 1, 1);

    private final SamiUseCaseConfig config = new SamiUseCaseConfig();
    private final NegocioService negocioService = mock(NegocioService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);

    @Test
    @DisplayName("El wiring de la conversación usa el umbral y el máximo de preguntas configurados")
    void conversarConSamiUseCaseShouldWireDictionaryAndProperties() {
        ConversarConSamiResponse response = whenConversing(propiedades(3), mock(SamiIaPort.class));

        assertThat(response.tipo()).isEqualTo("PREGUNTA");
    }

    @Test
    @DisplayName("Con el máximo de preguntas en cero consulta a la IA con el límite diario configurado")
    void conversarConSamiUseCaseShouldUseConfiguredMaxQuestionsAndLimit() {
        SamiIaPort samiIaPort = mock(SamiIaPort.class);
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami("Ventas", List.of(), 0.9, CONSUMO));
        when(consumoSamiRepository.reservar(any(), any(), eq(7), any())).thenReturn(true);

        ConversarConSamiResponse response = whenConversing(propiedades(0), samiIaPort);

        assertThat(response.propuesta().origen()).isEqualTo("IA");
    }

    @Test
    @DisplayName("El wiring de crear proyecto conecta negocio, plantilla y proyecto con el mapper")
    void crearProyectoDesdeSamiUseCaseShouldWireServicesAndMapper() {
        PlantillaService plantillaService = mock(PlantillaService.class);
        ProyectoService proyectoService = mock(ProyectoService.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(ProyectoTestData.NEGOCIO_ID);
        when(plantillaService.obtenerParaProyecto(ProyectoTestData.PLANTILLA_ID))
                .thenReturn(ProyectoTestData.plantilla());
        when(proyectoService.crearDesdePlantilla(any(), any(), any(), any(), any()))
                .thenReturn(ProyectoTestData.proyecto());

        ProyectoItem result = config.crearProyectoDesdeSami(
                        negocioService, plantillaService, proyectoService, Clock.systemUTC())
                .execute(new CrearProyectoDesdeSamiRequest(
                        USUARIO_ID, ProyectoTestData.PLANTILLA_ID, null, List.of()));

        assertThat(result.id()).isEqualTo(ProyectoTestData.PROYECTO_ID);
    }

    @Test
    @DisplayName("El wiring del taller usa el límite diario propio del taller")
    void conversarEnTallerUseCaseShouldWireTallerLimit() {
        TallerIaPort tallerIaPort = mock(TallerIaPort.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(UUID.randomUUID());
        when(consumoSamiRepository.reservar(any(), any(), eq(11), any())).thenReturn(true);
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        ConversarEnTallerResponse response = config.conversarEnTaller(negocioService, tallerIaPort,
                        consumoSamiRepository, SamiPruebas.catalogoReal(), SamiPruebas.kitsReales(),
                        Clock.systemUTC(), propiedades(3))
                .execute(new ConversarEnTallerRequest(
                        USUARIO_ID, "hola", new EstadoTallerItem("", "mosaic", List.of()), List.of(), null));

        assertThat(response.respuesta()).isEqualTo("Hola");
    }

    private SamiProperties propiedades(int maxPreguntas) {
        return new SamiProperties(0.7, maxPreguntas, 7, 11, new SamiProperties.Ia(
                "simulado", "us-east-1", null, "rapido", "avanzado", Duration.ofSeconds(30), 700, 0.6, 400));
    }

    private ConversarConSamiResponse whenConversing(SamiProperties properties, SamiIaPort samiIaPort) {
        PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(SamiPruebas.plantillas());
        return config.conversarConSami(negocioService, plantillaRepository, SamiPruebas.diccionarioReal(), samiIaPort,
                        consumoSamiRepository, Clock.systemUTC(), properties)
                .execute(new ConversarConSamiRequest(UUID.randomUUID(), "vendo velas", List.of()));
    }
}

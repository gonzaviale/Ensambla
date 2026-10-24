package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.service.sami.ConversacionSamiService;
import com.ensambla.backend.domain.service.sami.InterpretacionSamiService;
import com.ensambla.backend.domain.service.sami.RespaldoIaSamiService;
import com.ensambla.backend.domain.service.sami.SamiPruebas;
import com.ensambla.backend.domain.service.sami.TallerSamiService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.EstadoTallerItem;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.ia.SamiProperties;
import com.ensambla.backend.support.ProyectoTestData;
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

    private final SamiUseCaseConfig config = new SamiUseCaseConfig();

    @Test
    @DisplayName("El wiring de la conversación usa el umbral y el máximo de preguntas configurados")
    void conversarConSamiUseCaseShouldWireServicesAndProperties() {
        ConversarConSamiResponse response = whenConversing(propiedades(3), "vendo velas");

        assertThat(response.tipo()).isEqualTo("PREGUNTA");
    }

    @Test
    @DisplayName("Con el máximo de preguntas en cero consulta a la IA sin preguntar")
    void conversarConSamiUseCaseShouldUseConfiguredMaxQuestions() {
        RespaldoIaSamiService respaldo = mock(RespaldoIaSamiService.class);
        when(respaldo.proponer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(7)))
                .thenReturn(new com.ensambla.backend.domain.model.sami.InterpretacionSami(
                        null, "Ventas", List.of(), 0.9, List.of()));

        ConversarConSamiResponse response = whenConversing(propiedades(0), "vendo velas", respaldo);

        assertThat(response.propuesta().origen()).isEqualTo("IA");
    }

    @Test
    @DisplayName("El wiring de crear proyecto conecta negocio, plantilla y proyecto con el mapper")
    void crearProyectoDesdeSamiUseCaseShouldWireServicesAndMapper() {
        NegocioService negocioService = mock(NegocioService.class);
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
    void conversarEnTallerUseCaseShouldWireServiceAndTallerLimit() {
        NegocioService negocioService = mock(NegocioService.class);
        TallerSamiService tallerSamiService = mock(TallerSamiService.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(UUID.randomUUID());
        when(tallerSamiService.conversar(any(), eq("hola"), any(), any(), any(), any(), eq(11)))
                .thenReturn(new RespuestaTaller("Hola", List.of(), List.of()));

        ConversarEnTallerResponse response = config.conversarEnTaller(
                        negocioService, tallerSamiService, Clock.systemUTC(),
                        propiedades(3))
                .execute(new ConversarEnTallerRequest(
                        USUARIO_ID, "hola", new EstadoTallerItem("", "mosaic", List.of()), List.of(), null));

        assertThat(response.respuesta()).isEqualTo("Hola");
    }

    private SamiProperties propiedades(int maxPreguntas) {
        return new SamiProperties(0.7, maxPreguntas, 7, 11, new SamiProperties.Ia(
                "simulado", "us-east-1", null, "rapido", "avanzado", Duration.ofSeconds(30), 700, 0.6, 400));
    }

    private ConversarConSamiResponse whenConversing(SamiProperties properties, String texto) {
        return whenConversing(properties, texto, mock(RespaldoIaSamiService.class));
    }

    private ConversarConSamiResponse whenConversing(SamiProperties properties, String texto,
                                                    RespaldoIaSamiService respaldo) {
        PlantillaService plantillaService = mock(PlantillaService.class);
        when(plantillaService.listarParaProyecto()).thenReturn(SamiPruebas.plantillas());
        NegocioService negocioService = mock(NegocioService.class);
        DiccionarioSami diccionario = SamiPruebas.diccionarioReal();
        ConversacionSamiService conversacion =
                new ConversacionSamiService(new InterpretacionSamiService(diccionario), diccionario);
        return config.conversarConSami(
                        negocioService, plantillaService, conversacion, respaldo, Clock.systemUTC(), properties)
                .execute(new ConversarConSamiRequest(UUID.randomUUID(), texto, List.of()));
    }
}

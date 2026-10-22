package com.ensambla.backend.domain.useCase.sami;

import com.ensambla.backend.domain.dto.sami.ConversarConSamiRequest;
import com.ensambla.backend.domain.dto.sami.ConversarConSamiResponse;
import com.ensambla.backend.domain.dto.sami.RespuestaSamiItem;
import com.ensambla.backend.domain.mapper.sami.ConversarConSamiMapper;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.sami.ConversacionSamiService;
import com.ensambla.backend.domain.service.sami.InterpretacionSamiService;
import com.ensambla.backend.domain.service.sami.RespaldoIaSamiService;
import com.ensambla.backend.domain.service.sami.SamiPruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversarConSamiUseCaseTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID NEGOCIO_ID = UUID.randomUUID();
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final int LIMITE_IA = 20;

    @Mock
    private NegocioService negocioService;

    @Mock
    private PlantillaService plantillaService;

    @Mock
    private RespaldoIaSamiService respaldoIaSamiService;

    private ConversacionSamiService conversacionSamiService;

    @BeforeEach
    void setUp() {
        DiccionarioSami diccionario = SamiPruebas.diccionarioReal();
        conversacionSamiService = new ConversacionSamiService(new InterpretacionSamiService(diccionario), diccionario);
    }

    @Test
    @DisplayName("Con un texto claro propone la plantilla con sus columnas extra sin tocar la IA")
    void executeShouldProposeTemplateForClearTextWithoutAi() {
        givenActiveTemplates(SamiPruebas.plantillas());

        ConversarConSamiResponse response = whenConversing(useCase(3), "Vendo velas y anoto quién me debe");

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.propuesta().plantilla()).isEqualTo("Ventas");
        assertThat(response.propuesta().columnas()).extracting("nombre").containsExactly("Saldo pendiente");
        assertThat(response.propuesta().origen()).isEqualTo("ALGORITMO");
        verify(respaldoIaSamiService, never()).proponer(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Con un texto ambiguo devuelve la pregunta que lo aclara")
    void executeShouldAskQuestionForAmbiguousText() {
        givenActiveTemplates(SamiPruebas.plantillas());

        ConversarConSamiResponse response = whenConversing(useCase(3), "quiero un stock y llevar los pedidos");

        assertThat(response.tipo()).isEqualTo("PREGUNTA");
        assertThat(response.pregunta().opciones()).isNotEmpty();
    }

    @Test
    @DisplayName("Tras responder la pregunta usa la respuesta para proponer")
    void executeShouldUseAnswersToPropose() {
        givenActiveTemplates(SamiPruebas.plantillas());

        ConversarConSamiResponse response = useCase(3).execute(new ConversarConSamiRequest(
                USUARIO_ID,
                "quiero un stock y llevar los pedidos",
                List.of(new RespuestaSamiItem("principal", "pedidos"))));

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.propuesta().plantilla()).isEqualTo("Pedidos");
        assertThat(response.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si el algoritmo no alcanza consulta a la IA para el negocio del usuario y marca el origen")
    void executeShouldFallBackToAiWhenAlgorithmIsNotEnough() {
        List<PlantillaProyecto> plantillas = SamiPruebas.plantillas();
        givenActiveTemplates(plantillas);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(respaldoIaSamiService.proponer(NEGOCIO_ID, "hola", List.of(), plantillas, AHORA, LIMITE_IA))
                .thenReturn(new InterpretacionSami(
                        plantillas.getFirst().id(), "Inventario", List.of(), 0.85, List.of()));

        ConversarConSamiResponse response = whenConversing(useCase(0), "hola");

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.propuesta().plantilla()).isEqualTo("Inventario");
        assertThat(response.propuesta().origen()).isEqualTo("IA");
        assertThat(response.propuesta().confianza()).isEqualTo(0.85);
    }

    // --- arrange ---
    private ConversarConSamiUseCase useCase(int maxPreguntas) {
        return new ConversarConSamiUseCase(
                negocioService,
                plantillaService,
                conversacionSamiService,
                respaldoIaSamiService,
                new ConversarConSamiMapper(),
                Clock.fixed(INSTANTE, ZoneOffset.UTC),
                0.7,
                maxPreguntas,
                LIMITE_IA);
    }

    private void givenActiveTemplates(List<PlantillaProyecto> plantillas) {
        when(plantillaService.listarParaProyecto()).thenReturn(plantillas);
    }

    // --- act ---
    private ConversarConSamiResponse whenConversing(ConversarConSamiUseCase useCase, String texto) {
        return useCase.execute(new ConversarConSamiRequest(USUARIO_ID, texto, List.of()));
    }
}

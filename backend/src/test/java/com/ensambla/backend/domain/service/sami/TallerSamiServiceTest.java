package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TallerSamiServiceTest {

    private static final UUID NEGOCIO_ID = UUID.randomUUID();
    private static final int LIMITE = 60;
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 800, 90);
    private static final EstadoTaller ESTADO = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of())));
    private static final List<MensajeTaller> HISTORIAL = List.of(new MensajeTaller("sami", "Hola"));

    @Mock
    private TallerIaPort tallerIaPort;

    @Mock
    private ConsumoSamiRepository consumoSamiRepository;

    private TallerSamiService service;

    @BeforeEach
    void setUp() {
        service = new TallerSamiService(tallerIaPort, consumoSamiRepository, new HerramientasTallerService());
    }

    @Test
    @DisplayName("Reserva, consulta a la IA, registra el consumo y devuelve el texto con las acciones validadas")
    void conversarShouldReserveCallRegisterAndValidate() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Listo, sumé gastos.", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("borrar_todo", Map.of())), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "sumá gastos", ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Listo, sumé gastos.");
        assertThat(result.acciones()).hasSize(1);
        assertThat(result.acciones().getFirst().herramienta()).isEqualTo("agregar_pieza");
        assertThat(result.opciones()).isNull();
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Le pasa a la IA el mensaje, el estado y la charla reciente")
    void conversarShouldSendMessageStateAndHistory() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        service.conversar(NEGOCIO_ID, "hola", ESTADO, HISTORIAL, AHORA, LIMITE);

        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort).conversar(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ConsultaTaller("hola", ESTADO, HISTORIAL));
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void conversarShouldNotCallAiWhenLimitIsReached() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> service.conversar(NEGOCIO_ID, "hola", ESTADO, HISTORIAL, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(TallerSamiService.LIMITE_ALCANZADO);
        verify(tallerIaPort, never()).conversar(any());
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple")
    void conversarShouldTranslateAiFailure() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> service.conversar(NEGOCIO_ID, "hola", ESTADO, HISTORIAL, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(TallerSamiService.IA_NO_DISPONIBLE);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si el modelo no escribió texto usa la pregunta de la botonera")
    void conversarShouldUseQuestionWhenThereIsNoText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("ofrecer_opciones", Map.of(
                        "pregunta", "¿Qué sumamos?", "opciones", List.of("Gastos", "Clientes")))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "ayuda", ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("¿Qué sumamos?");
        assertThat(result.opciones().opciones()).containsExactly("Gastos", "Clientes");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto cuenta cuántos cambios hizo")
    void conversarShouldDescribeChangesWhenThereIsNoText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("  ", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO));
        RespuestaTaller uno = service.conversar(NEGOCIO_ID, "gastos", ESTADO, HISTORIAL, AHORA, LIMITE);
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("cambiar_distribucion", Map.of("layout", "single"))), CONSUMO));

        RespuestaTaller dos = service.conversar(NEGOCIO_ID, "gastos", ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(uno.texto()).isEqualTo("Listo, hice el cambio. Mirá cómo queda.");
        assertThat(dos.texto()).isEqualTo("Listo, hice 2 cambios. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto y nada de lo que pidió sirve, lo dice")
    void conversarShouldAdmitItCouldNotDoAnythingWhenNothingIsValid() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "turnos"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "turnos", ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo(TallerSamiService.SIN_CAMBIOS);
        assertThat(result.acciones()).isEmpty();
    }

    @Test
    @DisplayName("Lleva el texto del modelo sin espacios sobrantes")
    void conversarShouldStripModelText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("  Hola  \n", List.of(), CONSUMO));

        assertThat(service.conversar(NEGOCIO_ID, "hola", ESTADO, HISTORIAL, AHORA, LIMITE).texto()).isEqualTo("Hola");
    }

    private void givenQuotaAvailable() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(true);
    }
}

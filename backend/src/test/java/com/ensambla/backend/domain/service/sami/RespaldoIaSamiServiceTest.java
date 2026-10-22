package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.port.SamiIaPort;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespaldoIaSamiServiceTest {

    private static final UUID NEGOCIO_ID = UUID.randomUUID();
    private static final int LIMITE = 20;
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 300, 40);

    @Mock
    private SamiIaPort samiIaPort;

    @Mock
    private ConsumoSamiRepository consumoSamiRepository;

    private RespaldoIaSamiService service;
    private List<PlantillaProyecto> plantillas;

    @BeforeEach
    void setUp() {
        service = new RespaldoIaSamiService(samiIaPort, consumoSamiRepository, SamiPruebas.diccionarioReal());
        plantillas = SamiPruebas.plantillas();
    }

    @Test
    @DisplayName("Reserva la consulta, llama a la IA, registra el consumo y devuelve la interpretación")
    void proponerShouldReserveCallRegisterAndInterpret() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(true);
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami("Gastos", List.of(), 0.9, CONSUMO));

        InterpretacionSami result = service.proponer(NEGOCIO_ID, "pago cosas", List.of(), plantillas, AHORA, LIMITE);

        assertThat(result.plantilla()).isEqualTo("Gastos");
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void proponerShouldNotCallAiWhenLimitIsReached() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> service.proponer(NEGOCIO_ID, "texto", List.of(), plantillas, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(RespaldoIaSamiService.LIMITE_ALCANZADO);
        verify(samiIaPort, never()).proponer(any());
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("El día del límite es UTC aunque la hora local ya sea otro día")
    void proponerShouldUseUtcDayForTheLimit() {
        OffsetDateTime tardeEnArgentina = OffsetDateTime.parse("2026-10-19T22:30:00-03:00");
        when(consumoSamiRepository.reservar(NEGOCIO_ID, LocalDate.of(2026, 10, 20), LIMITE, tardeEnArgentina))
                .thenReturn(false);

        assertThatThrownBy(() -> service.proponer(
                NEGOCIO_ID, "texto", List.of(), plantillas, tardeEnArgentina, LIMITE))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(tardeEnArgentina.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple y deja reservada la consulta")
    void proponerShouldTranslateAiFailure() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(true);
        when(samiIaPort.proponer(any())).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> service.proponer(NEGOCIO_ID, "texto", List.of(), plantillas, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(RespaldoIaSamiService.IA_NO_DISPONIBLE);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA inventó una plantilla igual registra lo que gastó y rechaza la propuesta")
    void proponerShouldRegisterConsumptionAndRejectInvalidProposal() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(true);
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami("Peluquería", List.of(), 0.9, CONSUMO));

        assertThatThrownBy(() -> service.proponer(NEGOCIO_ID, "texto", List.of(), plantillas, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(RespaldoIaSamiService.RESPUESTA_INVALIDA);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Le pasa a la IA el texto, las plantillas y las respuestas como «pregunta → opción»")
    void proponerShouldSendTextTemplatesAndReadableAnswers() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(true);
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami("Gastos", List.of(), 0.9, CONSUMO));

        service.proponer(NEGOCIO_ID, "quiero ordenar", List.of(
                        new RespuestaSami("principal", "gastos"),
                        new RespuestaSami("principal", "no-existe"),
                        new RespuestaSami("inventada", "x")),
                plantillas, AHORA, LIMITE);

        ArgumentCaptor<ConsultaIaSami> captor = ArgumentCaptor.forClass(ConsultaIaSami.class);
        verify(samiIaPort).proponer(captor.capture());
        assertThat(captor.getValue().texto()).isEqualTo("quiero ordenar");
        assertThat(captor.getValue().plantillas()).isSameAs(plantillas);
        assertThat(captor.getValue().aclaraciones())
                .containsExactly("¿Qué es lo principal que querés anotar? → Mis gastos");
    }
}

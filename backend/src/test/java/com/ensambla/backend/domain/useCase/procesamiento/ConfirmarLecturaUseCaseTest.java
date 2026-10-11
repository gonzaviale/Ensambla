package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest.CampoItem;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest.FilaItem;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaResponse;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.mapper.procesamiento.ConfirmarLecturaMapper;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ConfirmacionLecturaService;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmarLecturaUseCaseTest {

    private static final UUID PROYECTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000021");
    private static final UUID BLOQUE_ID = UUID.fromString("00000000-0000-4000-8000-000000000022");

    @Mock
    private NegocioService negocioService;

    @Mock
    private LoteSubidaService loteSubidaService;

    @Mock
    private ConfirmacionLecturaService confirmacionLecturaService;

    private ConfirmarLecturaUseCase confirmarLecturaUseCase;

    @BeforeEach
    void setUp() {
        confirmarLecturaUseCase = new ConfirmarLecturaUseCase(
                negocioService,
                loteSubidaService,
                confirmacionLecturaService,
                new ConfirmarLecturaMapper(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Guarda lo revisado del lote del negocio y devuelve dónde quedó")
    void executeShouldConfirmLoteOfNegocioAndReturnDestination() {
        LoteSubida lote = givenLoteOfNegocio();
        givenServiceConfirms();

        ConfirmarLecturaResponse result = confirmarLecturaUseCase.execute(request());

        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.registros()).isEqualTo(3);
        thenServiceReceivedConfirmation(lote);
    }

    @Test
    @DisplayName("Propaga el error del servicio sin convertirlo")
    void executeShouldPropagateServiceError() {
        givenLoteOfNegocio();
        when(confirmacionLecturaService.confirmar(any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("Este cuaderno ya se guardó"));
        ConfirmarLecturaRequest request = request();

        assertThatThrownBy(() -> confirmarLecturaUseCase.execute(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Este cuaderno ya se guardó");
    }

    @Test
    @DisplayName("No busca el lote cuando el usuario no tiene negocio")
    void executeShouldStopWhenUsuarioHasNoNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new BusinessRuleException("Sin negocio"));
        ConfirmarLecturaRequest request = request();

        assertThatThrownBy(() -> confirmarLecturaUseCase.execute(request))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(loteSubidaService, confirmacionLecturaService);
    }

    // --- arrange ---
    private LoteSubida givenLoteOfNegocio() {
        LoteSubida lote = LoteTestData.lote(List.of(LoteTestData.foto()));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(loteSubidaService.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(lote);
        return lote;
    }

    private void givenServiceConfirms() {
        when(confirmacionLecturaService.confirmar(any(), any(), any(), any()))
                .thenReturn(new ConfirmacionGuardada(PROYECTO_ID, BLOQUE_ID, 3));
    }

    // --- assert ---
    private void thenServiceReceivedConfirmation(LoteSubida lote) {
        ArgumentCaptor<ConfirmacionLectura> confirmacion = ArgumentCaptor.forClass(ConfirmacionLectura.class);
        verify(confirmacionLecturaService).confirmar(
                eq(NEGOCIO_ID),
                same(lote),
                confirmacion.capture(),
                eq(AHORA));
        assertThat(confirmacion.getValue().nombreBloque()).isEqualTo("Ventas");
        assertThat(confirmacion.getValue().campos()).hasSize(1);
        assertThat(confirmacion.getValue().filas()).hasSize(1);
    }

    // --- helpers ---
    private ConfirmarLecturaRequest request() {
        return new ConfirmarLecturaRequest(
                USUARIO_ID,
                LOTE_ID,
                null,
                "Ventas",
                null,
                "Ventas",
                List.of(new CampoItem("monto", null, "Monto", "NUMERO", List.of())),
                List.of(new FilaItem(1, Map.of("monto", 850))));
    }
}

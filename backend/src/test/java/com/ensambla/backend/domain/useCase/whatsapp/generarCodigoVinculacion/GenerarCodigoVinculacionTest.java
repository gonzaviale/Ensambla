package com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerarCodigoVinculacionTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final String NUMERO_BOT = "5491100000000";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CodigoVinculacionRepository codigoRepository = mock(CodigoVinculacionRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final GenerarCodigoVinculacion generarCodigoVinculacion = new WhatsappUseCaseConfig()
            .generarCodigoVinculacion(
                    negocioRepository,
                    codigoRepository,
                    mensajeria,
                    new TransaccionDirecta(),
                    Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("Generar descarta los códigos pendientes y guarda uno nuevo que vence a los 10 minutos")
    void executeShouldReplacePendingCodigo() {
        givenNegocioDelUsuario();
        when(codigoRepository.existsByCodigo(anyString())).thenReturn(false);
        when(codigoRepository.save(any(CodigoVinculacion.class))).thenAnswer(call -> call.getArgument(0));
        when(mensajeria.numeroBot()).thenReturn(NUMERO_BOT);

        GenerarCodigoVinculacionResponse result = generarCodigoVinculacion
                .execute(new GenerarCodigoVinculacionRequest(USUARIO_ID));

        verify(codigoRepository).deleteSinUsarByUsuarioId(USUARIO_ID);
        assertThat(result.codigo()).matches("[A-HJKMNP-Z2-9]{6}");
        assertThat(result.expiraEn()).isEqualTo(OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC).plusMinutes(10));
        assertThat(result.numeroBot()).isEqualTo(NUMERO_BOT);
        assertThat(thenCodigoGuardado().getUsuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Generar vuelve a sortear el código si ya lo tiene otro usuario")
    void executeShouldDrawAgainWhenCodigoExists() {
        givenNegocioDelUsuario();
        when(codigoRepository.existsByCodigo(anyString())).thenReturn(true, false);
        when(codigoRepository.save(any(CodigoVinculacion.class))).thenAnswer(call -> call.getArgument(0));

        generarCodigoVinculacion.execute(new GenerarCodigoVinculacionRequest(USUARIO_ID));

        verify(codigoRepository, times(2)).existsByCodigo(anyString());
    }

    @Test
    @DisplayName("Generar sin negocio responde como inexistente y no toca los códigos")
    void executeShouldFailWithoutNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("No se encontró el negocio del usuario"));

        assertThatThrownBy(() -> generarCodigoVinculacion.execute(new GenerarCodigoVinculacionRequest(USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(codigoRepository, never()).deleteSinUsarByUsuarioId(any());
    }

    private void givenNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private CodigoVinculacion thenCodigoGuardado() {
        ArgumentCaptor<CodigoVinculacion> captor = ArgumentCaptor.forClass(CodigoVinculacion.class);
        verify(codigoRepository).save(captor.capture());
        return captor.getValue();
    }
}

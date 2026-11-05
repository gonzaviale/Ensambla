package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp.ConfigurarAvisosPorWhatsapp;
import com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp.ConfigurarAvisosPorWhatsappRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurarAvisosPorWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-21T10:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    private final PreferenciaRepository preferenciaRepository = mock(PreferenciaRepository.class);
    private final VinculoWhatsappRepository vinculoWhatsappRepository = mock(VinculoWhatsappRepository.class);

    private ConfigurarAvisosPorWhatsapp configurarAvisosPorWhatsapp;

    @BeforeEach
    void setUp() {
        configurarAvisosPorWhatsapp = new ConfigurarAvisosPorWhatsapp(
                preferenciaRepository, vinculoWhatsappRepository, new TransaccionDirecta(),
                Clock.fixed(INSTANTE, ZoneOffset.UTC));
        when(preferenciaRepository.obtenerOInicial(USUARIO_ID, AHORA))
                .thenReturn(PreferenciaUsuario.inicial(USUARIO_ID, AHORA.minusDays(1)));
        when(preferenciaRepository.save(any(PreferenciaUsuario.class))).thenAnswer(guardada -> guardada.getArgument(0));
    }

    @Test
    @DisplayName("Prende el resumen cuando la persona tiene un número vinculado")
    void executeShouldTurnItOnWhenThereIsALinkedNumber() {
        givenUnNumeroVinculado();

        PreferenciasItem result = whenConfigura(true);

        assertThat(result.avisosPorWhatsapp()).isTrue();
        assertThat(preferenciaGuardada().isAvisosPorWhatsapp()).isTrue();
        assertThat(preferenciaGuardada().getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Rechaza prenderlo sin un número vinculado y no guarda nada")
    void executeShouldRejectTurningItOnWithoutALinkedNumber() {
        when(vinculoWhatsappRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> whenConfigura(true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vinculá tu número de WhatsApp");
        verify(preferenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Apagarlo no pide tener un número vinculado")
    void executeShouldTurnItOffWithoutCheckingTheLinkedNumber() {
        PreferenciasItem result = whenConfigura(false);

        assertThat(result.avisosPorWhatsapp()).isFalse();
        verify(vinculoWhatsappRepository, never()).findByUsuarioId(any());
    }

    // --- arrange ---
    private void givenUnNumeroVinculado() {
        when(vinculoWhatsappRepository.findByUsuarioId(USUARIO_ID))
                .thenReturn(Optional.of(VinculoWhatsapp.builder().usuarioId(USUARIO_ID).telefono("5491155550101").build()));
    }

    // --- act ---
    private PreferenciasItem whenConfigura(boolean activo) {
        return configurarAvisosPorWhatsapp.execute(new ConfigurarAvisosPorWhatsappRequest(USUARIO_ID, activo));
    }

    // --- assert ---
    private PreferenciaUsuario preferenciaGuardada() {
        ArgumentCaptor<PreferenciaUsuario> captor = ArgumentCaptor.forClass(PreferenciaUsuario.class);
        verify(preferenciaRepository).save(captor.capture());
        return captor.getValue();
    }
}

package com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesvincularWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final DesvincularWhatsapp desvincularWhatsapp = new WhatsappUseCaseConfig()
            .desvincularWhatsapp(vinculoRepository, new TransaccionDirecta());

    @Test
    @DisplayName("Desvincular borra el número del usuario")
    void executeShouldDeleteLink() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(VinculoWhatsapp.nuevo(
                USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp("5491122334455"), OffsetDateTime.now())));

        desvincularWhatsapp.execute(new DesvincularWhatsappRequest(USUARIO_ID));

        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
    }

    @Test
    @DisplayName("Desvincular sin número vinculado responde como inexistente y no borra nada")
    void executeShouldFailWithoutLink() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> desvincularWhatsapp.execute(new DesvincularWhatsappRequest(USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
    }
}

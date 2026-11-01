package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VinculacionWhatsappServiceTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final TelefonoWhatsapp TELEFONO = new TelefonoWhatsapp("5491122334455");

    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final VerificacionWhatsappRepository verificacionRepository = mock(VerificacionWhatsappRepository.class);
    private final VinculacionWhatsappService service =
            new VinculacionWhatsappService(vinculoRepository, verificacionRepository);

    @Test
    @DisplayName("Vincular reemplaza el número anterior, guarda el nuevo y descarta la verificación en curso")
    void vincularShouldReplacePreviousLink() {
        when(vinculoRepository.save(any(VinculoWhatsapp.class))).thenAnswer(i -> i.getArgument(0));

        VinculoWhatsapp vinculo = service.vincular(USUARIO_ID, NEGOCIO_ID, TELEFONO, AHORA);

        assertThat(vinculo.getTelefono()).isEqualTo("5491122334455");
        assertThat(vinculo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        InOrder orden = inOrder(vinculoRepository, verificacionRepository);
        orden.verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
        orden.verify(verificacionRepository).deleteByUsuarioId(USUARIO_ID);
        orden.verify(vinculoRepository).save(any(VinculoWhatsapp.class));
    }

    @Test
    @DisplayName("Un número que ya es de otra cuenta no se vincula ni toca el vínculo actual")
    void vincularShouldRejectNumberOfAnotherUser() {
        when(vinculoRepository.findByTelefono("5491122334455")).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(OTRO_USUARIO_ID, NEGOCIO_ID, TELEFONO, AHORA)));

        assertThatThrownBy(() -> service.vincular(USUARIO_ID, NEGOCIO_ID, TELEFONO, AHORA))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ese número de WhatsApp ya está vinculado a otra cuenta");

        verify(vinculoRepository, never()).deleteByUsuarioId(any());
        verify(vinculoRepository, never()).save(any());
    }
}

package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecepcionWhatsappServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final MensajeWhatsappRepository repository = mock(MensajeWhatsappRepository.class);
    private final RecepcionWhatsappService service = new RecepcionWhatsappService(repository);

    @Test
    @DisplayName("Guarda el mensaje en el negocio del vínculo")
    void registrarShouldSaveMessageInLinksNegocio() {
        when(repository.existsByIdExterno("ID1")).thenReturn(false);
        when(repository.save(any(MensajeWhatsapp.class))).thenAnswer(call -> call.getArgument(0));

        Optional<MensajeWhatsapp> result = service.registrar(givenVinculo(), givenEntrante());

        assertThat(result).get().extracting(MensajeWhatsapp::getNegocioId).isEqualTo(NEGOCIO_ID);
        assertThat(result.get().getTexto()).isEqualTo("gasté 5000 en harina");
    }

    @Test
    @DisplayName("Si el mensaje ya estaba guardado no lo guarda de nuevo")
    void registrarShouldSkipDuplicatedMessage() {
        when(repository.existsByIdExterno("ID1")).thenReturn(true);

        Optional<MensajeWhatsapp> result = service.registrar(givenVinculo(), givenEntrante());

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    private static VinculoWhatsapp givenVinculo() {
        return VinculoWhatsapp.builder().negocioId(NEGOCIO_ID).telefono("5491122334455").build();
    }

    private static MensajeEntrante givenEntrante() {
        return new MensajeEntrante("5491122334455", "ID1", TipoMensajeWhatsapp.TEXTO, "gasté 5000 en harina", null,
                null, AHORA);
    }
}

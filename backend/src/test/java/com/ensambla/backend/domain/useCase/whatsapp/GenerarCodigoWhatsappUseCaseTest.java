package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.GenerarCodigoWhatsappRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.whatsapp.GenerarCodigoWhatsappMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.VinculoWhatsappService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.USUARIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.vinculoConCodigo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenerarCodigoWhatsappUseCaseTest {

    private static final GenerarCodigoWhatsappRequest REQUEST = new GenerarCodigoWhatsappRequest(USUARIO_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final VinculoWhatsappService vinculoWhatsappService = mock(VinculoWhatsappService.class);

    private GenerarCodigoWhatsappUseCase generarCodigoWhatsappUseCase;

    @BeforeEach
    void setUp() {
        generarCodigoWhatsappUseCase = new GenerarCodigoWhatsappUseCase(
                negocioService,
                vinculoWhatsappService,
                new GenerarCodigoWhatsappMapper(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Genera el código para el negocio de la sesión con la hora actual")
    void executeShouldGenerateCodigoForSessionNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(vinculoWhatsappService.generarCodigo(NEGOCIO_ID, AHORA)).thenReturn(vinculoConCodigo(NEGOCIO_ID));

        CodigoWhatsappItem result = generarCodigoWhatsappUseCase.execute(REQUEST);

        assertThat(result.codigo()).isEqualTo(CODIGO);
        assertThat(result.venceEn()).isEqualTo(AHORA.plusMinutes(10));
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no genera nada")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> generarCodigoWhatsappUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(vinculoWhatsappService);
    }
}

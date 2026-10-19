package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.GenerarCodigoWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest.MensajeEntrante;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.MensajeWhatsappService;
import com.ensambla.backend.domain.service.whatsapp.VinculoWhatsappService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.RECIBIDO_EN;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.TEXTO;
import static com.ensambla.backend.support.WhatsappTestData.USUARIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.WA_MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.texto;
import static com.ensambla.backend.support.WhatsappTestData.vinculoConCodigo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsappUseCaseConfigTest {

    private final WhatsappUseCaseConfig config = new WhatsappUseCaseConfig();
    private final Clock clock = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    @Test
    @DisplayName("El wiring de generar el código conecta los servicios, el mapper y el reloj")
    void generarCodigoWhatsappUseCaseShouldWireCollaborators() {
        NegocioService negocioService = mock(NegocioService.class);
        VinculoWhatsappService vinculoWhatsappService = mock(VinculoWhatsappService.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(vinculoWhatsappService.generarCodigo(NEGOCIO_ID, AHORA)).thenReturn(vinculoConCodigo(NEGOCIO_ID));

        CodigoWhatsappItem result = config.generarCodigoWhatsappUseCase(
                        negocioService, vinculoWhatsappService, config.generarCodigoWhatsappMapper(), clock)
                .execute(new GenerarCodigoWhatsappRequest(USUARIO_ID));

        assertThat(result.codigo()).isEqualTo(CODIGO);
    }

    @Test
    @DisplayName("El wiring de recibir mensajes conecta el servicio, el mapper y el reloj")
    void recibirMensajesWhatsappUseCaseShouldWireCollaborators() {
        MensajeWhatsappService mensajeWhatsappService = mock(MensajeWhatsappService.class);

        config.recibirMensajesWhatsappUseCase(mensajeWhatsappService, config.recibirMensajesWhatsappMapper(), clock)
                .execute(new RecibirMensajesWhatsappRequest(List.of(
                        new MensajeEntrante(WA_MENSAJE_ID, TELEFONO, "TEXTO", TEXTO, null, RECIBIDO_EN))));

        verify(mensajeWhatsappService).recibir(texto(TEXTO), AHORA);
    }
}

package com.ensambla.backend.domain.useCase.procesamiento.leerImagen;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ConsumoIaInmediatoService;
import com.ensambla.backend.domain.service.procesamiento.LecturaImagenService;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenRequest.CampoItem;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeerImagenTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000002");
    private static final double UMBRAL_CONFIANZA = 0.7;

    private final LecturaImagenService lecturaImagenService = mock(LecturaImagenService.class);
    private final NegocioService negocioService = mock(NegocioService.class);
    private final ConsumoIaInmediatoService consumoIaInmediatoService = mock(ConsumoIaInmediatoService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-01T12:00:00Z"), ZoneOffset.UTC);
    private final LeerImagen useCase = new LeerImagen(
            lecturaImagenService, UMBRAL_CONFIANZA, negocioService, consumoIaInmediatoService, clock);

    private final LeerImagenRequest request = new LeerImagenRequest(USUARIO_ID, ImagenTestData.jpeg(), "Ventas",
            List.of(new CampoItem("importe", "Monto", "NUMERO", null)), true);

    @Test
    @DisplayName("Lee la imagen contra el bloque pedido y devuelve lo leído")
    void executeShouldReadTheImageAgainstTheRequestedBlock() {
        givenLectura(new LecturaImagen(List.of(), List.of(), List.of("Renglón ilegible")));

        LeerImagenItem item = useCase.execute(request);

        assertThat(item).isEqualTo(new LeerImagenItem(List.of(), List.of(), List.of("Renglón ilegible")));
        thenLeyoContraElBloquePedido();
    }

    @Test
    @DisplayName("Registra el consumo del negocio del usuario antes de leer")
    void executeShouldRegisterTheBusinessConsumptionBeforeReading() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        givenLectura(new LecturaImagen(List.of(), List.of(), List.of()));

        useCase.execute(request);

        InOrder orden = inOrder(consumoIaInmediatoService, lecturaImagenService);
        orden.verify(consumoIaInmediatoService).registrar(NEGOCIO_ID, OffsetDateTime.now(clock));
        orden.verify(lecturaImagenService).leer(any(ImagenCuaderno.class), any(BloqueDeImagen.class), eq(true));
    }

    @Test
    @DisplayName("Con una imagen inválida no llega a leer")
    void executeShouldNotReadAnInvalidImage() {
        LeerImagenRequest vacia = new LeerImagenRequest(USUARIO_ID, new byte[0], "Ventas", request.campos(), true);

        assertThatThrownBy(() -> useCase.execute(vacia))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La imagen está vacía");

        verifyNoInteractions(lecturaImagenService, consumoIaInmediatoService);
    }

    @Test
    @DisplayName("Con un bloque sin datos no llega a leer")
    void executeShouldNotReadABlockWithoutFields() {
        LeerImagenRequest sinCampos = new LeerImagenRequest(USUARIO_ID, request.contenido(), "Ventas", List.of(), true);

        assertThatThrownBy(() -> useCase.execute(sinCampos)).isInstanceOf(BusinessRuleException.class);

        verify(lecturaImagenService, never()).leer(any(), any(), anyBoolean());
        verifyNoInteractions(consumoIaInmediatoService);
    }

    // --- arrange ---
    private void givenLectura(LecturaImagen lectura) {
        when(lecturaImagenService.leer(any(ImagenCuaderno.class), any(BloqueDeImagen.class), eq(true)))
                .thenReturn(lectura);
    }

    // --- assert ---
    private void thenLeyoContraElBloquePedido() {
        ArgumentCaptor<ImagenCuaderno> imagen = ArgumentCaptor.forClass(ImagenCuaderno.class);
        ArgumentCaptor<BloqueDeImagen> bloque = ArgumentCaptor.forClass(BloqueDeImagen.class);
        verify(lecturaImagenService).leer(imagen.capture(), bloque.capture(), eq(true));
        assertThat(imagen.getValue().contenido()).isSameAs(request.contenido());
        assertThat(bloque.getValue().nombre()).isEqualTo("Ventas");
        assertThat(bloque.getValue().campos()).singleElement().satisfies(campo -> {
            assertThat(campo.clave()).isEqualTo("importe");
            assertThat(campo.tipo()).isEqualTo(TipoCampo.NUMERO);
        });
    }
}

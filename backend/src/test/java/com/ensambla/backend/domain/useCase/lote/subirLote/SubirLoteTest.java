package com.ensambla.backend.domain.useCase.lote.subirLote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubirLoteTest {

    private static final List<byte[]> FOTOS = List.of(JPEG);
    private static final SubirLoteRequest REQUEST = new SubirLoteRequest(USUARIO_ID, FOTOS, null);
    private static final SubirLoteRequest REQUEST_CON_DESTINO = new SubirLoteRequest(USUARIO_ID, FOTOS, BLOQUE_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final LoteSubidaService loteSubidaService = mock(LoteSubidaService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);

    private SubirLote subirLote;

    @BeforeEach
    void setUp() {
        subirLote = new SubirLote(
                negocioService,
                loteSubidaService,
                bloqueService,
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Sube las fotos al negocio de la sesión con la hora actual y devuelve el lote creado")
    void executeShouldUploadToSessionNegocio() {
        givenNegocio();
        givenLoteSubido();

        LoteItem result = subirLote.execute(REQUEST);

        assertThat(result.id()).isEqualTo(LOTE_ID);
        assertThat(result.estado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Con un bloque destino del negocio lo valida y lo guarda en el lote")
    void executeShouldPassValidatedBloqueDestino() {
        givenNegocio();
        when(loteSubidaService.subir(NEGOCIO_ID, FOTOS, BLOQUE_ID, AHORA))
                .thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));

        LoteItem result = subirLote.execute(REQUEST_CON_DESTINO);

        assertThat(result.id()).isEqualTo(LOTE_ID);
        verify(bloqueService).obtener(BLOQUE_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un bloque destino de otro negocio responde como inexistente y no sube nada")
    void executeShouldRejectBloqueDestinoOfOtherNegocio() {
        givenNegocio();
        givenBloqueAjeno();

        assertThatThrownBy(() -> subirLote.execute(REQUEST_CON_DESTINO))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(loteSubidaService);
    }

    @Test
    @DisplayName("Sin bloque destino no consulta bloques")
    void executeShouldNotLookUpBloqueWhenNoDestino() {
        givenNegocio();
        givenLoteSubido();

        subirLote.execute(REQUEST);

        verifyNoInteractions(bloqueService);
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no sube nada")
    void executeShouldStopWhenNegocioMissing() {
        givenNoNegocio();

        assertThatThrownBy(() -> subirLote.execute(REQUEST)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(loteSubidaService);
    }

    @Test
    @DisplayName("Propaga sin cambios el rechazo de una foto inválida")
    void executeShouldPropagateBusinessRule() {
        givenNegocio();
        givenFotoInvalida();

        assertThatThrownBy(() -> subirLote.execute(REQUEST))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La foto 1 está vacía");
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void givenNoNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    private void givenBloqueAjeno() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("Bloque no encontrado"));
    }

    private void givenLoteSubido() {
        when(loteSubidaService.subir(NEGOCIO_ID, FOTOS, null, AHORA))
                .thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));
    }

    private void givenFotoInvalida() {
        when(loteSubidaService.subir(NEGOCIO_ID, FOTOS, null, AHORA))
                .thenThrow(new BusinessRuleException("La foto 1 está vacía"));
    }
}

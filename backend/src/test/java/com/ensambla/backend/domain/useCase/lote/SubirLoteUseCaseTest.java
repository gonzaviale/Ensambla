package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.SubirLoteRequest;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.lote.LoteItemMapper;
import com.ensambla.backend.domain.mapper.lote.SubirLoteMapper;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubirLoteUseCaseTest {

    private static final List<byte[]> FOTOS = List.of(JPEG);
    private static final SubirLoteRequest REQUEST = new SubirLoteRequest(USUARIO_ID, FOTOS);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final LoteSubidaService loteSubidaService = mock(LoteSubidaService.class);

    private SubirLoteUseCase subirLoteUseCase;

    @BeforeEach
    void setUp() {
        subirLoteUseCase = new SubirLoteUseCase(
                negocioService,
                loteSubidaService,
                new SubirLoteMapper(new LoteItemMapper()),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Sube las fotos al negocio de la sesión con la hora actual y devuelve el lote creado")
    void executeShouldUploadToSessionNegocio() {
        givenNegocio();
        givenLoteSubido();

        LoteItem result = subirLoteUseCase.execute(REQUEST);

        assertThat(result.id()).isEqualTo(LOTE_ID);
        assertThat(result.estado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no sube nada")
    void executeShouldStopWhenNegocioMissing() {
        givenNoNegocio();

        assertThatThrownBy(() -> subirLoteUseCase.execute(REQUEST)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(loteSubidaService);
    }

    @Test
    @DisplayName("Propaga sin cambios el rechazo de una foto inválida")
    void executeShouldPropagateBusinessRule() {
        givenNegocio();
        givenFotoInvalida();

        assertThatThrownBy(() -> subirLoteUseCase.execute(REQUEST))
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

    private void givenLoteSubido() {
        when(loteSubidaService.subir(NEGOCIO_ID, FOTOS, AHORA))
                .thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));
    }

    private void givenFotoInvalida() {
        when(loteSubidaService.subir(NEGOCIO_ID, FOTOS, AHORA))
                .thenThrow(new BusinessRuleException("La foto 1 está vacía"));
    }
}

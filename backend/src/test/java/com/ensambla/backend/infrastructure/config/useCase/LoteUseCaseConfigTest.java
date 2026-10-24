package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLoteRequest;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLoteRequest;
import com.ensambla.backend.support.LoteTestData;
import com.ensambla.backend.support.TransaccionDirecta;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoteUseCaseConfigTest {

    private static final List<byte[]> FOTOS = List.of(JPEG);

    private final LoteUseCaseConfig config = new LoteUseCaseConfig();
    private final NegocioService negocioService = mock(NegocioService.class);
    private final LoteSubidaService loteSubidaService = mock(LoteSubidaService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);

    @Test
    @DisplayName("El wiring de la subida conecta negocio, repositorio, almacenamiento y reloj")
    void subirLoteUseCaseShouldWireDependencies() {
        givenNegocio();
        when(loteSubidaRepository.saveConFotos(any())).thenReturn(LoteTestData.lote(List.of()));

        LoteItem result = config.subirLote(negocioService, loteSubidaRepository, mock(ArchivoCuadernoPort.class),
                mock(ControlPlanService.class), new TransaccionDirecta(), bloqueService,
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC))
                .execute(new SubirLoteRequest(USUARIO_ID, FOTOS, null));

        assertThat(result.id()).isEqualTo(LOTE_ID);
    }

    @Test
    @DisplayName("El wiring de la consulta conecta negocio y servicio")
    void obtenerLoteUseCaseShouldWireDependencies() {
        givenNegocio();
        when(loteSubidaService.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(LoteTestData.lote(List.of()));

        LoteItem result = config.obtenerLote(negocioService, loteSubidaService)
                .execute(new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));

        assertThat(result.id()).isEqualTo(LOTE_ID);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }
}

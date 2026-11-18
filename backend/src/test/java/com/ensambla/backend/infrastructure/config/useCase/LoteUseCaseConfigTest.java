package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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

    private final LoteUseCaseConfig config = new LoteUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private LoteItem lote;

    @Test
    @DisplayName("El wiring de la subida conecta negocio, repositorio, almacenamiento y reloj")
    void elWiringDeLaSubidaConectaSusDependencias() {
        dadoUnNegocio();
        dadoQueGuardarDevuelveElLote();

        cuandoSeSubeUnaFoto();

        entoncesSeObtuvoElLote();
    }

    @Test
    @DisplayName("El wiring de la consulta conecta negocio y servicio")
    void elWiringDeLaConsultaConectaNegocioYServicio() {
        dadoUnNegocio();
        dadoQueElLoteExiste();

        cuandoSeConsultaElLote();

        entoncesSeObtuvoElLote();
    }

    private void dadoUnNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void dadoQueGuardarDevuelveElLote() {
        when(loteSubidaRepository.saveConFotos(any())).thenReturn(LoteTestData.lote(List.of()));
    }

    private void dadoQueElLoteExiste() {
        when(loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(LoteTestData.lote(List.of()));
    }

    private void cuandoSeSubeUnaFoto() {
        lote = config.subirLote(negocioRepository, loteSubidaRepository, mock(ArchivoCuadernoPort.class),
                        mock(ControlPlanService.class), new TransaccionDirecta(), bloqueRepository,
                        Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC))
                .execute(new SubirLoteRequest(USUARIO_ID, List.of(JPEG), null));
    }

    private void cuandoSeConsultaElLote() {
        lote = config.obtenerLote(negocioRepository, loteSubidaRepository).execute(new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));
    }

    private void entoncesSeObtuvoElLote() {
        assertThat(lote.id()).isEqualTo(LOTE_ID);
    }
}
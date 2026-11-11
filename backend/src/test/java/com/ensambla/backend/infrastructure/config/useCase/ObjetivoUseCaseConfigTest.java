package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivoRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjetivoUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    private final ObjetivoUseCaseConfig config = new ObjetivoUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);

    @Test
    @DisplayName("El wiring de la propuesta carga los sistemas del negocio y no encuentra candidatos sin datos")
    void propuestaMiniObjetivosServiceShouldWireTheBusinessSources() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());

        CandidatosDelNegocio result = propuesta().candidatos(NEGOCIO_ID, LocalDate.of(2026, 11, 10));

        assertThat(result.candidatos()).isEmpty();
    }

    @Test
    @DisplayName("El wiring de listar mini-objetivos conecta los repositorios")
    void listarMiniObjetivosShouldWireTheRepositories() {
        givenObjetivoYMiniObjetivo();
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));

        ListarMiniObjetivosResponse result = config
                .listarMiniObjetivos(negocioRepository, objetivoRepository, miniObjetivoRepository)
                .execute(new ListarMiniObjetivosRequest(USUARIO_ID, OBJETIVO_ID));

        assertThat(result.minis()).hasSize(1);
    }

    @Test
    @DisplayName("El wiring de listar candidatos conecta la propuesta y el reloj")
    void listarCandidatosShouldWireTheProposal() {
        givenObjetivoYMiniObjetivo();
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());

        ListarCandidatosResponse result = config
                .listarCandidatos(negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(), CLOCK)
                .execute(new ListarCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("El wiring de reemplazar conecta la propuesta: sin candidatos el elegido no está disponible")
    void reemplazarMiniObjetivoShouldWireTheProposal() {
        givenObjetivoYMiniObjetivo();
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
        ReemplazarMiniObjetivo reemplazar = config.reemplazarMiniObjetivo(
                negocioRepository, objetivoRepository, miniObjetivoRepository, propuesta(),
                new TransaccionDirecta(), CLOCK);

        assertThatThrownBy(() -> reemplazar.execute(new ReemplazarMiniObjetivoRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, TipoMiniObjetivo.MEJORAR_MARGEN, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("El wiring de abandonar conecta los repositorios y guarda el mini-objetivo quitado")
    void abandonarMiniObjetivoShouldWireTheRepositories() {
        givenObjetivoYMiniObjetivo();

        config.abandonarMiniObjetivo(
                        negocioRepository, objetivoRepository, miniObjetivoRepository, new TransaccionDirecta(), CLOCK)
                .execute(new AbandonarMiniObjetivoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));

        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
    }

    @Test
    @DisplayName("El wiring de pasos conecta las fuentes reales del negocio")
    void obtenerPasosShouldWireBusinessSources() {
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        SistemaRepository sistemaRepository = mock(SistemaRepository.class);
        DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
        ZonaRepository zonaRepository = mock(ZonaRepository.class);
        AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());

        ObtenerPasosResponse response = config.obtenerPasos(
                        negocioRepository,
                        sistemaRepository,
                        datosDeProyectoService,
                        zonaRepository,
                        agregadoZonaRepository,
                        new MetricasVentas(30, new BigDecimal("0.5")),
                        Clock.systemUTC())
                .execute(new ObtenerPasosRequest(USUARIO_ID));

        assertThat(response.content()).isEmpty();
    }

    // --- helpers ---
    private void givenObjetivoYMiniObjetivo() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    private PropuestaMiniObjetivosService propuesta() {
        return config.propuestaMiniObjetivosService(
                negocioRepository,
                sistemaRepository,
                mock(DatosDeProyectoService.class),
                mock(ZonaRepository.class),
                mock(AgregadoZonaRepository.class),
                new MetricasVentas(30, new BigDecimal("0.5")));
    }
}

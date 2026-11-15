package com.ensambla.backend.domain.useCase.objetivo.crearObjetivo;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.TextoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.infrastructure.config.useCase.ObjetivoUseCaseConfig;
import com.ensambla.backend.infrastructure.ia.ObjetivosIaProperties;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearObjetivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final int LIMITE_DIARIO = 10;
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("simulado", "modelo", 100, 50);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final RedaccionObjetivosIaPort redaccionObjetivosIaPort = mock(RedaccionObjetivosIaPort.class);

    @BeforeEach
    void givenNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        givenRepositorioAsignaId();
    }

    @Test
    @DisplayName("Guarda el objetivo en el negocio de la sesión con su texto, su meta y la fecha de hoy")
    void executeShouldSaveObjetivoOfSessionNegocio() {
        givenNegocioSinDatos();

        whenCrear(false, " " + TEXTO + " ");

        Objetivo guardado = thenObjetivoGuardado();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getTexto()).isEqualTo(TEXTO);
        assertThat(guardado.getMeta()).isEqualTo(META);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(guardado.estaAlcanzado()).isFalse();
    }

    @Test
    @DisplayName("Responde con el objetivo guardado, ya con su id")
    void executeShouldReturnSavedObjetivo() {
        givenNegocioSinDatos();

        ObjetivoItem result = whenCrear(false, TEXTO);

        assertThat(result.id()).isEqualTo(OBJETIVO_ID);
        assertThat(result.texto()).isEqualTo(TEXTO);
        assertThat(result.meta()).isEqualTo(META);
        assertThat(result.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Sin datos con qué proponer guarda el objetivo sin mini-objetivos y no inventa ninguno")
    void executeShouldSaveObjetivoWithoutMiniObjetivosWhenThereIsNothingToPropose() {
        givenNegocioSinDatos();

        whenCrear(false, TEXTO);

        verifyNoInteractions(miniObjetivoRepository);
    }

    @Test
    @DisplayName("Propone los 3 mini-objetivos de mayor aporte, uno por lugar, en la semana de hoy")
    void executeShouldSaveTheThreeBestMiniObjetivos() {
        givenCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"),
                candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "5000"),
                candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "4000"));

        whenCrear(false, TEXTO);

        List<MiniObjetivo> guardados = thenMiniObjetivosGuardados(3);
        assertThat(guardados).extracting(MiniObjetivo::getPosicion).containsExactly(1, 2, 3);
        assertThat(guardados).extracting(MiniObjetivo::getTipo).containsExactly(
                TipoMiniObjetivo.SUBIR_PRECIO, TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.VENTAS_SEMANALES);
        assertThat(guardados).allSatisfy(mini -> {
            assertThat(mini.getObjetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(mini.getNegocioId()).isEqualTo(NEGOCIO_ID);
            assertThat(mini.getSemanaInicio()).isEqualTo(SEMANA);
            assertThat(mini.getCreadoEn()).isEqualTo(AHORA);
            assertThat(mini.isTextoConIa()).isFalse();
        });
    }

    @Test
    @DisplayName("Si el resultado de hoy ya llega a la meta, el objetivo nace alcanzado y sin mini-objetivos")
    void executeShouldSaveAReachedObjetivoWithoutMiniObjetivos() {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(META.add(BigDecimal.ONE), List.of(candidato())));

        whenCrear(false, TEXTO);

        Objetivo guardado = thenObjetivoGuardado();
        assertThat(guardado.estaAlcanzado()).isTrue();
        assertThat(guardado.getAlcanzadoEn()).isEqualTo(AHORA);
        verifyNoInteractions(miniObjetivoRepository);
    }

    @Test
    @DisplayName("Con la IA apagada no la consulta ni gasta el cupo")
    void executeShouldNotConsultTheAiWhenItIsOff() {
        givenCandidatos(candidato());

        whenCrear(false, TEXTO);

        verifyNoInteractions(controlPlanService, consumoSamiRepository, redaccionObjetivosIaPort);
    }

    @Test
    @DisplayName("Con la IA prendida pero sin mini-objetivos que redactar no la consulta")
    void executeShouldNotConsultTheAiWhenThereIsNothingToWrite() {
        givenNegocioSinDatos();

        whenCrear(true, TEXTO);

        verifyNoInteractions(controlPlanService, consumoSamiRepository, redaccionObjetivosIaPort);
    }

    @Test
    @DisplayName("Con la IA prendida reserva el cupo, la consulta, registra lo gastado y guarda sus textos")
    void executeShouldSaveTheTextsWrittenByTheAi() {
        givenCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
        givenCupoDisponible();
        when(redaccionObjetivosIaPort.redactar(any())).thenReturn(new RedaccionObjetivosIa(
                List.of(new TextoMiniObjetivo("Título 1", "Razón 1"), new TextoMiniObjetivo("Título 2", "Razón 2")),
                CONSUMO));

        whenCrear(true, TEXTO);

        verify(controlPlanService).exigirIa(NEGOCIO_ID);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
        List<MiniObjetivo> guardados = thenMiniObjetivosGuardados(2);
        assertThat(guardados).extracting(MiniObjetivo::getTitulo).containsExactly("Título 1", "Título 2");
        assertThat(guardados).extracting(MiniObjetivo::getJustificacion).containsExactly("Razón 1", "Razón 2");
        assertThat(guardados).allSatisfy(mini -> assertThat(mini.isTextoConIa()).isTrue());
    }

    @Test
    @DisplayName("A la IA le pasa los candidatos elegidos y ningún otro")
    void executeShouldPassOnlyTheChosenCandidatesToTheAi() {
        givenCandidatos(candidato());
        givenCupoDisponible();
        when(redaccionObjetivosIaPort.redactar(any())).thenReturn(new RedaccionObjetivosIa(
                List.of(new TextoMiniObjetivo("Título", "Razón")), CONSUMO));

        whenCrear(true, TEXTO);

        ArgumentCaptor<List<CandidatoMiniObjetivo>> captor = ArgumentCaptor.captor();
        verify(redaccionObjetivosIaPort).redactar(captor.capture());
        assertThat(captor.getValue()).containsExactly(candidato());
    }

    @Test
    @DisplayName("Sin cupo diario guarda el texto de reserva y no consulta a la IA")
    void executeShouldUseTheReserveTextWhenTheDailyLimitIsReached() {
        givenCandidatos(candidato());
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(false);

        whenCrear(true, TEXTO);

        verifyNoInteractions(redaccionObjetivosIaPort);
        assertThat(thenMiniObjetivosGuardados(1)).singleElement().satisfies(mini -> {
            assertThat(mini.getTitulo()).isEqualTo(candidato().titulo());
            assertThat(mini.isTextoConIa()).isFalse();
        });
    }

    @Test
    @DisplayName("Si el plan no incluye la IA guarda el texto de reserva sin gastar cupo")
    void executeShouldUseTheReserveTextWhenThePlanHasNoAi() {
        givenCandidatos(candidato());
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "Sin IA")).when(controlPlanService)
                .exigirIa(NEGOCIO_ID);

        whenCrear(true, TEXTO);

        verifyNoInteractions(consumoSamiRepository, redaccionObjetivosIaPort);
        assertThat(thenMiniObjetivosGuardados(1)).singleElement()
                .satisfies(mini -> assertThat(mini.isTextoConIa()).isFalse());
    }

    @Test
    @DisplayName("Si la IA falla guarda el texto de reserva y no registra consumo")
    void executeShouldUseTheReserveTextWhenTheAiFails() {
        givenCandidatos(candidato());
        givenCupoDisponible();
        when(redaccionObjetivosIaPort.redactar(any())).thenThrow(new IllegalStateException("IA caída"));

        whenCrear(true, TEXTO);

        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
        assertThat(thenMiniObjetivosGuardados(1)).singleElement().satisfies(mini -> {
            assertThat(mini.getTitulo()).isEqualTo(candidato().titulo());
            assertThat(mini.isTextoConIa()).isFalse();
        });
    }

    @Test
    @DisplayName("Si la IA devuelve menos textos que mini-objetivos guarda el texto de reserva de todos")
    void executeShouldUseTheReserveTextWhenTheAiReturnsFewerTexts() {
        givenCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
        givenCupoDisponible();
        when(redaccionObjetivosIaPort.redactar(any())).thenReturn(new RedaccionObjetivosIa(
                List.of(new TextoMiniObjetivo("Título", "Razón")), CONSUMO));

        whenCrear(true, TEXTO);

        assertThat(thenMiniObjetivosGuardados(2)).allSatisfy(mini -> assertThat(mini.isTextoConIa()).isFalse());
    }

    @Test
    @DisplayName("Propaga el error del repositorio sin convertirlo")
    void executeShouldPropagateRepositoryError() {
        givenNegocioSinDatos();
        doThrow(new IllegalStateException("base caída")).when(objetivoRepository).save(any());

        assertThatThrownBy(() -> whenCrear(false, TEXTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("base caída");
    }

    // --- arrange ---
    private void givenRepositorioAsignaId() {
        when(objetivoRepository.save(any())).thenAnswer(invocation -> {
            Objetivo objetivo = invocation.getArgument(0);
            objetivo.setId(OBJETIVO_ID);
            return objetivo;
        });
        when(miniObjetivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenNegocioSinDatos() {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of()));
    }

    private void givenCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of(candidatos)));
    }

    private void givenCupoDisponible() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(true);
    }

    // --- act ---
    private ObjetivoItem whenCrear(boolean iaHabilitada, String texto) {
        CrearObjetivo crearObjetivo = new ObjetivoUseCaseConfig().crearObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                controlPlanService,
                consumoSamiRepository,
                redaccionObjetivosIaPort,
                new TransaccionDirecta(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC),
                new ObjetivosIaProperties(iaHabilitada, LIMITE_DIARIO));
        return crearObjetivo.execute(new CrearObjetivoRequest(USUARIO_ID, texto, META));
    }

    // --- assert ---
    private Objetivo thenObjetivoGuardado() {
        ArgumentCaptor<Objetivo> captor = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(captor.capture());
        return captor.getValue();
    }

    private List<MiniObjetivo> thenMiniObjetivosGuardados(int cantidad) {
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository, times(cantidad)).save(captor.capture());
        return captor.getAllValues();
    }
}

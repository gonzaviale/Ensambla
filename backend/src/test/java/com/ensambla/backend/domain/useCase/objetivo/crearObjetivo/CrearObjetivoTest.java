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
import static org.assertj.core.api.Assertions.catchThrowable;
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
    private ObjetivoItem respuesta;
    private Throwable error;

    @BeforeEach
    void dadoNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        dadoRepositorioAsignaId();
    }

    @Test
    @DisplayName("Guarda el objetivo en el negocio de la sesión con su texto, su meta y la fecha de hoy")
    void guardaElObjetivoEnElNegocioDeLaSesionConSuTextoSuMetaYLaFechaDeHoy() {
        dadoNegocioSinDatos();

        cuandoCrear(false, " " + TEXTO + " ");

        entoncesSeGuardaElObjetivoDelNegocioDeLaSesion();
    }

    @Test
    @DisplayName("Responde con el objetivo guardado, ya con su id")
    void respondeConElObjetivoGuardadoYaConSuId() {
        dadoNegocioSinDatos();

        cuandoCrear(false, TEXTO);

        entoncesSeRespondeConElObjetivoGuardado();
    }

    @Test
    @DisplayName("Sin datos con qué proponer guarda el objetivo sin mini-objetivos y no inventa ninguno")
    void sinDatosConQueProponerGuardaElObjetivoSinMiniObjetivosYNoInventaNinguno() {
        dadoNegocioSinDatos();

        cuandoCrear(false, TEXTO);

        entoncesNoSeGuardaNingunMiniObjetivo();
    }

    @Test
    @DisplayName("Propone los 3 mini-objetivos de mayor aporte, uno por lugar, en la semana de hoy")
    void proponeLos3MiniObjetivosDeMayorAporteUnoPorLugarEnLaSemanaDeHoy() {
        dadoCuatroCandidatos();

        cuandoCrear(false, TEXTO);

        entoncesSeGuardanLosTresDeMayorAporteUnoPorLugar();
    }

    @Test
    @DisplayName("Si el resultado de hoy ya llega a la meta, el objetivo nace alcanzado y sin mini-objetivos")
    void siElResultadoDeHoyYaLlegaALaMetaElObjetivoNaceAlcanzadoYSinMiniObjetivos() {
        dadoUnResultadoQueYaLlegaALaMeta();

        cuandoCrear(false, TEXTO);

        entoncesElObjetivoNaceAlcanzado();
        entoncesNoSeGuardaNingunMiniObjetivo();
    }

    @Test
    @DisplayName("Con la IA apagada no la consulta ni gasta el cupo")
    void conLaIaApagadaNoLaConsultaNiGastaElCupo() {
        dadoCandidatos(candidato());

        cuandoCrear(false, TEXTO);

        entoncesNoSeConsultaLaIaNiSeGastaElCupo();
    }

    @Test
    @DisplayName("Con la IA prendida pero sin mini-objetivos que redactar no la consulta")
    void conLaIaPrendidaPeroSinMiniObjetivosQueRedactarNoLaConsulta() {
        dadoNegocioSinDatos();

        cuandoCrear(true, TEXTO);

        entoncesNoSeConsultaLaIaNiSeGastaElCupo();
    }

    @Test
    @DisplayName("Con la IA prendida reserva el cupo, la consulta, registra lo gastado y guarda sus textos")
    void conLaIaPrendidaReservaElCupoLaConsultaRegistraLoGastadoYGuardaSusTextos() {
        dadoDosCandidatos();
        dadoCupoDisponible();
        dadoLaIaRedactaDosTextos();

        cuandoCrear(true, TEXTO);

        entoncesSeReservaElCupoYSeRegistraLoGastado();
        entoncesSeGuardanLosTextosDeLaIa();
    }

    @Test
    @DisplayName("A la IA le pasa los candidatos elegidos y ningún otro")
    void aLaIaLePasaLosCandidatosElegidosYNingunOtro() {
        dadoCandidatos(candidato());
        dadoCupoDisponible();
        dadoLaIaRedactaUnTexto();

        cuandoCrear(true, TEXTO);

        entoncesLaIaRecibeSoloLosCandidatosElegidos();
    }

    @Test
    @DisplayName("Sin cupo diario guarda el texto de reserva y no consulta a la IA")
    void sinCupoDiarioGuardaElTextoDeReservaYNoConsultaALaIa() {
        dadoCandidatos(candidato());
        dadoSinCupoDiario();

        cuandoCrear(true, TEXTO);

        entoncesNoSeConsultaLaIa();
        entoncesSeGuardaUnMiniConElTextoDeReserva();
    }

    @Test
    @DisplayName("Si el plan no incluye la IA guarda el texto de reserva sin gastar cupo")
    void siElPlanNoIncluyeLaIaGuardaElTextoDeReservaSinGastarCupo() {
        dadoCandidatos(candidato());
        dadoUnPlanSinIa();

        cuandoCrear(true, TEXTO);

        entoncesNoSeGastaCupoNiSeConsultaLaIa();
        entoncesSeGuardaUnMiniSinTextoDeIa();
    }

    @Test
    @DisplayName("Si la IA falla guarda el texto de reserva y no registra consumo")
    void siLaIaFallaGuardaElTextoDeReservaYNoRegistraConsumo() {
        dadoCandidatos(candidato());
        dadoCupoDisponible();
        dadoUnaIaCaida();

        cuandoCrear(true, TEXTO);

        entoncesNoSeRegistraConsumo();
        entoncesSeGuardaUnMiniConElTextoDeReserva();
    }

    @Test
    @DisplayName("Si la IA devuelve menos textos que mini-objetivos guarda el texto de reserva de todos")
    void siLaIaDevuelveMenosTextosQueMiniObjetivosGuardaElTextoDeReservaDeTodos() {
        dadoDosCandidatos();
        dadoCupoDisponible();
        dadoLaIaRedactaUnTexto();

        cuandoCrear(true, TEXTO);

        entoncesSeGuardanDosMinisSinTextoDeIa();
    }

    @Test
    @DisplayName("Propaga el error del repositorio sin convertirlo")
    void propagaElErrorDelRepositorioSinConvertirlo() {
        dadoNegocioSinDatos();
        dadoUnRepositorioQueFalla();

        cuandoCrearConError(false, TEXTO);

        entoncesSePropagaElErrorDeLaBase();
    }

    // --- arrange ---
    private void dadoRepositorioAsignaId() {
        when(objetivoRepository.save(any())).thenAnswer(invocation -> {
            Objetivo objetivo = invocation.getArgument(0);
            objetivo.setId(OBJETIVO_ID);
            return objetivo;
        });
        when(miniObjetivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoNegocioSinDatos() {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of()));
    }

    private void dadoCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of(candidatos)));
    }

    private void dadoDosCandidatos() {
        dadoCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
    }

    private void dadoCuatroCandidatos() {
        dadoCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"),
                candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "5000"),
                candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "4000"));
    }

    private void dadoUnResultadoQueYaLlegaALaMeta() {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(META.add(BigDecimal.ONE), List.of(candidato())));
    }

    private void dadoCupoDisponible() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(true);
    }

    private void dadoSinCupoDiario() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(false);
    }

    private void dadoLaIaRedactaDosTextos() {
        dadoLaIaRedacta(new TextoMiniObjetivo("Título 1", "Razón 1"), new TextoMiniObjetivo("Título 2", "Razón 2"));
    }

    private void dadoLaIaRedactaUnTexto() {
        dadoLaIaRedacta(new TextoMiniObjetivo("Título", "Razón"));
    }

    private void dadoLaIaRedacta(TextoMiniObjetivo... textos) {
        when(redaccionObjetivosIaPort.redactar(any())).thenReturn(new RedaccionObjetivosIa(List.of(textos), CONSUMO));
    }

    private void dadoUnPlanSinIa() {
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "Sin IA")).when(controlPlanService)
                .exigirIa(NEGOCIO_ID);
    }

    private void dadoUnaIaCaida() {
        when(redaccionObjetivosIaPort.redactar(any())).thenThrow(new IllegalStateException("IA caída"));
    }

    private void dadoUnRepositorioQueFalla() {
        doThrow(new IllegalStateException("base caída")).when(objetivoRepository).save(any());
    }

    // --- act ---
    private void cuandoCrear(boolean iaHabilitada, String texto) {
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
                new ObjetivosIaProperties(iaHabilitada, LIMITE_DIARIO, true, 6));
        respuesta = crearObjetivo.execute(new CrearObjetivoRequest(USUARIO_ID, texto, META));
    }

    private void cuandoCrearConError(boolean iaHabilitada, String texto) {
        error = catchThrowable(() -> cuandoCrear(iaHabilitada, texto));
    }

    // --- assert ---
    private void entoncesSeGuardaElObjetivoDelNegocioDeLaSesion() {
        Objetivo guardado = objetivoGuardado();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getTexto()).isEqualTo(TEXTO);
        assertThat(guardado.getMeta()).isEqualTo(META);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(guardado.estaAlcanzado()).isFalse();
    }

    private void entoncesSeRespondeConElObjetivoGuardado() {
        assertThat(respuesta.id()).isEqualTo(OBJETIVO_ID);
        assertThat(respuesta.texto()).isEqualTo(TEXTO);
        assertThat(respuesta.meta()).isEqualTo(META);
        assertThat(respuesta.creadoEn()).isEqualTo(AHORA);
    }

    private void entoncesNoSeGuardaNingunMiniObjetivo() {
        verifyNoInteractions(miniObjetivoRepository);
    }

    private void entoncesSeGuardanLosTresDeMayorAporteUnoPorLugar() {
        List<MiniObjetivo> guardados = minisGuardados(3);
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

    private void entoncesElObjetivoNaceAlcanzado() {
        Objetivo guardado = objetivoGuardado();
        assertThat(guardado.estaAlcanzado()).isTrue();
        assertThat(guardado.getAlcanzadoEn()).isEqualTo(AHORA);
    }

    private void entoncesNoSeConsultaLaIaNiSeGastaElCupo() {
        verifyNoInteractions(controlPlanService, consumoSamiRepository, redaccionObjetivosIaPort);
    }

    private void entoncesSeReservaElCupoYSeRegistraLoGastado() {
        verify(controlPlanService).exigirIa(NEGOCIO_ID);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    private void entoncesSeGuardanLosTextosDeLaIa() {
        List<MiniObjetivo> guardados = minisGuardados(2);
        assertThat(guardados).extracting(MiniObjetivo::getTitulo).containsExactly("Título 1", "Título 2");
        assertThat(guardados).extracting(MiniObjetivo::getJustificacion).containsExactly("Razón 1", "Razón 2");
        assertThat(guardados).allSatisfy(mini -> assertThat(mini.isTextoConIa()).isTrue());
    }

    private void entoncesLaIaRecibeSoloLosCandidatosElegidos() {
        ArgumentCaptor<List<CandidatoMiniObjetivo>> captor = ArgumentCaptor.captor();
        verify(redaccionObjetivosIaPort).redactar(captor.capture());
        assertThat(captor.getValue()).containsExactly(candidato());
    }

    private void entoncesNoSeConsultaLaIa() {
        verifyNoInteractions(redaccionObjetivosIaPort);
    }

    private void entoncesNoSeGastaCupoNiSeConsultaLaIa() {
        verifyNoInteractions(consumoSamiRepository, redaccionObjetivosIaPort);
    }

    private void entoncesNoSeRegistraConsumo() {
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void entoncesSeGuardaUnMiniConElTextoDeReserva() {
        assertThat(minisGuardados(1)).singleElement().satisfies(mini -> {
            assertThat(mini.getTitulo()).isEqualTo(candidato().titulo());
            assertThat(mini.isTextoConIa()).isFalse();
        });
    }

    private void entoncesSeGuardaUnMiniSinTextoDeIa() {
        assertThat(minisGuardados(1)).singleElement()
                .satisfies(mini -> assertThat(mini.isTextoConIa()).isFalse());
    }

    private void entoncesSeGuardanDosMinisSinTextoDeIa() {
        assertThat(minisGuardados(2)).allSatisfy(mini -> assertThat(mini.isTextoConIa()).isFalse());
    }

    private void entoncesSePropagaElErrorDeLaBase() {
        assertThat(error).isInstanceOf(IllegalStateException.class).hasMessage("base caída");
    }

    // --- helpers ---
    private Objetivo objetivoGuardado() {
        ArgumentCaptor<Objetivo> captor = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(captor.capture());
        return captor.getValue();
    }

    private List<MiniObjetivo> minisGuardados(int cantidad) {
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository, times(cantidad)).save(captor.capture());
        return captor.getAllValues();
    }
}

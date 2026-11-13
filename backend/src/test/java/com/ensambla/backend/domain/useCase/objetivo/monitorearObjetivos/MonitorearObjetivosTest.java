package com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MonitorearObjetivosTest {

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);
    /** El día argentino de AHORA. */
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("00000000-0000-4000-8000-0000000000aa");
    private static final RegistroId PRODUCTO_QUE_YA_NO_ESTA = RegistroId.de("d0000000-0000-4000-8000-0000000000ff");
    /** Lo que deja la yerba por semana con los datos del almacén de prueba: 14 unidades por $900. */
    private static final String GANANCIA_YERBA = "12600.00";

    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final ProgresoObjetivoRepository progresoObjetivoRepository = mock(ProgresoObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final MedidorMiniObjetivo medidor =
            MedidorMiniObjetivo.de(List.of(almacen()), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
    private MonitorearObjetivos monitorearObjetivos;

    @BeforeEach
    void setUp() {
        monitorearObjetivos = monitor(50);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(NEGOCIO_ID));
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor);
        when(miniObjetivoRepository.save(any(MiniObjetivo.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Guarda la foto del resultado del negocio para el objetivo")
    void executeShouldSaveThePhotoOfTheBusinessResultForTheObjetivo() {
        givenObjetivoConMinis(objetivo(), miniObjetivo());

        whenMonitorear();

        ProgresoObjetivo foto = fotosGuardadas().stream().filter(ProgresoObjetivo::esDelObjetivo).findFirst()
                .orElseThrow();
        assertThat(foto.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(foto.dia()).isEqualTo(HOY);
        assertThat(foto.valor()).isEqualByComparingTo("-27000");
        assertThat(foto.medidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mide cada mini-objetivo en curso, lo guarda y anota su foto")
    void executeShouldMeasureEachMiniObjetivoInProgress() {
        MiniObjetivo mini = miniObjetivo();
        mini.setValorActual(new BigDecimal("10000.00"));
        givenObjetivoConMinis(objetivo(), mini);

        whenMonitorear();

        ArgumentCaptor<MiniObjetivo> guardado = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getValorActual()).isEqualByComparingTo(GANANCIA_YERBA);
        assertThat(guardado.getValue().getSinAvanceDesde()).isNull();
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(fotosGuardadas()).filteredOn(foto -> MINI_ID.equals(foto.miniObjetivoId()))
                .singleElement()
                .satisfies(foto -> assertThat(foto.valor()).isEqualByComparingTo(GANANCIA_YERBA));
    }

    @Test
    @DisplayName("Marca como logrado el mini-objetivo cuya meta ya se cumplió")
    void executeShouldMarkAsAchievedTheMiniObjetivoWhoseGoalWasMet() {
        MiniObjetivo mini = miniObjetivo();
        mini.setMeta(new BigDecimal("12000.00"));
        givenObjetivoConMinis(objetivo(), mini);

        whenMonitorear();

        ArgumentCaptor<MiniObjetivo> guardado = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
    }

    @Test
    @DisplayName("Un mini-objetivo cuyo producto ya no está en el cuaderno queda como estaba y no suma foto")
    void executeShouldLeaveAsIsTheMiniObjetivoThatCannotBeMeasured() {
        MiniObjetivo mini = miniObjetivo();
        mini.setRegistroId(PRODUCTO_QUE_YA_NO_ESTA);
        givenObjetivoConMinis(objetivo(), mini);

        whenMonitorear();

        verify(miniObjetivoRepository, never()).save(any());
        assertThat(fotosGuardadas()).allMatch(ProgresoObjetivo::esDelObjetivo);
    }

    @Test
    @DisplayName("No mide los mini-objetivos que ya no están en curso")
    void executeShouldNotMeasureTheMiniObjetivosNoLongerInProgress() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        givenObjetivoConMinis(objetivo(), logrado);

        whenMonitorear();

        verify(miniObjetivoRepository, never()).save(any());
        assertThat(fotosGuardadas()).allMatch(ProgresoObjetivo::esDelObjetivo);
    }

    @Test
    @DisplayName("Marca el objetivo como alcanzado cuando el resultado llega a la meta")
    void executeShouldMarkTheObjetivoAsAchievedWhenTheResultReachesTheGoal() {
        Objetivo objetivo = objetivo();
        objetivo.setMeta(new BigDecimal("-30000"));
        givenObjetivoConMinis(objetivo, miniObjetivo());

        whenMonitorear();

        ArgumentCaptor<Objetivo> guardado = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(guardado.capture());
        assertThat(guardado.getValue().estaAlcanzado()).isTrue();
        assertThat(guardado.getValue().getAlcanzadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Un objetivo que todavía no llega a la meta no se vuelve a guardar")
    void executeShouldNotSaveTheObjetivoWhenItDoesNotReachTheGoal() {
        givenObjetivoConMinis(objetivo(), miniObjetivo());

        whenMonitorear();

        verify(objetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un objetivo ya alcanzado no se vuelve a guardar y sigue anotando su foto")
    void executeShouldNotSaveAnObjetivoThatWasAlreadyAchieved() {
        Objetivo objetivo = objetivo();
        objetivo.setMeta(new BigDecimal("-30000"));
        objetivo.marcarAlcanzado(AHORA.minusDays(3));
        givenObjetivoConMinis(objetivo, miniObjetivo());

        whenMonitorear();

        verify(objetivoRepository, never()).save(any());
        assertThat(fotosGuardadas()).anyMatch(ProgresoObjetivo::esDelObjetivo);
    }

    @Test
    @DisplayName("Un objetivo que nunca tuvo mini-objetivos recibe los suyos con texto de reserva y los mide")
    void executeShouldProposeMiniObjetivosToAnObjetivoWithoutThem() {
        givenObjetivoConMinis(objetivo());
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(List.of(candidato()));

        whenMonitorear();

        ArgumentCaptor<MiniObjetivo> guardado = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository, times(2)).save(guardado.capture());
        MiniObjetivo propuesto = guardado.getAllValues().getFirst();
        assertThat(propuesto.getPosicion()).isEqualTo(1);
        assertThat(propuesto.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(propuesto.isTextoConIa()).isFalse();
        assertThat(fotosGuardadas()).anyMatch(foto -> !foto.esDelObjetivo());
    }

    @Test
    @DisplayName("Un objetivo alcanzado sin mini-objetivos no recibe propuestas")
    void executeShouldNotProposeToAnAchievedObjetivoWithoutMiniObjetivos() {
        Objetivo objetivo = objetivo();
        objetivo.marcarAlcanzado(AHORA.minusDays(3));
        givenObjetivoConMinis(objetivo);

        whenMonitorear();

        verify(propuestaMiniObjetivosService, never()).candidatos(any(NegocioId.class), any(MedidorMiniObjetivo.class));
        verify(miniObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un objetivo con mini-objetivos quitados no recibe propuestas nuevas")
    void executeShouldNotProposeWhenTheObjetivoAlreadyHadMiniObjetivos() {
        MiniObjetivo quitado = miniObjetivo();
        quitado.setEstado(EstadoMiniObjetivo.ABANDONADO);
        givenObjetivoConMinis(objetivo(), quitado);

        whenMonitorear();

        verify(propuestaMiniObjetivosService, never()).candidatos(any(NegocioId.class), any(MedidorMiniObjetivo.class));
    }

    @Test
    @DisplayName("Un negocio con error no frena a los demás")
    void executeShouldKeepGoingWhenABusinessFails() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(OTRO_NEGOCIO_ID, NEGOCIO_ID));
        when(propuestaMiniObjetivosService.medidor(OTRO_NEGOCIO_ID, HOY)).thenThrow(new IllegalStateException("falló"));
        givenObjetivoConMinis(objetivo(), miniObjetivo());

        whenMonitorear();

        verify(objetivoRepository, never()).findAllByNegocioId(OTRO_NEGOCIO_ID);
        assertThat(fotosGuardadas()).anyMatch(ProgresoObjetivo::esDelObjetivo);
    }

    @Test
    @DisplayName("Sin objetivos no mide nada")
    void executeShouldDoNothingWithoutObjetivos() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of());

        whenMonitorear();

        verifyNoInteractions(propuestaMiniObjetivosService, miniObjetivoRepository, progresoObjetivoRepository);
    }

    @Test
    @DisplayName("Pide los negocios de a tandas hasta que una viene incompleta")
    void executeShouldRequestTheBusinessesInBatchesUntilOneIsIncomplete() {
        monitorearObjetivos = monitor(2);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 2)).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(objetivoRepository.findNegocioIdsConObjetivos(1, 2)).thenReturn(List.of(NEGOCIO_ID));
        when(propuestaMiniObjetivosService.medidor(OTRO_NEGOCIO_ID, HOY)).thenReturn(medidor);
        when(objetivoRepository.findAllByNegocioId(any(NegocioId.class))).thenReturn(List.of());

        whenMonitorear();

        verify(objetivoRepository).findNegocioIdsConObjetivos(0, 2);
        verify(objetivoRepository).findNegocioIdsConObjetivos(1, 2);
        verify(objetivoRepository, never()).findNegocioIdsConObjetivos(2, 2);
    }

    @Test
    @DisplayName("No tiene cómo crear avisos ni mandar mensajes: no depende de ellos")
    void shouldNotDependOnNotifications() {
        List<String> tipos = Arrays.stream(MonitorearObjetivos.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getSimpleName)
                .toList();

        assertThat(tipos).noneMatch(tipo -> tipo.contains("Aviso") || tipo.contains("Mensaj")
                || tipo.contains("Whatsapp"));
    }

    // --- arrange ---
    private MonitorearObjetivos monitor(int tamanoTanda) {
        return new MonitorearObjetivos(
                objetivoRepository,
                new MonitoreoObjetivosService(objetivoRepository, miniObjetivoRepository,
                        progresoObjetivoRepository, propuestaMiniObjetivosService),
                new TransaccionDirecta(),
                CLOCK,
                tamanoTanda);
    }

    private void givenObjetivoConMinis(Objetivo objetivo, MiniObjetivo... minis) {
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo));
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(minis));
    }

    // --- act ---
    private void whenMonitorear() {
        monitorearObjetivos.execute();
    }

    // --- assert ---
    private List<ProgresoObjetivo> fotosGuardadas() {
        ArgumentCaptor<ProgresoObjetivo> captor = ArgumentCaptor.forClass(ProgresoObjetivo.class);
        verify(progresoObjetivoRepository, atLeast(0)).registrar(captor.capture());
        return captor.getAllValues();
    }
}

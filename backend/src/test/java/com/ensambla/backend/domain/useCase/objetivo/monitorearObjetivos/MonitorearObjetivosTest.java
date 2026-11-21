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
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
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
    private Objetivo objetivoMonitoreado;

    @BeforeEach
    void setUp() {
        monitorearObjetivos = monitor(50);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(NEGOCIO_ID));
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor);
        when(miniObjetivoRepository.save(any(MiniObjetivo.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Guarda la foto del resultado del negocio para el objetivo")
    void guardaLaFotoDelResultadoDelNegocioParaElObjetivo() {
        dadoObjetivoConMinis(objetivo(), miniObjetivo());

        cuandoMonitorear();

        entoncesSeGuardaLaFotoDelResultadoDelObjetivo();
    }

    @Test
    @DisplayName("Mide cada mini-objetivo en curso y lo guarda")
    void mideCadaMiniObjetivoEnCursoYLoGuarda() {
        dadoObjetivoConMinis(objetivo(), miniEnCursoConValorViejo());

        cuandoMonitorear();

        entoncesElMiniQuedaMedidoYEnCurso();
    }

    @Test
    @DisplayName("Anota la foto de cada mini-objetivo medido")
    void anotaLaFotoDeCadaMiniObjetivoMedido() {
        dadoObjetivoConMinis(objetivo(), miniEnCursoConValorViejo());

        cuandoMonitorear();

        entoncesSeGuardaLaFotoDelMini();
    }

    @Test
    @DisplayName("Marca como logrado el mini-objetivo cuya meta ya se cumplió")
    void marcaComoLogradoElMiniObjetivoCuyaMetaYaSeCumplio() {
        dadoObjetivoConMinis(objetivo(), miniConLaMetaCumplida());

        cuandoMonitorear();

        entoncesElMiniQuedaLogrado();
    }

    @Test
    @DisplayName("Un mini-objetivo cuyo producto ya no está en el cuaderno queda como estaba y no suma foto")
    void unMiniObjetivoCuyoProductoYaNoEstaEnElCuadernoQuedaComoEstabaYNoSumaFoto() {
        dadoObjetivoConMinis(objetivo(), miniConProductoQueYaNoEsta());

        cuandoMonitorear();

        entoncesNoSeGuardaNingunMiniNiSumaFotoDeMini();
    }

    @Test
    @DisplayName("No mide los mini-objetivos que ya no están en curso")
    void noMideLosMiniObjetivosQueYaNoEstanEnCurso() {
        dadoObjetivoConMinis(objetivo(), miniConEstado(EstadoMiniObjetivo.LOGRADO));

        cuandoMonitorear();

        entoncesNoSeGuardaNingunMiniNiSumaFotoDeMini();
    }

    @Test
    @DisplayName("Marca el objetivo como alcanzado cuando el resultado llega a la meta")
    void marcaElObjetivoComoAlcanzadoCuandoElResultadoLlegaALaMeta() {
        dadoObjetivoConMinis(objetivoConMeta("-30000"), miniObjetivo());

        cuandoMonitorear();

        entoncesElObjetivoQuedaAlcanzado();
    }

    @Test
    @DisplayName("Un objetivo que todavía no llega a la meta no se vuelve a guardar")
    void unObjetivoQueTodaviaNoLlegaALaMetaNoSeVuelveAGuardar() {
        dadoObjetivoConMinis(objetivo(), miniObjetivo());

        cuandoMonitorear();

        entoncesNoSeGuardaElObjetivo();
    }

    @Test
    @DisplayName("Un objetivo ya alcanzado no se vuelve a guardar")
    void unObjetivoYaAlcanzadoNoSeVuelveAGuardar() {
        dadoObjetivoConMinis(objetivoYaAlcanzado(), miniObjetivo());

        cuandoMonitorear();

        entoncesNoSeGuardaElObjetivo();
    }

    @Test
    @DisplayName("Un objetivo ya alcanzado sigue anotando su foto")
    void unObjetivoYaAlcanzadoSigueAnotandoSuFoto() {
        dadoObjetivoConMinis(objetivoYaAlcanzado(), miniObjetivo());

        cuandoMonitorear();

        entoncesSeSigueGuardandoLaFotoDelObjetivo();
    }

    @Test
    @DisplayName("Un objetivo que nunca tuvo mini-objetivos recibe los suyos con texto de reserva")
    void unObjetivoQueNuncaTuvoMiniObjetivosRecibeLosSuyosConTextoDeReserva() {
        dadoObjetivoSinMinisYConUnCandidato();

        cuandoMonitorear();

        entoncesSeLeProponeElMiniConTextoDeReserva();
    }

    @Test
    @DisplayName("Un objetivo que nunca tuvo mini-objetivos recibe los suyos y los mide")
    void unObjetivoQueNuncaTuvoMiniObjetivosRecibeLosSuyosYLosMide() {
        dadoObjetivoSinMinisYConUnCandidato();

        cuandoMonitorear();

        entoncesSeGuardaLaFotoDeUnMini();
    }

    @Test
    @DisplayName("Un objetivo alcanzado sin mini-objetivos no recibe propuestas")
    void unObjetivoAlcanzadoSinMiniObjetivosNoRecibePropuestas() {
        dadoObjetivoConMinis(objetivoYaAlcanzado());

        cuandoMonitorear();

        entoncesNoSeProponeNada();
    }

    @Test
    @DisplayName("Un objetivo con mini-objetivos quitados no recibe propuestas nuevas")
    void unObjetivoConMiniObjetivosQuitadosNoRecibePropuestasNuevas() {
        dadoObjetivoConMinis(objetivo(), miniConEstado(EstadoMiniObjetivo.ABANDONADO));

        cuandoMonitorear();

        entoncesNoSeConsultanCandidatos();
    }

    @Test
    @DisplayName("Un mini-objetivo manual no se mide ni suma foto")
    void unMiniObjetivoManualNoSeMideNiSumaFoto() {
        dadoObjetivoConMinis(objetivo(), miniManual());

        cuandoMonitorear();

        entoncesNoSeGuardaNingunMiniNiSumaFotoDeMini();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos nunca se marca alcanzado")
    void unObjetivoSinMetaEnPesosNuncaSeMarcaAlcanzado() {
        dadoObjetivoConMinis(objetivoSinMeta());

        cuandoMonitorear();

        entoncesElObjetivoSinMetaNoQuedaAlcanzado();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos no recibe mini-objetivos solo")
    void unObjetivoSinMetaEnPesosNoRecibeMiniObjetivosSolo() {
        dadoObjetivoConMinis(objetivoSinMeta());

        cuandoMonitorear();

        entoncesSoloSeGuardaLaFotoDelObjetivoSinProponerNada();
    }

    @Test
    @DisplayName("Un negocio con error no frena a los demás")
    void unNegocioConErrorNoFrenaALosDemas() {
        dadoUnNegocioQueFallaAntesDeOtroQueFunciona();

        cuandoMonitorear();

        entoncesElNegocioQueFuncionaSigueMonitoreado();
    }

    @Test
    @DisplayName("Sin objetivos no mide nada")
    void sinObjetivosNoMideNada() {
        dadoNingunNegocioConObjetivos();

        cuandoMonitorear();

        entoncesNoSeMideNiSeGuardaNada();
    }

    @Test
    @DisplayName("Pide los negocios de a tandas hasta que una viene incompleta")
    void pideLosNegociosDeATandasHastaQueUnaVieneIncompleta() {
        dadoNegociosEnTandasDeDos();

        cuandoMonitorear();

        entoncesSePidenLasTandasHastaLaIncompleta();
    }

    @Test
    @DisplayName("No tiene cómo crear avisos ni mandar mensajes: no depende de ellos")
    void shouldNotDependOnNotifications() {
        entoncesNoDependeDeAvisosNiDeMensajes();
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

    private void dadoObjetivoConMinis(Objetivo objetivo, MiniObjetivo... minis) {
        objetivoMonitoreado = objetivo;
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo));
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(minis));
    }

    private void dadoObjetivoSinMinisYConUnCandidato() {
        dadoObjetivoConMinis(objetivo());
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(List.of(candidato()));
    }

    private void dadoUnNegocioQueFallaAntesDeOtroQueFunciona() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(OTRO_NEGOCIO_ID, NEGOCIO_ID));
        when(propuestaMiniObjetivosService.medidor(OTRO_NEGOCIO_ID, HOY)).thenThrow(new IllegalStateException("falló"));
        dadoObjetivoConMinis(objetivo(), miniObjetivo());
    }

    private void dadoNingunNegocioConObjetivos() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of());
    }

    private void dadoNegociosEnTandasDeDos() {
        monitorearObjetivos = monitor(2);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 2)).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(objetivoRepository.findNegocioIdsConObjetivos(1, 2)).thenReturn(List.of(NEGOCIO_ID));
        when(propuestaMiniObjetivosService.medidor(OTRO_NEGOCIO_ID, HOY)).thenReturn(medidor);
        when(objetivoRepository.findAllByNegocioId(any(NegocioId.class))).thenReturn(List.of());
    }

    // --- act ---
    private void cuandoMonitorear() {
        monitorearObjetivos.execute();
    }

    // --- assert ---
    private void entoncesSeGuardaLaFotoDelResultadoDelObjetivo() {
        ProgresoObjetivo foto = fotosGuardadas().stream().filter(ProgresoObjetivo::esDelObjetivo).findFirst()
                .orElseThrow();
        assertThat(foto.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(foto.dia()).isEqualTo(HOY);
        assertThat(foto.valor()).isEqualByComparingTo("-27000");
        assertThat(foto.medidoEn()).isEqualTo(AHORA);
    }

    private void entoncesElMiniQuedaMedidoYEnCurso() {
        MiniObjetivo guardado = miniGuardado();
        assertThat(guardado.getValorActual()).isEqualByComparingTo(GANANCIA_YERBA);
        assertThat(guardado.getSinAvanceDesde()).isNull();
        assertThat(guardado.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
    }

    private void entoncesSeGuardaLaFotoDelMini() {
        assertThat(fotosGuardadas()).filteredOn(foto -> MINI_ID.equals(foto.miniObjetivoId()))
                .singleElement()
                .satisfies(foto -> assertThat(foto.valor()).isEqualByComparingTo(GANANCIA_YERBA));
    }

    private void entoncesElMiniQuedaLogrado() {
        assertThat(miniGuardado().getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
    }

    private void entoncesNoSeGuardaNingunMiniNiSumaFotoDeMini() {
        verify(miniObjetivoRepository, never()).save(any());
        assertThat(fotosGuardadas()).allMatch(ProgresoObjetivo::esDelObjetivo);
    }

    private void entoncesElObjetivoQuedaAlcanzado() {
        ArgumentCaptor<Objetivo> guardado = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(guardado.capture());
        assertThat(guardado.getValue().estaAlcanzado()).isTrue();
        assertThat(guardado.getValue().getAlcanzadoEn()).isEqualTo(AHORA);
    }

    private void entoncesNoSeGuardaElObjetivo() {
        verify(objetivoRepository, never()).save(any());
    }

    private void entoncesSeSigueGuardandoLaFotoDelObjetivo() {
        assertThat(fotosGuardadas()).anyMatch(ProgresoObjetivo::esDelObjetivo);
    }

    private void entoncesSeLeProponeElMiniConTextoDeReserva() {
        ArgumentCaptor<MiniObjetivo> guardado = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository, times(2)).save(guardado.capture());
        MiniObjetivo propuesto = guardado.getAllValues().getFirst();
        assertThat(propuesto.getPosicion()).isEqualTo(1);
        assertThat(propuesto.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(propuesto.isTextoConIa()).isFalse();
    }

    private void entoncesSeGuardaLaFotoDeUnMini() {
        assertThat(fotosGuardadas()).anyMatch(foto -> !foto.esDelObjetivo());
    }

    private void entoncesNoSeProponeNada() {
        entoncesNoSeConsultanCandidatos();
        verify(miniObjetivoRepository, never()).save(any());
    }

    private void entoncesNoSeConsultanCandidatos() {
        verify(propuestaMiniObjetivosService, never()).candidatos(any(NegocioId.class), any(MedidorMiniObjetivo.class));
    }

    private void entoncesElObjetivoSinMetaNoQuedaAlcanzado() {
        assertThat(objetivoMonitoreado.estaAlcanzado()).isFalse();
        verify(objetivoRepository, never()).save(any());
    }

    private void entoncesSoloSeGuardaLaFotoDelObjetivoSinProponerNada() {
        entoncesNoSeProponeNada();
        assertThat(fotosGuardadas()).singleElement().satisfies(foto -> assertThat(foto.esDelObjetivo()).isTrue());
    }

    private void entoncesElNegocioQueFuncionaSigueMonitoreado() {
        verify(objetivoRepository, never()).findAllByNegocioId(OTRO_NEGOCIO_ID);
        assertThat(fotosGuardadas()).anyMatch(ProgresoObjetivo::esDelObjetivo);
    }

    private void entoncesNoSeMideNiSeGuardaNada() {
        verifyNoInteractions(propuestaMiniObjetivosService, miniObjetivoRepository, progresoObjetivoRepository);
    }

    private void entoncesSePidenLasTandasHastaLaIncompleta() {
        verify(objetivoRepository).findNegocioIdsConObjetivos(0, 2);
        verify(objetivoRepository).findNegocioIdsConObjetivos(1, 2);
        verify(objetivoRepository, never()).findNegocioIdsConObjetivos(2, 2);
    }

    private void entoncesNoDependeDeAvisosNiDeMensajes() {
        List<String> tipos = Arrays.stream(MonitorearObjetivos.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getSimpleName)
                .toList();

        assertThat(tipos).noneMatch(tipo -> tipo.contains("Aviso") || tipo.contains("Mensaj")
                || tipo.contains("Whatsapp"));
    }

    // --- helpers ---
    private MiniObjetivo miniGuardado() {
        ArgumentCaptor<MiniObjetivo> guardado = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(guardado.capture());
        return guardado.getValue();
    }

    private List<ProgresoObjetivo> fotosGuardadas() {
        ArgumentCaptor<ProgresoObjetivo> captor = ArgumentCaptor.forClass(ProgresoObjetivo.class);
        verify(progresoObjetivoRepository, atLeast(0)).registrar(captor.capture());
        return captor.getAllValues();
    }

    private static MiniObjetivo miniEnCursoConValorViejo() {
        MiniObjetivo mini = miniObjetivo();
        mini.setValorActual(new BigDecimal("10000.00"));
        return mini;
    }

    private static MiniObjetivo miniConLaMetaCumplida() {
        MiniObjetivo mini = miniObjetivo();
        mini.setMeta(new BigDecimal("12000.00"));
        return mini;
    }

    private static MiniObjetivo miniConProductoQueYaNoEsta() {
        MiniObjetivo mini = miniObjetivo();
        mini.setRegistroId(PRODUCTO_QUE_YA_NO_ESTA);
        return mini;
    }

    private static MiniObjetivo miniConEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(estado);
        return mini;
    }

    private static Objetivo objetivoConMeta(String meta) {
        Objetivo objetivo = objetivo();
        objetivo.setMeta(new BigDecimal(meta));
        return objetivo;
    }

    private static Objetivo objetivoYaAlcanzado() {
        Objetivo objetivo = objetivoConMeta("-30000");
        objetivo.marcarAlcanzado(AHORA.minusDays(3));
        return objetivo;
    }
}

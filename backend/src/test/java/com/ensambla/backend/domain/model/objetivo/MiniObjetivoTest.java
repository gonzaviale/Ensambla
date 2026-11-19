package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MiniObjetivoTest {

    private static final OffsetDateTime DESPUES = AHORA.plusDays(2);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    private MiniObjetivo mini;
    private MiniObjetivo creado;
    private List<MiniObjetivo> propuestos;
    private CandidatoMiniObjetivo candidatoConMeta;
    private CandidatoMiniObjetivo candidatoParaElMini;
    private boolean respuesta;
    private BigDecimal avance;
    private Optional<HitoObjetivo> hito;
    private Optional<BigDecimal> metaSiguiente;
    private Throwable error;

    @Test
    @DisplayName("Uno nuevo arranca en curso con el valor inicial del candidato y un id propio")
    void unoNuevoArrancaEnCursoConElValorInicialDelCandidatoYUnIdPropio() {
        dadoUnCandidato(candidato());

        cuandoSeCreaUnMiniNuevo();

        entoncesArrancaEnCursoConLosDatosDelCandidato();
    }

    @Test
    @DisplayName("Uno nuevo conserva que el texto lo redactó la IA")
    void unoNuevoConservaQueElTextoLoRedactoLaIa() {
        dadoUnCandidatoConTextoDeIa();

        cuandoSeCreaUnMiniNuevo();

        entoncesConservaQueElTextoLoRedactoLaIa();
    }

    @ParameterizedTest
    @CsvSource({"EN_CURSO, true", "LOGRADO, false"})
    @DisplayName("Está en curso mientras no se logre, se reemplace o se quite")
    void estaEnCursoMientrasNoSeLogreSeReemplaceOSeQuite(EstadoMiniObjetivo estado, boolean esperado) {
        dadoUnMini(conEstado(estado));

        cuandoSePreguntaSiEstaEnCurso();

        entoncesLaRespuestaEs(esperado);
    }

    @ParameterizedTest
    @CsvSource({"14182.99, false", "14183.00, true", "20000.00, true"})
    @DisplayName("Un mini-objetivo que sube se cumple cuando el valor llega a la meta o la pasa")
    void unMiniObjetivoQueSubeSeCumpleCuandoElValorLlegaALaMetaOLaPasa(String valorActual, boolean esperado) {
        dadoUnMini(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, valorActual));

        cuandoSePreguntaSiEstaCumplido();

        entoncesLaRespuestaEs(esperado);
    }

    @ParameterizedTest
    @CsvSource({"14183.01, false", "14183.00, true", "100.00, true"})
    @DisplayName("Un mini-objetivo que baja se cumple cuando el valor baja hasta la meta o por debajo")
    void unMiniObjetivoQueBajaSeCumpleCuandoElValorBajaHastaLaMetaOPorDebajo(String valorActual, boolean esperado) {
        dadoUnMini(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, valorActual));

        cuandoSePreguntaSiEstaCumplido();

        entoncesLaRespuestaEs(esperado);
    }

    @Test
    @DisplayName("El avance de uno que sube es la parte del camino de lo inicial a la meta que ya se recorrió")
    void elAvanceDeUnoQueSubeEsLaParteDelCaminoDeLoInicialALaMetaQueYaSeRecorrio() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "150.00"));

        cuandoSeCalculaElAvance();

        entoncesElAvanceEs("0.5");
    }

    @Test
    @DisplayName("El avance de uno que baja cuenta lo que bajó del valor inicial")
    void elAvanceDeUnoQueBajaCuentaLoQueBajoDelValorInicial() {
        dadoUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100.00", "60.00", "80.00"));

        cuandoSeCalculaElAvance();

        entoncesElAvanceEs("0.5");
    }

    @Test
    @DisplayName("El avance no baja de 0 si el valor se aleja de la meta")
    void elAvanceNoBajaDe0SiElValorSeAlejaDeLaMeta() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "50.00"));

        cuandoSeCalculaElAvance();

        entoncesElAvanceEs("0");
    }

    @Test
    @DisplayName("El avance no pasa de 1 si el valor supera la meta")
    void elAvanceNoPasaDe1SiElValorSuperaLaMeta() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "500.00"));

        cuandoSeCalculaElAvance();

        entoncesElAvanceEs("1");
    }

    @Test
    @DisplayName("Sin camino que recorrer, el avance es 1 si ya se cumple")
    void sinCaminoQueRecorrerElAvanceEs1SiYaSeCumple() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "200.00"));

        cuandoSeCalculaElAvance();

        entoncesElAvanceEs("1");
    }

    @Test
    @DisplayName("Sin camino que recorrer, el avance es 0 si no se cumple")
    void sinCaminoQueRecorrerElAvanceEs0SiNoSeCumple() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "150.00"));

        cuandoSeCalculaElAvance();

        entoncesElAvanceEs("0");
    }

    @Test
    @DisplayName("Al reemplazarlo queda reemplazado, apunta al nuevo y se anota cuándo")
    void alReemplazarloQuedaReemplazadoApuntaAlNuevoYSeAnotaCuando() {
        dadoUnMini(miniObjetivo());

        cuandoSeReemplazaPorOtro();

        entoncesQuedaReemplazadoApuntandoAlNuevo();
    }

    @Test
    @DisplayName("Al quitarlo queda abandonado y se anota cuándo")
    void alQuitarloQuedaAbandonadoYSeAnotaCuando() {
        dadoUnMini(miniObjetivo());

        cuandoSeAbandona();

        entoncesQuedaAbandonado();
    }

    @Test
    @DisplayName("No se puede reemplazar uno que ya no está en curso")
    void noSePuedeReemplazarUnoQueYaNoEstaEnCurso() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        cuandoSeReemplazaPorOtro();

        entoncesSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("No se puede quitar uno que ya no está en curso")
    void noSePuedeQuitarUnoQueYaNoEstaEnCurso() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.REEMPLAZADO));

        cuandoSeAbandona();

        entoncesSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("Propone un mini-objetivo en curso por candidato, en las posiciones 1, 2, 3 y en ese orden")
    void proponeUnMiniObjetivoEnCursoPorCandidatoEnLasPosiciones123YEnEseOrden() {
        cuandoSeProponenDosCandidatos();

        entoncesSeProponenEnCursoEnLasPosiciones1Y2EnOrden();
    }

    @Test
    @DisplayName("Sin candidatos no propone ningún mini-objetivo")
    void sinCandidatosNoProponeNingunMiniObjetivo() {
        cuandoSeProponenCandidatos(List.of());

        entoncesNoSeProponeNingunMini();
    }

    @Test
    @DisplayName("Al medir un valor que mejora, lo guarda y deja de estar sin avance")
    void alMedirUnValorQueMejoraLoGuardaYDejaDeEstarSinAvance() {
        dadoUnMini(conSinAvanceDesde(HOY.minusDays(5)));

        cuandoSeMide("13000");

        entoncesGuardaElValorYDejaDeEstarSinAvance();
    }

    @Test
    @DisplayName("Al medir un valor que no mejora, queda sin avance desde hoy")
    void alMedirUnValorQueNoMejoraQuedaSinAvanceDesdeHoy() {
        dadoUnMini(miniObjetivo());

        cuandoSeMide(VALOR_INICIAL.toPlainString());

        entoncesQuedaSinAvanceDesdeHoy();
    }

    @Test
    @DisplayName("Si ya estaba sin avance y el valor empeora, conserva el día en que empezó")
    void siYaEstabaSinAvanceYElValorEmpeoraConservaElDiaEnQueEmpezo() {
        dadoUnMini(conSinAvanceDesde(HOY.minusDays(5)));

        cuandoSeMide("12000");

        entoncesConservaElDiaEnQueEmpezoSinAvance();
    }

    @Test
    @DisplayName("En un tipo que baja, un valor menor es una mejora")
    void enUnTipoQueBajaUnValorMenorEsUnaMejora() {
        dadoUnMini(gastoQueBajaSinAvance());

        cuandoSeMide("38000");

        entoncesEsUnaMejoraYSigueEnCurso();
    }

    @Test
    @DisplayName("En un tipo que baja, un valor mayor no es una mejora")
    void enUnTipoQueBajaUnValorMayorNoEsUnaMejora() {
        dadoUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000"));

        cuandoSeMide("41000");

        entoncesQuedaSinAvanceDesdeHoy();
    }

    @Test
    @DisplayName("Al medir un valor que llega a la meta queda logrado y sin avance")
    void alMedirUnValorQueLlegaALaMetaQuedaLogradoYSinAvance() {
        dadoUnMini(conSinAvanceDesde(HOY.minusDays(20)));

        cuandoSeMide(META_MINI.toPlainString());

        entoncesQuedaLogradoYSinAvance();
    }

    @Test
    @DisplayName("Al medir guarda el valor con centavos")
    void alMedirGuardaElValorConCentavos() {
        dadoUnMini(miniObjetivo());

        cuandoSeMide("12999.996");

        entoncesGuardaElValorConCentavos();
    }

    @Test
    @DisplayName("No se mide un mini-objetivo que ya no está en curso")
    void noSeMideUnMiniObjetivoQueYaNoEstaEnCurso() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        cuandoSeMide(META_MINI.toPlainString());

        entoncesSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("Uno logrado tiene el hito de logrado")
    void unoLogradoTieneElHitoDeLogrado() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        cuandoSePideElHito();

        entoncesElHitoEs(HitoObjetivo.MINI_LOGRADO);
    }

    @Test
    @DisplayName("Uno en curso que no mejora desde hace 14 días tiene el hito de sin avance")
    void unoEnCursoQueNoMejoraDesdeHace14DiasTieneElHitoDeSinAvance() {
        dadoUnMini(conSinAvanceDesde(HOY.minusDays(MiniObjetivo.DIAS_SIN_AVANCE)));

        cuandoSePideElHito();

        entoncesElHitoEs(HitoObjetivo.SIN_AVANCE);
    }

    @Test
    @DisplayName("Uno que no mejora desde hace menos de 14 días todavía no tiene hito")
    void unoQueNoMejoraDesdeHaceMenosDe14DiasTodaviaNoTieneHito() {
        dadoUnMini(conSinAvanceDesde(HOY.minusDays(MiniObjetivo.DIAS_SIN_AVANCE - 1L)));

        cuandoSePideElHito();

        entoncesNoTieneHito();
    }

    @Test
    @DisplayName("Uno que viene mejorando no tiene hito")
    void unoQueVieneMejorandoNoTieneHito() {
        dadoUnMini(miniObjetivo());

        cuandoSePideElHito();

        entoncesNoTieneHito();
    }

    @Test
    @DisplayName("Uno reemplazado no tiene hito aunque haya quedado sin avance")
    void unoReemplazadoNoTieneHitoAunqueHayaQuedadoSinAvance() {
        dadoUnMini(reemplazadoQueQuedoSinAvance());

        cuandoSePideElHito();

        entoncesNoTieneHito();
    }

    @ParameterizedTest
    @CsvSource({"36000, true", "36001, false"})
    @DisplayName("Un valor cumple la meta de un tipo que baja cuando no la supera")
    void unValorCumpleLaMetaDeUnTipoQueBajaCuandoNoLaSupera(String valor, boolean esperado) {
        dadoUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000"));

        cuandoSePreguntaSiElValorCumple(valor);

        entoncesLaRespuestaEs(esperado);
    }

    @ParameterizedTest
    @CsvSource({"LOGRADO", "ABANDONADO"})
    @DisplayName("Uno logrado o quitado que nadie siguió espera el reajuste del cierre")
    void unoLogradoOQuitadoQueNadieSiguioEsperaElReajusteDelCierre(EstadoMiniObjetivo estado) {
        dadoUnMini(conEstado(estado));

        cuandoSePreguntaSiEsperaReajuste();

        entoncesLaRespuestaEs(true);
    }

    @Test
    @DisplayName("Uno en curso no espera reajuste")
    void unoEnCursoNoEsperaReajuste() {
        dadoUnMini(miniObjetivo());

        cuandoSePreguntaSiEsperaReajuste();

        entoncesLaRespuestaEs(false);
    }

    @Test
    @DisplayName("Uno reemplazado no espera reajuste")
    void unoReemplazadoNoEsperaReajuste() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.REEMPLAZADO));

        cuandoSePreguntaSiEsperaReajuste();

        entoncesLaRespuestaEs(false);
    }

    @Test
    @DisplayName("Uno logrado que ya tiene quien lo siga no espera reajuste")
    void unoLogradoQueYaTieneQuienLoSigaNoEsperaReajuste() {
        dadoUnMini(logradoYaSeguidoPorOtro());

        cuandoSePreguntaSiEsperaReajuste();

        entoncesLaRespuestaEs(false);
    }

    @Test
    @DisplayName("La meta siguiente suma al valor al cierre el mismo recorrido que se pidió al principio")
    void laMetaSiguienteSumaAlValorAlCierreElMismoRecorridoQueSePidioAlPrincipio() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "12600", "14183", "14500"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("14500");

        entoncesLaMetaSiguienteEs("16083.00");
    }

    @Test
    @DisplayName("Si el valor al cierre quedó debajo de la meta, la meta siguiente parte de la meta")
    void siElValorAlCierreQuedoDebajoDeLaMetaLaMetaSiguienteParteDeLaMeta() {
        dadoUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "12600", "14183", "14500"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("14000");

        entoncesLaMetaSiguienteEs("15766.00");
    }

    @Test
    @DisplayName("En un tipo que baja, la meta siguiente resta el recorrido inicial")
    void enUnTipoQueBajaLaMetaSiguienteRestaElRecorridoInicial() {
        dadoUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "35000"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("35000");

        entoncesLaMetaSiguienteEs("31000.00");
    }

    @Test
    @DisplayName("Un gasto no puede bajar de cero: si ya llegó, no hay meta siguiente")
    void unGastoNoPuedeBajarDeCeroSiYaLlegoNoHayMetaSiguiente() {
        dadoUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100", "0", "0"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("0");

        entoncesNoHayMetaSiguiente();
    }

    @Test
    @DisplayName("El margen siguiente no pasa del tope")
    void elMargenSiguienteNoPasaDelTope() {
        dadoUnMini(conValores(TipoMiniObjetivo.MEJORAR_MARGEN, "55", "57", "59"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("59");

        entoncesLaMetaSiguienteEs("60.00");
    }

    @Test
    @DisplayName("Un margen que ya está en el tope no tiene meta siguiente")
    void unMargenQueYaEstaEnElTopeNoTieneMetaSiguiente() {
        dadoUnMini(conValores(TipoMiniObjetivo.MEJORAR_MARGEN, "58", "60", "60"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("60");

        entoncesNoHayMetaSiguiente();
    }

    @Test
    @DisplayName("Si la meta era igual al valor inicial, la siguiente pide un 10 % más")
    void siLaMetaEraIgualAlValorInicialLaSiguientePideUn10Mas() {
        dadoUnMini(conValores(TipoMiniObjetivo.VENTAS_SEMANALES, "100", "100", "100"));

        cuandoSePideLaMetaSiguienteConValorAlCierre("100");

        entoncesLaMetaSiguienteEs("110.00");
    }

    @Test
    @DisplayName("Con otra meta conserva el tipo, el producto, el aporte y el título, y arranca del valor al cierre")
    void conOtraMetaConservaElTipoElProductoElAporteYElTituloYArrancaDelValorAl() {
        dadoUnMini(miniObjetivo());

        cuandoSePideUnCandidatoConOtraMeta();

        entoncesElCandidatoConservaElMiniYArrancaDelValorAlCierre();
    }

    @Test
    @DisplayName("Uno en curso que se logra al cierre queda logrado y apunta al que lo sigue")
    void unoEnCursoQueSeLograAlCierreQuedaLogradoYApuntaAlQueLoSigue() {
        dadoUnMini(conSinAvanceDesde(HOY));

        cuandoSeLograYSeguirConOtro();

        entoncesQuedaLogradoSinAvanceYApuntaAlQueLoSigue();
    }

    @Test
    @DisplayName("Uno que ya estaba logrado conserva su estado y apunta al que lo sigue")
    void unoQueYaEstabaLogradoConservaSuEstadoYApuntaAlQueLoSigue() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        cuandoSeLograYSeguirConOtro();

        entoncesSigueLogradoYApuntaAlQueLoSigue();
    }

    @Test
    @DisplayName("No se puede seguir uno logrado que ya tiene quien lo siga")
    void noSePuedeSeguirUnoLogradoQueYaTieneQuienLoSiga() {
        dadoUnMini(logradoYaSeguidoPorOtro());

        cuandoSeLograYSeguirConOtro();

        entoncesSeRechazaPorqueYaFueReajustado();
    }

    @Test
    @DisplayName("No se puede subir la meta de uno quitado")
    void noSePuedeSubirLaMetaDeUnoQuitado() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.ABANDONADO));

        cuandoSeLograYSeguirConOtro();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Uno en curso que cede su lugar queda reemplazado")
    void unoEnCursoQueCedeSuLugarQuedaReemplazado() {
        dadoUnMini(miniObjetivo());

        cuandoCedeSuLugarAOtro();

        entoncesQuedaReemplazadoYApuntaAlQueOcupaElLugar();
    }

    @Test
    @DisplayName("Uno quitado que cede su lugar sigue quitado y apunta al que ocupó el lugar")
    void unoQuitadoQueCedeSuLugarSigueQuitadoYApuntaAlQueOcupoElLugar() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.ABANDONADO));

        cuandoCedeSuLugarAOtro();

        entoncesSigueQuitadoYApuntaAlQueOcupoElLugar();
    }

    @Test
    @DisplayName("No cede su lugar uno que ya fue reemplazado")
    void noCedeSuLugarUnoQueYaFueReemplazado() {
        dadoUnMini(conEstado(EstadoMiniObjetivo.REEMPLAZADO));

        cuandoCedeSuLugarAOtro();

        entoncesSeRechazaPorqueYaFueReajustado();
    }

    @Test
    @DisplayName("Un mini-objetivo es medido por defecto y no manual")
    void unMiniObjetivoEsMedidoPorDefectoYNoManual() {
        dadoUnMini(miniObjetivo());

        entoncesEsMedidoYNoManual();
    }

    @Test
    @DisplayName("Un mini-objetivo manual no es medido")
    void unMiniObjetivoManualNoEsMedido() {
        dadoUnMini(miniManual());

        entoncesEsManualYNoMedido();
    }

    @Test
    @DisplayName("Un manual en curso se puede responder")
    void unManualEnCursoSePuedeResponder() {
        dadoUnMini(miniManual());

        cuandoSeExigeQueSeaUnManualEnCurso();

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Un mini-objetivo medido no se responde")
    void unMiniObjetivoMedidoNoSeResponde() {
        dadoUnMini(miniObjetivo());

        cuandoSeExigeQueSeaUnManualEnCurso();

        entoncesSeRechazaPorqueSoloSeRespondenLosManuales();
    }

    @Test
    @DisplayName("Un manual que ya no está en curso no se responde")
    void unManualQueYaNoEstaEnCursoNoSeResponde() {
        dadoUnMini(manualAbandonado());

        cuandoSeExigeQueSeaUnManualEnCurso();

        entoncesSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("Una acción manual nace en curso, sin métrica ni números, en la posición y la semana pedidas")
    void unaAccionManualNaceEnCursoSinMetricaNiNumerosEnLaPosicionYLaSemanaPedidas() {
        cuandoSeCreaUnMiniManual();

        entoncesNaceManualEnCursoSinMetricaNiNumeros();
    }

    // --- arrange ---
    private void dadoUnMini(MiniObjetivo miniDePrueba) {
        mini = miniDePrueba;
    }

    private void dadoUnCandidato(CandidatoMiniObjetivo candidatoDePrueba) {
        candidatoParaElMini = candidatoDePrueba;
    }

    // --- act ---
    private void cuandoSeCreaUnMiniNuevo() {
        creado = MiniObjetivo.nuevo(objetivo(), 2, candidatoParaElMini, SEMANA, AHORA);
    }

    private void cuandoSeCreaUnMiniManual() {
        creado = MiniObjetivo.manual(
                objetivo(), 2, new AccionManual(TITULO_MANUAL, JUSTIFICACION_MANUAL), true, SEMANA, AHORA);
    }

    private void cuandoSeProponenDosCandidatos() {
        CandidatoMiniObjetivo margen = candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "14000");
        cuandoSeProponenCandidatos(List.of(candidato(), margen));
    }

    private void cuandoSeProponenCandidatos(List<CandidatoMiniObjetivo> candidatos) {
        propuestos = MiniObjetivo.propuestos(objetivo(), candidatos, SEMANA, AHORA);
    }

    private void cuandoSePreguntaSiEstaEnCurso() {
        respuesta = mini.estaEnCurso();
    }

    private void cuandoSePreguntaSiEstaCumplido() {
        respuesta = mini.cumplido();
    }

    private void cuandoSePreguntaSiElValorCumple(String valor) {
        respuesta = mini.cumpleCon(new BigDecimal(valor));
    }

    private void cuandoSePreguntaSiEsperaReajuste() {
        respuesta = mini.esperaReajuste();
    }

    private void cuandoSeCalculaElAvance() {
        avance = mini.avance();
    }

    private void cuandoSeReemplazaPorOtro() {
        error = catchThrowable(() -> mini.reemplazarPor(OTRO_MINI_ID, DESPUES));
    }

    private void cuandoSeAbandona() {
        error = catchThrowable(() -> mini.abandonar(DESPUES));
    }

    private void cuandoSeMide(String valor) {
        error = catchThrowable(() -> mini.medir(new BigDecimal(valor), HOY, DESPUES));
    }

    private void cuandoSePideElHito() {
        hito = mini.hito(HOY);
    }

    private void cuandoSePideLaMetaSiguienteConValorAlCierre(String valorAlCierre) {
        metaSiguiente = mini.metaSiguiente(new BigDecimal(valorAlCierre));
    }

    private void cuandoSePideUnCandidatoConOtraMeta() {
        candidatoConMeta = mini.conMeta(new BigDecimal("14500"), new BigDecimal("16083.00"), "Lo lograste");
    }

    private void cuandoSeLograYSeguirConOtro() {
        error = catchThrowable(() -> mini.lograrYSeguirCon(OTRO_MINI_ID, DESPUES));
    }

    private void cuandoCedeSuLugarAOtro() {
        error = catchThrowable(() -> mini.cederLugarA(OTRO_MINI_ID, DESPUES));
    }

    private void cuandoSeExigeQueSeaUnManualEnCurso() {
        error = catchThrowable(() -> mini.exigirManualEnCurso());
    }

    // --- assert ---
    private void entoncesArrancaEnCursoConLosDatosDelCandidato() {
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(creado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(creado.getPosicion()).isEqualTo(2);
        assertThat(creado.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(creado.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(creado.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(creado.getMeta()).isEqualTo(META_MINI);
        assertThat(creado.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(creado.getValorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(creado.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(creado.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(creado.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(creado.getTitulo()).isEqualTo(TITULO);
        assertThat(creado.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(creado.isTextoConIa()).isFalse();
        assertThat(creado.getReemplazadoPor()).isNull();
        assertThat(creado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(creado.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesConservaQueElTextoLoRedactoLaIa() {
        assertThat(creado.isTextoConIa()).isTrue();
        assertThat(creado.getTitulo()).isEqualTo("Título");
    }

    private void entoncesLaRespuestaEs(boolean esperada) {
        assertThat(respuesta).isEqualTo(esperada);
    }

    private void entoncesElAvanceEs(String esperado) {
        assertThat(avance).isEqualByComparingTo(esperado);
    }

    private void entoncesQuedaReemplazadoApuntandoAlNuevo() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesQuedaAbandonado() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesSeRechazaPorqueYaNoEstaEnCurso() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    private void entoncesSeProponenEnCursoEnLasPosiciones1Y2EnOrden() {
        assertThat(propuestos).extracting(MiniObjetivo::getPosicion).containsExactly(1, 2);
        assertThat(propuestos).extracting(MiniObjetivo::getTipo)
                .containsExactly(TipoMiniObjetivo.SUBIR_PRECIO, TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(propuestos).allSatisfy(propuesto -> {
            assertThat(propuesto.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
            assertThat(propuesto.getObjetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(propuesto.getSemanaInicio()).isEqualTo(SEMANA);
        });
    }

    private void entoncesNoSeProponeNingunMini() {
        assertThat(propuestos).isEmpty();
    }

    private void entoncesGuardaElValorYDejaDeEstarSinAvance() {
        assertThat(mini.getValorActual()).isEqualByComparingTo("13000");
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesQuedaSinAvanceDesdeHoy() {
        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY);
    }

    private void entoncesConservaElDiaEnQueEmpezoSinAvance() {
        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY.minusDays(5));
        assertThat(mini.getValorActual()).isEqualByComparingTo("12000");
    }

    private void entoncesEsUnaMejoraYSigueEnCurso() {
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
    }

    private void entoncesQuedaLogradoYSinAvance() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getValorActual()).isEqualByComparingTo(META_MINI);
    }

    private void entoncesGuardaElValorConCentavos() {
        assertThat(mini.getValorActual()).isEqualTo(new BigDecimal("13000.00"));
    }

    private void entoncesElHitoEs(HitoObjetivo esperado) {
        assertThat(hito).contains(esperado);
    }

    private void entoncesNoTieneHito() {
        assertThat(hito).isEmpty();
    }

    private void entoncesLaMetaSiguienteEs(String esperada) {
        assertThat(metaSiguiente).hasValueSatisfying(meta -> assertThat(meta).isEqualByComparingTo(esperada));
    }

    private void entoncesNoHayMetaSiguiente() {
        assertThat(metaSiguiente).isEmpty();
    }

    private void entoncesElCandidatoConservaElMiniYArrancaDelValorAlCierre() {
        assertThat(candidatoConMeta.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(candidatoConMeta.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(candidatoConMeta.referencia()).isEqualTo(PRODUCTO);
        assertThat(candidatoConMeta.valorInicial()).isEqualByComparingTo("14500.00");
        assertThat(candidatoConMeta.valorInicial().scale()).isEqualTo(2);
        assertThat(candidatoConMeta.meta()).isEqualByComparingTo("16083.00");
        assertThat(candidatoConMeta.aporteEstimado()).isEqualTo(APORTE);
        assertThat(candidatoConMeta.titulo()).isEqualTo(TITULO);
        assertThat(candidatoConMeta.justificacion()).isEqualTo("Lo lograste");
        assertThat(candidatoConMeta.textoConIa()).isFalse();
    }

    private void entoncesQuedaLogradoSinAvanceYApuntaAlQueLoSigue() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesSigueLogradoYApuntaAlQueLoSigue() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
    }

    private void entoncesSeRechazaPorqueYaFueReajustado() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya fue reajustado");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesQuedaReemplazadoYApuntaAlQueOcupaElLugar() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
    }

    private void entoncesSigueQuitadoYApuntaAlQueOcupoElLugar() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesEsMedidoYNoManual() {
        assertThat(mini.getModo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(mini.esMedido()).isTrue();
        assertThat(mini.esManual()).isFalse();
    }

    private void entoncesEsManualYNoMedido() {
        assertThat(mini.esManual()).isTrue();
        assertThat(mini.esMedido()).isFalse();
    }

    private void entoncesNoSeRechaza() {
        assertThat(error).isNull();
    }

    private void entoncesSeRechazaPorqueSoloSeRespondenLosManuales() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sólo se responden los mini-objetivos manuales");
    }

    private void entoncesNaceManualEnCursoSinMetricaNiNumeros() {
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(creado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(creado.getPosicion()).isEqualTo(2);
        assertThat(creado.esManual()).isTrue();
        assertThat(creado.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(creado.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(creado.getTitulo()).isEqualTo(TITULO_MANUAL);
        assertThat(creado.getJustificacion()).isEqualTo(JUSTIFICACION_MANUAL);
        assertThat(creado.isTextoConIa()).isTrue();
        assertThat(creado.getTipo()).isNull();
        assertThat(creado.getMeta()).isNull();
        assertThat(creado.getValorInicial()).isNull();
        assertThat(creado.getValorActual()).isNull();
        assertThat(creado.getAporteEstimado()).isNull();
        assertThat(creado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(creado.getActualizadoEn()).isEqualTo(AHORA);
    }

    // --- helpers ---
    private static MiniObjetivo conSinAvanceDesde(LocalDate dia) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setSinAvanceDesde(dia);
        return miniDePrueba;
    }

    private static MiniObjetivo conEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setEstado(estado);
        return miniDePrueba;
    }

    private static MiniObjetivo conValorActual(TipoMiniObjetivo tipo, String valorActual) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setTipo(tipo);
        miniDePrueba.setValorActual(new BigDecimal(valorActual));
        return miniDePrueba;
    }

    private static MiniObjetivo conValores(TipoMiniObjetivo tipo, String inicial, String meta, String actual) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setTipo(tipo);
        miniDePrueba.setValorInicial(new BigDecimal(inicial));
        miniDePrueba.setMeta(new BigDecimal(meta));
        miniDePrueba.setValorActual(new BigDecimal(actual));
        return miniDePrueba;
    }

    private static MiniObjetivo gastoQueBajaSinAvance() {
        MiniObjetivo gasto = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000");
        gasto.setSinAvanceDesde(HOY.minusDays(3));
        return gasto;
    }

    private static MiniObjetivo reemplazadoQueQuedoSinAvance() {
        MiniObjetivo reemplazado = conSinAvanceDesde(HOY.minusDays(30));
        reemplazado.setEstado(EstadoMiniObjetivo.REEMPLAZADO);
        return reemplazado;
    }

    private static MiniObjetivo logradoYaSeguidoPorOtro() {
        MiniObjetivo seguido = conEstado(EstadoMiniObjetivo.LOGRADO);
        seguido.setReemplazadoPor(MiniObjetivoId.nuevo());
        return seguido;
    }

    private static MiniObjetivo manualAbandonado() {
        MiniObjetivo manual = miniManual();
        manual.abandonar(AHORA);
        return manual;
    }

    private void dadoUnCandidatoConTextoDeIa() {
        dadoUnCandidato(candidato().conTextoDeIa(new TextoMiniObjetivo("Título", "Razón")));
    }
}

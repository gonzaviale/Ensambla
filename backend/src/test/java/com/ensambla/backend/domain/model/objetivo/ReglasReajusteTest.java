package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.DecisionTestData.impacto;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class ReglasReajusteTest {

    /** La semana que se cierra: la siguiente a la que empezó el mini-objetivo de prueba. */
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 16);
    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    /** Por debajo de la meta de prueba ($14.183). */
    private static final Optional<BigDecimal> SIN_LLEGAR = Optional.of(new BigDecimal("13000"));
    private static final Optional<BigDecimal> NADA = Optional.empty();

    private MiniObjetivo mini = miniObjetivo();
    private Optional<BigDecimal> alCierre = NADA;
    private Optional<BigDecimal> dosAtras = NADA;
    private boolean sinEfecto;
    private Optional<MotivoReajuste> motivo;

    private List<DecisionObjetivo> decisiones = List.of();
    private Map<AvisoId, Aviso> avisos = Map.of();
    private boolean decisionSinEfecto;

    private List<MiniObjetivo> miniObjetivos = List.of();
    private List<MiniObjetivo> usados;
    private List<CandidatoMiniObjetivo> candidatos = List.of();
    private List<MiniObjetivo> enCurso = List.of();
    private List<MiniObjetivo> yaUsados = List.of();
    private MotivoReajuste motivoDelReemplazo = MotivoReajuste.SIN_AVANCE;
    private Optional<CandidatoMiniObjetivo> reemplazo;
    private CandidatoMiniObjetivo esperado;

    @Test
    @DisplayName("Uno logrado que nadie siguió sube su meta")
    void unoLogradoQueNadieSiguioSubeSuMeta() {
        dadoUnMiniObjetivoConEstado(EstadoMiniObjetivo.LOGRADO);

        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.LOGRADO);
    }

    @Test
    @DisplayName("Uno quitado que nadie reemplazó se cambia por otro")
    void unoQuitadoQueNadieReemplazoSeCambiaPorOtro() {
        dadoUnMiniObjetivoConEstado(EstadoMiniObjetivo.ABANDONADO);

        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.QUITADO);
    }

    @Test
    @DisplayName("Uno logrado o quitado que ya tiene quien lo siga no se toca")
    void unoLogradoOQuitadoQueYaTieneQuienLoSigaNoSeToca() {
        dadoUnMiniObjetivoLogradoQueYaTieneQuienLoSiga();

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("Uno reemplazado no se toca")
    void unoReemplazadoNoSeToca() {
        dadoUnMiniObjetivoConEstado(EstadoMiniObjetivo.REEMPLAZADO);

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("Uno de producto que ya no se puede medir se cambia")
    void unoDeProductoQueYaNoSePuedeMedirSeCambia() {
        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.PRODUCTO_FUERA);
    }

    @Test
    @DisplayName("Uno del negocio que no se puede medir (sin costos para el margen) se mantiene")
    void unoDelNegocioQueNoSePuedeMedirSinCostosParaElMargenSeMantiene() {
        dadoUnMiniObjetivoDeMejorarMargen();

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("Uno en curso que al domingo llega a su meta sube la meta")
    void unoEnCursoQueAlDomingoLlegaASuMetaSubeLaMeta() {
        dadoQueAlCierreSeLlegoALaMeta();

        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.LOGRADO);
    }

    @Test
    @DisplayName("Uno que empezó la misma semana que se cierra sólo puede subir su meta")
    void unoQueEmpezoLaMismaSemanaQueSeCierraSoloPuedeSubirSuMeta() {
        dadoUnMiniObjetivoQueEmpezoLaSemanaQueSeCierra();

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("Si una decisión vinculada no movió la venta, prueba otro camino")
    void siUnaDecisionVinculadaNoMovioLaVentaPruebaOtroCamino() {
        dadoUnaDecisionVinculadaSinEfectoYSinLlegarALaMeta();

        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.DECISION_SIN_EFECTO);
    }

    @Test
    @DisplayName("Si no mejoró respecto de dos cierres atrás, se cambia por falta de avance")
    void siNoMejoroRespectoDeDosCierresAtrasSeCambiaPorFaltaDeAvance() {
        dadoQueNoMejoroRespectoDeDosCierresAtras();

        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.SIN_AVANCE);
    }

    @Test
    @DisplayName("Si mejoró respecto de dos cierres atrás, se mantiene")
    void siMejoroRespectoDeDosCierresAtrasSeMantiene() {
        dadoQueMejoroRespectoDeDosCierresAtras();

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("En un tipo que baja, mejorar es bajar: si bajó, se mantiene")
    void enUnTipoQueBajaMejorarEsBajarSiBajoSeMantiene() {
        dadoUnRecorteDeGastoQueBajoDe40000A38000();

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("En un tipo que baja, si no bajó se cambia por falta de avance")
    void enUnTipoQueBajaSiNoBajoSeCambiaPorFaltaDeAvance() {
        dadoUnRecorteDeGastoQueSeQuedoEn40000();

        cuandoSeDecideElReajuste();

        entoncesElMotivoEs(MotivoReajuste.SIN_AVANCE);
    }

    @Test
    @DisplayName("Sin cierre de dos semanas atrás no hay con qué medir el avance: se mantiene")
    void sinCierreDeDosSemanasAtrasNoHayConQueMedirElAvanceSeMantiene() {
        dadoUnCierreSinLlegarALaMetaYSinCierreDeDosSemanasAtras();

        cuandoSeDecideElReajuste();

        entoncesNoSeReajusta();
    }

    @Test
    @DisplayName("Una decisión de aplicar un aviso medida y sin subir las ventas no tuvo efecto")
    void unaDecisionDeAplicarUnAvisoMedidaYSinSubirLasVentasNoTuvoEfecto() {
        dadoUnAvisoAplicadoCuyasVentasNoSubieron();

        cuandoSeConsultaSiLaDecisionTuvoEfecto();

        entoncesLaDecisionNoTuvoEfecto();
    }

    @Test
    @DisplayName("Una decisión que subió las ventas no cuenta como sin efecto")
    void unaDecisionQueSubioLasVentasNoCuentaComoSinEfecto() {
        dadoUnAvisoAplicadoCuyasVentasSubieron();

        cuandoSeConsultaSiLaDecisionTuvoEfecto();

        entoncesLaDecisionNoCuentaComoSinEfecto();
    }

    @Test
    @DisplayName("Una decisión que se sigue midiendo no cuenta como sin efecto")
    void unaDecisionQueSeSigueMidiendoNoCuentaComoSinEfecto() {
        dadoUnAvisoAplicadoQueSigueMidiendose();

        cuandoSeConsultaSiLaDecisionTuvoEfecto();

        entoncesLaDecisionNoCuentaComoSinEfecto();
    }

    @Test
    @DisplayName("Una decisión de otro mini-objetivo no cuenta como sin efecto")
    void unaDecisionDeOtroMiniObjetivoNoCuentaComoSinEfecto() {
        dadoUnAvisoAplicadoCuyasVentasSubieronYOtroMiniObjetivo();

        cuandoSeConsultaSiLaDecisionTuvoEfecto();

        entoncesLaDecisionNoCuentaComoSinEfecto();
    }

    @Test
    @DisplayName("Una decisión sobre un paso, sin aviso, no se mide")
    void unaDecisionSobreUnPasoSinAvisoNoSeMide() {
        dadoUnaDecisionSobreUnPasoSinAviso();

        cuandoSeConsultaSiLaDecisionTuvoEfecto();

        entoncesLaDecisionNoCuentaComoSinEfecto();
    }

    @Test
    @DisplayName("Los usados son los que siguen en curso y los que empezaron en las últimas cuatro semanas")
    void losUsadosSonLosQueSiguenEnCursoYLosQueEmpezaronEnLasUltimasCuatroSemanas() {
        dadoUnoViejoEnCursoUnoRecienteLogradoYUnoViejoReemplazado();

        cuandoSePidenLosUsados();

        entoncesLosUsadosSonElViejoEnCursoYElRecienteLogrado();
    }

    @Test
    @DisplayName("El reemplazo es el de mayor aporte que no es el mismo ni uno usado ni choca con otro en curso")
    void elReemplazoEsElDeMayorAporteQueNoEsElMismoNiUnoUsadoNiChocaConOtroEnCurso() {
        dadoCandidatosQueIncluyenAlMismoUnoUsadoUnoQueChocaYUnoLibre();

        cuandoSePideElReemplazo();

        entoncesElReemplazoEsElCandidatoLibre();
    }

    @Test
    @DisplayName("Para probar otro camino el reemplazo tiene que ser de otro tipo")
    void paraProbarOtroCaminoElReemplazoTieneQueSerDeOtroTipo() {
        dadoUnCandidatoDelMismoTipoYOtroDeOtroTipo();
        dadoQueSeProbaraOtroCamino();

        cuandoSePideElReemplazo();

        entoncesElReemplazoEsElDeOtroTipo();
    }

    @Test
    @DisplayName("Si no hace falta otro camino el reemplazo puede ser del mismo tipo")
    void siNoHaceFaltaOtroCaminoElReemplazoPuedeSerDelMismoTipo() {
        dadoUnCandidatoDelMismoTipoYOtroDeOtroTipo();

        cuandoSePideElReemplazo();

        entoncesElReemplazoEsElDelMismoTipo();
    }

    @Test
    @DisplayName("Sin candidatos posibles no hay reemplazo")
    void sinCandidatosPosiblesNoHayReemplazo() {
        dadoUnSoloCandidatoYNingunMiniObjetivoEnCurso();

        cuandoSePideElReemplazo();

        entoncesNoHayReemplazo();
    }

    // --- dado ---

    private void dadoUnMiniObjetivoConEstado(EstadoMiniObjetivo estado) {
        mini = miniObjetivo();
        mini.setEstado(estado);
    }

    private void dadoUnMiniObjetivoLogradoQueYaTieneQuienLoSiga() {
        dadoUnMiniObjetivoConEstado(EstadoMiniObjetivo.LOGRADO);
        mini.setReemplazadoPor(OTRO_MINI_ID);
    }

    private void dadoUnMiniObjetivoDeMejorarMargen() {
        mini.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    private void dadoQueAlCierreSeLlegoALaMeta() {
        alCierre = Optional.of(new BigDecimal("15000"));
        sinEfecto = true;
    }

    private void dadoUnMiniObjetivoQueEmpezoLaSemanaQueSeCierra() {
        mini.setSemanaInicio(SEMANA);
        alCierre = SIN_LLEGAR;
        dosAtras = SIN_LLEGAR;
        sinEfecto = true;
    }

    private void dadoUnaDecisionVinculadaSinEfectoYSinLlegarALaMeta() {
        alCierre = SIN_LLEGAR;
        sinEfecto = true;
    }

    private void dadoQueNoMejoroRespectoDeDosCierresAtras() {
        alCierre = SIN_LLEGAR;
        dosAtras = SIN_LLEGAR;
    }

    private void dadoQueMejoroRespectoDeDosCierresAtras() {
        alCierre = SIN_LLEGAR;
        dosAtras = Optional.of(new BigDecimal("12000"));
    }

    private void dadoUnRecorteDeGastoQueBajoDe40000A38000() {
        dadoUnRecorteDeGastoConMeta36000();
        alCierre = Optional.of(new BigDecimal("38000"));
        dosAtras = Optional.of(new BigDecimal("40000"));
    }

    private void dadoUnRecorteDeGastoQueSeQuedoEn40000() {
        dadoUnRecorteDeGastoConMeta36000();
        alCierre = Optional.of(new BigDecimal("40000"));
        dosAtras = Optional.of(new BigDecimal("40000"));
    }

    private void dadoUnRecorteDeGastoConMeta36000() {
        mini.setTipo(TipoMiniObjetivo.RECORTAR_GASTO);
        mini.setMeta(new BigDecimal("36000"));
    }

    private void dadoUnCierreSinLlegarALaMetaYSinCierreDeDosSemanasAtras() {
        alCierre = SIN_LLEGAR;
        dosAtras = NADA;
    }

    private void dadoUnAvisoAplicadoCuyasVentasNoSubieron() {
        dadoUnAvisoAplicadoConImpacto(new ImpactoAviso(14, new BigDecimal("3.00"), new BigDecimal("2.00"),
                new BigDecimal("150.00"), new BigDecimal("100.00")));
    }

    private void dadoUnAvisoAplicadoCuyasVentasSubieron() {
        dadoUnAvisoAplicadoConImpacto(impacto());
    }

    private void dadoUnAvisoAplicadoQueSigueMidiendose() {
        decisiones = List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        avisos = Map.of(AVISO_ID, aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO));
    }

    private void dadoUnAvisoAplicadoCuyasVentasSubieronYOtroMiniObjetivo() {
        dadoUnAvisoAplicadoCuyasVentasSubieron();
        mini = miniObjetivo();
        mini.setId(OTRO_MINI_ID);
    }

    private void dadoUnaDecisionSobreUnPasoSinAviso() {
        decisiones = List.of(decision(TipoDecision.PASO_ACEPTADO, null));
        avisos = Map.of();
    }

    private void dadoUnAvisoAplicadoConImpacto(ImpactoAviso impacto) {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(impacto, AHORA);
        decisiones = List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        avisos = Map.of(AVISO_ID, aplicado);
    }

    private void dadoUnoViejoEnCursoUnoRecienteLogradoYUnoViejoReemplazado() {
        MiniObjetivo viejoEnCurso = miniObjetivo();
        viejoEnCurso.setSemanaInicio(SEMANA.minusWeeks(10));
        MiniObjetivo recienteLogrado = miniObjetivo();
        recienteLogrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        recienteLogrado.setSemanaInicio(SEMANA.minusWeeks(3));
        MiniObjetivo viejoReemplazado = miniObjetivo();
        viejoReemplazado.setEstado(EstadoMiniObjetivo.REEMPLAZADO);
        viejoReemplazado.setSemanaInicio(SEMANA.minusWeeks(4));
        miniObjetivos = List.of(viejoEnCurso, recienteLogrado, viejoReemplazado);
    }

    private void dadoCandidatosQueIncluyenAlMismoUnoUsadoUnoQueChocaYUnoLibre() {
        esperado = candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "1000");
        candidatos = List.of(candidato(),
                candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "9000"),
                candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "8000"),
                esperado);
        enCurso = List.of(mini, otroEnCurso(TipoMiniObjetivo.MEJORAR_MARGEN));
        yaUsados = List.of(otroEnCurso(TipoMiniObjetivo.VENTAS_SEMANALES));
    }

    private void dadoUnCandidatoDelMismoTipoYOtroDeOtroTipo() {
        CandidatoMiniObjetivo mismoTipo = new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID,
                "Aceite", BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("9000"), "Subile al aceite", "x", false);
        CandidatoMiniObjetivo otroTipo = candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "1000");
        candidatos = List.of(mismoTipo, otroTipo);
        enCurso = List.of(mini);
    }

    private void dadoQueSeProbaraOtroCamino() {
        motivoDelReemplazo = MotivoReajuste.DECISION_SIN_EFECTO;
    }

    private void dadoUnSoloCandidatoYNingunMiniObjetivoEnCurso() {
        candidatos = List.of(candidato());
        enCurso = List.of();
    }

    // --- cuando ---

    private void cuandoSeDecideElReajuste() {
        motivo = ReglasReajuste.decidir(mini, alCierre, dosAtras, sinEfecto, SEMANA);
    }

    private void cuandoSeConsultaSiLaDecisionTuvoEfecto() {
        decisionSinEfecto = ReglasReajuste.decisionSinEfecto(mini, decisiones, avisos);
    }

    private void cuandoSePidenLosUsados() {
        usados = ReglasReajuste.usados(miniObjetivos, SEMANA);
    }

    private void cuandoSePideElReemplazo() {
        reemplazo = ReglasReajuste.reemplazo(candidatos, mini, motivoDelReemplazo, enCurso, yaUsados);
    }

    // --- entonces ---

    private void entoncesElMotivoEs(MotivoReajuste esperadoMotivo) {
        assertThat(motivo).contains(esperadoMotivo);
    }

    private void entoncesNoSeReajusta() {
        assertThat(motivo).isEmpty();
    }

    private void entoncesLaDecisionNoTuvoEfecto() {
        assertThat(decisionSinEfecto).isTrue();
    }

    private void entoncesLaDecisionNoCuentaComoSinEfecto() {
        assertThat(decisionSinEfecto).isFalse();
    }

    private void entoncesLosUsadosSonElViejoEnCursoYElRecienteLogrado() {
        assertThat(usados).containsExactly(miniObjetivos.get(0), miniObjetivos.get(1));
    }

    private void entoncesElReemplazoEsElCandidatoLibre() {
        assertThat(reemplazo).contains(esperado);
    }

    private void entoncesElReemplazoEsElDeOtroTipo() {
        assertThat(reemplazo).contains(candidatos.get(1));
    }

    private void entoncesElReemplazoEsElDelMismoTipo() {
        assertThat(reemplazo).contains(candidatos.get(0));
    }

    private void entoncesNoHayReemplazo() {
        assertThat(reemplazo).isEmpty();
    }

    // --- datos ---

    private static MiniObjetivo otroEnCurso(TipoMiniObjetivo tipo) {
        MiniObjetivo otro = miniObjetivo();
        otro.setId(MiniObjetivoId.nuevo());
        otro.setTipo(tipo);
        otro.setRegistroId(null);
        otro.setReferencia(null);
        return otro;
    }
}

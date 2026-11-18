package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.CONSUMO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.ENTENDIDO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.OBJETIVO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.RESPUESTA_IA;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.catalogo;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.consulta;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.manual;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.medido;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.respuestaIa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PropuestaObjetivoTest {

    private static final String METRICA_C1 = "c1";
    private static final String METRICA_C2 = "c2";
    private static final String METRICA_C3 = "c3";
    private static final String METRICA_C4 = "c4";
    private static final String METRICA_FUERA_DEL_CATALOGO = "c9";
    private static final int LARGO_MAXIMO_DE_RESPUESTA = 600;
    private static final int LARGO_MAXIMO_DE_ENTENDIDO = 300;

    private PropuestaObjetivoIa respuestaDeLaIa;
    private PropuestaObjetivo propuesta;
    private Throwable error;

    @Test
    @DisplayName("Mezcla una métrica del catálogo con una acción: el objetivo sería híbrido")
    void mezclaUnaMetricaDelCatalogoConUnaAccionElObjetivoSeriaHibrido() {
        dadoUnaMetricaYUnaAccionValidas();

        cuandoSeArmaLaPropuesta();

        entoncesLaPropuestaTomaLosDatosDeLaIa();
        entoncesLosModosSonMedidoYManual();
        entoncesLaMetricaEsLaDelCatalogoYLaAccionLaDeLaIa();
        entoncesElTipoEs(TipoObjetivo.HIBRIDO);
        entoncesSePuedeGuardar();
    }

    @Test
    @DisplayName("Una métrica medida usa el texto y los números del servidor, nunca los de la IA")
    void unaMetricaMedidaUsaElTextoYLosNumerosDelServidorNuncaLosDeLaIa() {
        dadoUnaRespuestaConMetricas(METRICA_C2);

        cuandoSeArmaLaPropuesta();

        entoncesLaUnicaMetricaEsLaDelCatalogoSinTextoDeIa();
        entoncesElTipoEs(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Una métrica fuera del catálogo se descarta")
    void unaMetricaFueraDelCatalogoSeDescarta() {
        dadoUnaRespuestaConMetricas(METRICA_FUERA_DEL_CATALOGO, METRICA_C1);

        cuandoSeArmaLaPropuesta();

        entoncesLasMetricasSonLasDelCatalogo(0);
    }

    @Test
    @DisplayName("Dos métricas que miden lo mismo (mismo producto) se quedan en la primera")
    void dosMetricasQueMidenLoMismoMismoProductoSeQuedanEnLaPrimera() {
        dadoUnaRespuestaConMetricas(METRICA_C1, METRICA_C4, METRICA_C2);

        cuandoSeArmaLaPropuesta();

        entoncesLasMetricasSonLasDelCatalogo(0, 1);
    }

    @Test
    @DisplayName("La misma métrica dos veces se queda con una sola")
    void laMismaMetricaDosVecesSeQuedaConUnaSola() {
        dadoUnaRespuestaConMetricas(METRICA_C2, METRICA_C2);

        cuandoSeArmaLaPropuesta();

        entoncesHayMinis(1);
    }

    @Test
    @DisplayName("Una acción con un importe o un porcentaje, o sin texto, se descarta")
    void unaAccionConUnImporteOUnPorcentajeOSinTextoSeDescarta() {
        dadoUnaRespuestaConAccionesInvalidasYUnaValida();

        cuandoSeArmaLaPropuesta();

        entoncesSoloQuedaLaAccionValida();
    }

    @Test
    @DisplayName("Se quedan los primeros 3 mini-objetivos")
    void seQuedanLosPrimeros3MiniObjetivos() {
        dadoTresMetricasYUnaAccion();

        cuandoSeArmaLaPropuesta();

        entoncesQuedanElMaximoDeMinisTodosMedidos();
    }

    @Test
    @DisplayName("Una meta en pesos no arma mini-objetivos aunque la IA los mande, y no se puede guardar")
    void unaMetaEnPesosNoArmaMiniObjetivosAunqueLaIaLosMandeYNoSePuedeGuardar() {
        dadoUnaMetaEnPesosConMetricas();

        cuandoSeArmaLaPropuesta();

        entoncesLaPropuestaEsMonetariaSinMinisNiTipo();
        entoncesNoSePuedeGuardar();
    }

    @Test
    @DisplayName("Si no queda nada válido para guardar no hay tipo ni se puede guardar")
    void siNoQuedaNadaValidoParaGuardarNoHayTipoNiSePuedeGuardar() {
        dadoUnaRespuestaConMetricas(METRICA_FUERA_DEL_CATALOGO);

        cuandoSeArmaLaPropuesta();

        entoncesNoHayMinisNiTipo();
        entoncesNoSePuedeGuardar();
    }

    @Test
    @DisplayName("Sin respuesta para la persona, la IA no sirve")
    void sinRespuestaParaLaPersonaLaIaNoSirve() {
        dadoUnaRespuestaDeLaIaConTexto(null);

        cuandoSeArmaLaPropuestaConError();

        entoncesLaIaNoSirve();
    }

    @Test
    @DisplayName("Con una respuesta en blanco para la persona, la IA no sirve")
    void conUnaRespuestaEnBlancoParaLaPersonaLaIaNoSirve() {
        dadoUnaRespuestaDeLaIaConTexto("  ");

        cuandoSeArmaLaPropuestaConError();

        entoncesLaIaNoSirve();
    }

    @Test
    @DisplayName("Con una respuesta de más de 600 caracteres, la IA no sirve")
    void conUnaRespuestaDeMasDe600CaracteresLaIaNoSirve() {
        dadoUnaRespuestaDeLaIaConTexto("a".repeat(LARGO_MAXIMO_DE_RESPUESTA + 1));

        cuandoSeArmaLaPropuestaConError();

        entoncesLaIaNoSirveConElMensajeDeRespuestaInvalida();
    }

    @Test
    @DisplayName("Una respuesta de 600 caracteres se acepta y queda sin espacios en los extremos")
    void unaRespuestaDe600CaracteresSeAceptaYQuedaSinEspaciosEnLosExtremos() {
        dadoUnaRespuestaDeLaIaConTexto(" " + "a".repeat(LARGO_MAXIMO_DE_RESPUESTA) + " ");

        cuandoSeArmaLaPropuesta();

        entoncesLaRespuestaTieneElLargoMaximo();
    }

    @Test
    @DisplayName("Un «entendido» nulo queda sin entender")
    void unEntendidoNuloQuedaSinEntender() {
        dadoUnaRespuestaDeLaIaConEntendido(null);

        cuandoSeArmaLaPropuesta();

        entoncesElEntendidoEsNulo();
    }

    @Test
    @DisplayName("Un «entendido» vacío queda sin entender")
    void unEntendidoVacioQuedaSinEntender() {
        dadoUnaRespuestaDeLaIaConEntendido("");

        cuandoSeArmaLaPropuesta();

        entoncesElEntendidoEsNulo();
    }

    @Test
    @DisplayName("Un «entendido» de más de 300 caracteres queda sin entender")
    void unEntendidoDeMasDe300CaracteresQuedaSinEntender() {
        dadoUnaRespuestaDeLaIaConEntendido("a".repeat(LARGO_MAXIMO_DE_ENTENDIDO + 1));

        cuandoSeArmaLaPropuesta();

        entoncesElEntendidoEsNulo();
    }

    @Test
    @DisplayName("Un «entendido» válido queda sin espacios en los extremos")
    void unEntendidoValidoQuedaSinEspaciosEnLosExtremos() {
        dadoUnaRespuestaDeLaIaConEntendido("  " + ENTENDIDO + " ");

        cuandoSeArmaLaPropuesta();

        entoncesElEntendidoEs(ENTENDIDO);
    }

    @Test
    @DisplayName("El respaldo es un solo mini-objetivo manual de seguimiento con el objetivo de la persona")
    void elRespaldoEsUnSoloMiniObjetivoManualDeSeguimientoConElObjetivoDeLaPersona() {
        cuandoSeArmaElRespaldo();

        entoncesElRespaldoEsUnSeguimientoSemanal();
    }

    @Test
    @DisplayName("Una acción elegida antes no impide elegir después una métrica del catálogo")
    void unaAccionElegidaAntesNoImpideElegirDespuesUnaMetricaDelCatalogo() {
        dadoUnaAccionYDosMetricas();

        cuandoSeArmaLaPropuesta();

        entoncesLosModosSon(ModoMiniObjetivo.MANUAL, ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MEDIDO);
    }

    // --- arrange ---
    private void dadoUnaMetricaYUnaAccionValidas() {
        respuestaDeLaIa = respuestaIa(medido(METRICA_C3), manual(TITULO_ACCION, JUSTIFICACION_ACCION));
    }

    private void dadoUnaRespuestaConMetricas(String... metricas) {
        respuestaDeLaIa = respuestaIa(Arrays.stream(metricas).map(id -> medido(id)).toArray(EleccionIa[]::new));
    }

    private void dadoUnaRespuestaConAccionesInvalidasYUnaValida() {
        respuestaDeLaIa = respuestaIa(
                manual("Cobrá $5.000 más", JUSTIFICACION_ACCION),
                manual(TITULO_ACCION, "Vas a vender un 20 % más"),
                manual(null, null),
                manual(TITULO_ACCION, JUSTIFICACION_ACCION));
    }

    private void dadoTresMetricasYUnaAccion() {
        respuestaDeLaIa = respuestaIa(
                medido(METRICA_C1), medido(METRICA_C2), medido(METRICA_C3),
                manual(TITULO_ACCION, JUSTIFICACION_ACCION));
    }

    private void dadoUnaMetaEnPesosConMetricas() {
        respuestaDeLaIa = new PropuestaObjetivoIa(RESPUESTA_IA, ENTENDIDO, true, List.of(medido(METRICA_C1)), CONSUMO);
    }

    private void dadoUnaRespuestaDeLaIaConTexto(String respuesta) {
        respuestaDeLaIa = new PropuestaObjetivoIa(
                respuesta, ENTENDIDO, false, List.of(), new ConsumoIaSami("p", "m", 1, 1));
    }

    private void dadoUnaRespuestaDeLaIaConEntendido(String entendido) {
        respuestaDeLaIa = new PropuestaObjetivoIa(RESPUESTA_IA, entendido, false, List.of(), CONSUMO);
    }

    private void dadoUnaAccionYDosMetricas() {
        respuestaDeLaIa = respuestaIa(
                manual(TITULO_ACCION, JUSTIFICACION_ACCION), medido(METRICA_C1), medido(METRICA_C2));
    }

    // --- act ---
    private void cuandoSeArmaLaPropuesta() {
        propuesta = PropuestaObjetivo.de(respuestaDeLaIa, consulta());
    }

    private void cuandoSeArmaLaPropuestaConError() {
        error = catchThrowable(this::cuandoSeArmaLaPropuesta);
    }

    private void cuandoSeArmaElRespaldo() {
        propuesta = PropuestaObjetivo.respaldo(OBJETIVO);
    }

    // --- assert ---
    private void entoncesLaPropuestaTomaLosDatosDeLaIa() {
        assertThat(propuesta.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(propuesta.entendido()).isEqualTo(ENTENDIDO);
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(propuesta.esMonetario()).isFalse();
    }

    private void entoncesLosModosSonMedidoYManual() {
        entoncesLosModosSon(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
    }

    private void entoncesLosModosSon(ModoMiniObjetivo... modos) {
        assertThat(propuesta.minis()).extracting(MiniObjetivoPropuesto::modo).containsExactly(modos);
    }

    private void entoncesLaMetricaEsLaDelCatalogoYLaAccionLaDeLaIa() {
        assertThat(propuesta.minis().get(0).candidato()).isEqualTo(catalogo().get(2));
        assertThat(propuesta.minis().get(1).accion())
                .isEqualTo(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION));
        assertThat(propuesta.minis().get(1).textoConIa()).isTrue();
    }

    private void entoncesElTipoEs(TipoObjetivo tipo) {
        assertThat(propuesta.tipo()).contains(tipo);
    }

    private void entoncesSePuedeGuardar() {
        assertThat(propuesta.puedeGuardar()).isTrue();
    }

    private void entoncesNoSePuedeGuardar() {
        assertThat(propuesta.puedeGuardar()).isFalse();
    }

    private void entoncesLaUnicaMetricaEsLaDelCatalogoSinTextoDeIa() {
        assertThat(propuesta.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.candidato()).isEqualTo(catalogo().get(1));
            assertThat(mini.textoConIa()).isFalse();
        });
    }

    private void entoncesLasMetricasSonLasDelCatalogo(int... posiciones) {
        List<CandidatoMiniObjetivo> esperadas = Arrays.stream(posiciones).mapToObj(catalogo()::get).toList();
        assertThat(propuesta.minis()).extracting(MiniObjetivoPropuesto::candidato)
                .containsExactlyElementsOf(esperadas);
    }

    private void entoncesHayMinis(int cantidad) {
        assertThat(propuesta.minis()).hasSize(cantidad);
    }

    private void entoncesSoloQuedaLaAccionValida() {
        assertThat(propuesta.minis()).singleElement()
                .satisfies(mini -> assertThat(mini.accion().titulo()).isEqualTo(TITULO_ACCION));
    }

    private void entoncesQuedanElMaximoDeMinisTodosMedidos() {
        assertThat(propuesta.minis()).hasSize(MiniObjetivo.MAXIMO_EN_CURSO);
        assertThat(propuesta.minis()).allMatch(MiniObjetivoPropuesto::esMedido);
    }

    private void entoncesLaPropuestaEsMonetariaSinMinisNiTipo() {
        assertThat(propuesta.esMonetario()).isTrue();
        entoncesNoHayMinisNiTipo();
    }

    private void entoncesNoHayMinisNiTipo() {
        assertThat(propuesta.minis()).isEmpty();
        assertThat(propuesta.tipo()).isEmpty();
    }

    private void entoncesLaIaNoSirve() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesLaIaNoSirveConElMensajeDeRespuestaInvalida() {
        assertThat(error).isInstanceOf(IllegalStateException.class).hasMessage("La IA no dio una respuesta válida");
    }

    private void entoncesLaRespuestaTieneElLargoMaximo() {
        assertThat(propuesta.respuesta()).hasSize(LARGO_MAXIMO_DE_RESPUESTA);
    }

    private void entoncesElEntendidoEsNulo() {
        assertThat(propuesta.entendido()).isNull();
    }

    private void entoncesElEntendidoEs(String esperado) {
        assertThat(propuesta.entendido()).isEqualTo(esperado);
    }

    private void entoncesElRespaldoEsUnSeguimientoSemanal() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(propuesta.entendido()).isNull();
        assertThat(propuesta.esMonetario()).isFalse();
        assertThat(propuesta.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.esManual()).isTrue();
            assertThat(mini.textoConIa()).isFalse();
            assertThat(mini.accion().justificacion()).contains(OBJETIVO);
        });
        entoncesElTipoEs(TipoObjetivo.SIN_DATOS);
        entoncesSePuedeGuardar();
    }
}

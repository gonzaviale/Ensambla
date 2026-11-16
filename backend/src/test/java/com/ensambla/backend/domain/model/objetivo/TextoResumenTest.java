package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static org.assertj.core.api.Assertions.assertThat;

class TextoResumenTest {

    private static final BigDecimal RESULTADO = new BigDecimal("180000");
    private static final BigDecimal META = new BigDecimal("500000");
    private static final BigDecimal RESULTADO_ANTERIOR_MENOR = new BigDecimal("175900");
    private static final BigDecimal RESULTADO_ANTERIOR_MAYOR = new BigDecimal("182000");
    private static final ConteoDecisiones NINGUNA = new ConteoDecisiones(0, 0, 0, 0);
    private static final String TITULO_NUEVO = "Vender más";

    private String texto;
    private MiniObjetivo anterior;
    private MiniObjetivo nuevo;

    @Test
    @DisplayName("La primera semana cuenta el resultado, que no hubo decisiones y cuánto falta")
    void queFuncionoShouldTellTheResultWhenThereIsNoPreviousWeek() {
        whenSeResumeLaSemana(Optional.empty(), NINGUNA, META, false, false);

        thenElTextoEs("Al cerrar la semana, lo que te queda de los últimos 30 días es $180.000. "
                + "No tomaste decisiones sobre tu objetivo. Te faltan $320.000 para tu meta.");
    }

    @Test
    @DisplayName("Si el resultado subió dice cuánto y lista las decisiones")
    void queFuncionoShouldTellHowMuchTheResultRose() {
        whenSeResumeLaSemana(Optional.of(RESULTADO_ANTERIOR_MENOR), new ConteoDecisiones(1, 1, 0, 0), META, false,
                false);

        thenElTextoEs("Esta semana tu resultado subió $4.100: de $175.900 a $180.000. "
                + "Aplicaste 1 aviso y descartaste 1 aviso. Te faltan $320.000 para tu meta.");
    }

    @Test
    @DisplayName("Si el resultado bajó lo dice y, con la meta alcanzada, sugiere subirla")
    void queFuncionoShouldTellTheResultFellAndCelebrateTheGoal() {
        whenSeResumeLaSemana(Optional.of(RESULTADO_ANTERIOR_MAYOR), new ConteoDecisiones(2, 1, 1, 3), META, true,
                false);

        thenElTextoEs("Esta semana tu resultado bajó $2.000: de $182.000 a $180.000. "
                + "Aplicaste 2 avisos, descartaste 1 aviso, aceptaste 1 paso y dejaste para después 3 pasos. "
                + "¡Llegaste a tu meta! Podés subirla para seguir creciendo.");
    }

    @Test
    @DisplayName("Si el resultado no cambió y no hubo ventas, avisa que no se cambiaron los mini-objetivos")
    void queFuncionoShouldTellNothingChangedWhenThereWereNoSales() {
        whenSeResumeLaSemana(Optional.of(RESULTADO), new ConteoDecisiones(0, 0, 0, 1), META, false, true);

        thenElTextoEs("Esta semana tu resultado se mantuvo en $180.000. Dejaste para después 1 paso. "
                + "No hubo ventas esta semana, así que no cambiamos tus mini-objetivos.");
    }

    @Test
    @DisplayName("Sin meta en pesos no dice cuánto falta y sigue con los mini-objetivos")
    void queFuncionoShouldNotTellWhatIsMissingWithoutGoal() {
        whenSeResumeLaSemana(Optional.empty(), NINGUNA, null, false, false);

        thenElTextoNoDiceCuantoFalta();
        thenElTextoTerminaCon("Seguí con tus mini-objetivos de la semana que viene.");
    }

    @Test
    @DisplayName("Explica que quitaste un mini-objetivo y con cuál lo reemplazamos")
    void motivoShouldExplainARemovedMini() {
        givenUnAnteriorYUnNuevo();

        whenSeExplicaElMotivo(MotivoReajuste.QUITADO, BigDecimal.ZERO);

        thenElTextoEs("Quitaste «Subile $250 a Yerba 1 kg». En su lugar te proponemos «Vender más».");
    }

    @Test
    @DisplayName("Explica que el producto ya no está en el cuaderno o le falta el precio o el costo")
    void motivoShouldExplainAProductThatLeft() {
        givenUnAnteriorYUnNuevo();

        whenSeExplicaElMotivo(MotivoReajuste.PRODUCTO_FUERA, BigDecimal.ZERO);

        thenElTextoEs("«Yerba 1 kg» ya no está en tu cuaderno o le falta el precio o el costo. "
                + "Lo cambiamos por «Vender más».");
    }

    @Test
    @DisplayName("Explica que la decisión aplicada no hizo subir las ventas")
    void motivoShouldExplainADecisionWithoutEffect() {
        givenUnAnteriorYUnNuevo();

        whenSeExplicaElMotivo(MotivoReajuste.DECISION_SIN_EFECTO, BigDecimal.ZERO);

        thenElTextoEs("Aplicaste un aviso para «Subile $250 a Yerba 1 kg» y las ventas no subieron. "
                + "Probamos con «Vender más».");
    }

    @Test
    @DisplayName("Explica que hace semanas que el mini-objetivo no avanza")
    void motivoShouldExplainAMiniWithoutProgress() {
        givenUnAnteriorYUnNuevo();

        whenSeExplicaElMotivo(MotivoReajuste.SIN_AVANCE, BigDecimal.ZERO);

        thenElTextoEs("Hace 2 semanas que «Subile $250 a Yerba 1 kg» no avanza. Lo cambiamos por «Vender más».");
    }

    @Test
    @DisplayName("Uno logrado que ya no puede pedir más deja su lugar a otro")
    void motivoShouldExplainAnAchievedMiniObjetivoThatCannotAskForMore() {
        givenUnAnteriorLogradoDeMargen();

        whenSeExplicaElMotivo(MotivoReajuste.LOGRADO, new BigDecimal("60"));

        thenElTextoEs("Lo lograste: llegaste a 60 %. Como ya no se puede pedir más, ahora probamos con "
                + "«Vender más».");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("La meta subida se dice en pesos, unidades o puntos según el tipo")
    @CsvSource(delimiter = '|', textBlock = """
            SUBIR_PRECIO    | 14300 | 15883 | Lo lograste: llegaste a $14.300 por semana. Ahora el objetivo es $15.883 por semana.
            RECORTAR_GASTO  | 35000 | 31000 | Lo lograste: llegaste a $35.000 en el mes. Ahora el objetivo es $31.000 en el mes.
            MOVER_PARADO    | 4     | 6     | Lo lograste: llegaste a 4 por semana. Ahora el objetivo es 6 por semana.
            MEJORAR_MARGEN  | 37.7  | 39.7  | Lo lograste: llegaste a 37,7 %. Ahora el objetivo es 39,7 %.
            """)
    void metaSubidaShouldUseTheUnitOfEachType(TipoMiniObjetivo tipo, BigDecimal lograda, BigDecimal nueva,
                                              String esperado) {
        whenSeDiceLaMetaSubida(tipo, lograda, nueva);

        thenElTextoEs(esperado);
    }

    @Test
    @DisplayName("La meta subida de los productos bajo el mínimo se dice en esa unidad")
    void metaSubidaShouldSpeakTheUnitOfProductsUnderTheMinimum() {
        whenSeDiceLaMetaSubida(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO, BigDecimal.ZERO, BigDecimal.ZERO);

        thenElTextoContiene("0 productos bajo el mínimo");
    }

    @Test
    @DisplayName("La meta subida del capital parado se dice en pesos de mercadería parada")
    void metaSubidaShouldSpeakTheUnitOfStuckCapital() {
        whenSeDiceLaMetaSubida(TipoMiniObjetivo.CAPITAL_PARADO, new BigDecimal("24000"), new BigDecimal("18000"));

        thenElTextoContiene("$24.000 en mercadería parada", "$18.000 en mercadería parada");
    }

    @Test
    @DisplayName("La meta subida de la variedad vendida se dice en productos distintos por semana")
    void metaSubidaShouldSpeakTheUnitOfSoldVariety() {
        whenSeDiceLaMetaSubida(TipoMiniObjetivo.VARIEDAD_VENDIDA, new BigDecimal("3"), new BigDecimal("4"));

        thenElTextoContiene("3 productos distintos por semana", "4 productos distintos por semana");
    }

    @Test
    @DisplayName("Sin mini-objetivos manuales el resumen no dice nada de ellos")
    void manualesShouldBeEmptyWithoutManualMinis() {
        whenSeResumenLosManuales(List.of());

        thenElTextoEstaVacio();
    }

    @Test
    @DisplayName("Cuenta lo que la persona hizo, hizo en parte y no hizo")
    void manualesShouldCountTheAnswers() {
        whenSeResumenLosManuales(List.of(
                new ManualCierre(MINI_ID, "Uno", HizoManual.SI, QuePaso.FUNCIONO),
                new ManualCierre(OTRO_MINI_ID, "Dos", HizoManual.SI, QuePaso.OTRA),
                new ManualCierre(MiniObjetivoId.nuevo(), "Tres", HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO),
                new ManualCierre(MiniObjetivoId.nuevo(), "Cuatro", HizoManual.NO, QuePaso.NO_FUNCIONO)));

        thenElTextoEs("De tus acciones respondiste: 2 hechas, 1 en parte y 1 sin hacer.");
    }

    @Test
    @DisplayName("Dice qué acciones quedaron sin respuesta y que no cambian hasta que responda")
    void manualesShouldTellWhichOnesHaveNoAnswer() {
        whenSeResumenLosManuales(List.of(
                new ManualCierre(MINI_ID, "Uno", HizoManual.SI, QuePaso.FUNCIONO),
                new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null)));

        thenElTextoEs("De tus acciones respondiste: 1 hechas, 0 en parte y 0 sin hacer. "
                + "No me contaste cómo te fue con «Un cartel»: queda igual hasta que respondas.");
    }

    @Test
    @DisplayName("Si ninguna se respondió sólo dice cuáles esperan la respuesta")
    void manualesShouldOnlyListTheMissingAnswers() {
        whenSeResumenLosManuales(List.of(new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null)));

        thenElTextoEs("No me contaste cómo te fue con «Un cartel»: queda igual hasta que respondas.");
    }

    // --- arrange ---
    private void givenUnAnteriorYUnNuevo() {
        anterior = miniObjetivo();
        nuevo = miniNuevo();
    }

    private void givenUnAnteriorLogradoDeMargen() {
        givenUnAnteriorYUnNuevo();
        anterior.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    // --- act ---
    private void whenSeResumeLaSemana(Optional<BigDecimal> anteriorResultado, ConteoDecisiones decisiones,
                                      BigDecimal meta, boolean llegoALaMeta, boolean sinVentas) {
        texto = TextoResumen.queFunciono(RESULTADO, anteriorResultado, decisiones, meta, llegoALaMeta, sinVentas);
    }

    private void whenSeExplicaElMotivo(MotivoReajuste motivo, BigDecimal valor) {
        texto = TextoResumen.motivo(motivo, anterior, valor, nuevo);
    }

    private void whenSeDiceLaMetaSubida(TipoMiniObjetivo tipo, BigDecimal lograda, BigDecimal nueva) {
        texto = TextoResumen.metaSubida(tipo, lograda, nueva);
    }

    private void whenSeResumenLosManuales(List<ManualCierre> manuales) {
        texto = TextoResumen.manuales(manuales);
    }

    // --- assert ---
    private void thenElTextoEs(String esperado) {
        assertThat(texto).isEqualTo(esperado);
    }

    private void thenElTextoContiene(String... fragmentos) {
        assertThat(texto).contains(fragmentos);
    }

    private void thenElTextoNoDiceCuantoFalta() {
        assertThat(texto).doesNotContain("Te faltan");
    }

    private void thenElTextoTerminaCon(String cierre) {
        assertThat(texto).endsWith(cierre);
    }

    private void thenElTextoEstaVacio() {
        assertThat(texto).isEmpty();
    }

    // --- helpers ---
    private static MiniObjetivo miniNuevo() {
        MiniObjetivo mini = miniObjetivo();
        mini.setTitulo(TITULO_NUEVO);
        return mini;
    }
}

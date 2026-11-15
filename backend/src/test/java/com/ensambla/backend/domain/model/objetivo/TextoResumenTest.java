package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    private static final ConteoDecisiones NINGUNA = new ConteoDecisiones(0, 0, 0, 0);

    @Test
    @DisplayName("La primera semana cuenta el resultado, que no hubo decisiones y cuánto falta")
    void queFuncionoShouldTellTheResultWhenThereIsNoPreviousWeek() {
        String result = TextoResumen.queFunciono(RESULTADO, Optional.empty(), NINGUNA, META, false, false);

        assertThat(result).isEqualTo("Al cerrar la semana, lo que te queda de los últimos 30 días es $180.000. "
                + "No tomaste decisiones sobre tu objetivo. Te faltan $320.000 para tu meta.");
    }

    @Test
    @DisplayName("Si el resultado subió dice cuánto y lista las decisiones")
    void queFuncionoShouldTellHowMuchTheResultRose() {
        String result = TextoResumen.queFunciono(RESULTADO, Optional.of(new BigDecimal("175900")),
                new ConteoDecisiones(1, 1, 0, 0), META, false, false);

        assertThat(result).isEqualTo("Esta semana tu resultado subió $4.100: de $175.900 a $180.000. "
                + "Aplicaste 1 aviso y descartaste 1 aviso. Te faltan $320.000 para tu meta.");
    }

    @Test
    @DisplayName("Si el resultado bajó lo dice y, con la meta alcanzada, sugiere subirla")
    void queFuncionoShouldTellTheResultFellAndCelebrateTheGoal() {
        String result = TextoResumen.queFunciono(RESULTADO, Optional.of(new BigDecimal("182000")),
                new ConteoDecisiones(2, 1, 1, 3), META, true, false);

        assertThat(result).isEqualTo("Esta semana tu resultado bajó $2.000: de $182.000 a $180.000. "
                + "Aplicaste 2 avisos, descartaste 1 aviso, aceptaste 1 paso y dejaste para después 3 pasos. "
                + "¡Llegaste a tu meta! Podés subirla para seguir creciendo.");
    }

    @Test
    @DisplayName("Si el resultado no cambió y no hubo ventas, avisa que no se cambiaron los mini-objetivos")
    void queFuncionoShouldTellNothingChangedWhenThereWereNoSales() {
        String result = TextoResumen.queFunciono(RESULTADO, Optional.of(RESULTADO), new ConteoDecisiones(0, 0, 0, 1),
                META, false, true);

        assertThat(result).isEqualTo("Esta semana tu resultado se mantuvo en $180.000. Dejaste para después 1 paso. "
                + "No hubo ventas esta semana, así que no cambiamos tus mini-objetivos.");
    }

    @Test
    @DisplayName("Explica cada motivo de reajuste con los títulos de los dos mini-objetivos")
    void motivoShouldExplainEachReason() {
        MiniObjetivo anterior = miniObjetivo();
        MiniObjetivo nuevo = nuevo();

        assertThat(TextoResumen.motivo(MotivoReajuste.QUITADO, anterior, BigDecimal.ZERO, nuevo))
                .isEqualTo("Quitaste «Subile $250 a Yerba 1 kg». En su lugar te proponemos «Vender más».");
        assertThat(TextoResumen.motivo(MotivoReajuste.PRODUCTO_FUERA, anterior, BigDecimal.ZERO, nuevo))
                .isEqualTo("«Yerba 1 kg» ya no está en tu cuaderno o le falta el precio o el costo. "
                        + "Lo cambiamos por «Vender más».");
        assertThat(TextoResumen.motivo(MotivoReajuste.DECISION_SIN_EFECTO, anterior, BigDecimal.ZERO, nuevo))
                .isEqualTo("Aplicaste un aviso para «Subile $250 a Yerba 1 kg» y las ventas no subieron. "
                        + "Probamos con «Vender más».");
        assertThat(TextoResumen.motivo(MotivoReajuste.SIN_AVANCE, anterior, BigDecimal.ZERO, nuevo))
                .isEqualTo("Hace 2 semanas que «Subile $250 a Yerba 1 kg» no avanza. Lo cambiamos por «Vender más».");
    }

    @Test
    @DisplayName("Uno logrado que ya no puede pedir más deja su lugar a otro")
    void motivoShouldExplainAnAchievedMiniObjetivoThatCannotAskForMore() {
        MiniObjetivo anterior = miniObjetivo();
        anterior.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);

        assertThat(TextoResumen.motivo(MotivoReajuste.LOGRADO, anterior, new BigDecimal("60"), nuevo()))
                .isEqualTo("Lo lograste: llegaste a 60 %. Como ya no se puede pedir más, ahora probamos con "
                        + "«Vender más».");
    }

    @Test
    @DisplayName("La meta subida se dice en pesos, unidades o puntos según el tipo")
    void metaSubidaShouldUseTheUnitOfEachType() {
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.SUBIR_PRECIO, new BigDecimal("14300"),
                new BigDecimal("15883"))).isEqualTo(
                "Lo lograste: llegaste a $14.300 por semana. Ahora el objetivo es $15.883 por semana.");
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.RECORTAR_GASTO, new BigDecimal("35000"),
                new BigDecimal("31000"))).isEqualTo(
                "Lo lograste: llegaste a $35.000 en el mes. Ahora el objetivo es $31.000 en el mes.");
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.MOVER_PARADO, new BigDecimal("4"),
                new BigDecimal("6")))
                .isEqualTo("Lo lograste: llegaste a 4 por semana. Ahora el objetivo es 6 por semana.");
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.MEJORAR_MARGEN, new BigDecimal("37.7"),
                new BigDecimal("39.7"))).isEqualTo("Lo lograste: llegaste a 37,7 %. Ahora el objetivo es 39,7 %.");
    }

    // --- helpers ---
    private static MiniObjetivo nuevo() {
        MiniObjetivo nuevo = miniObjetivo();
        nuevo.setTitulo("Vender más");
        return nuevo;
    }

    @Test
    @DisplayName("Sin meta en pesos no dice cuánto falta y sigue con los mini-objetivos")
    void queFuncionoShouldNotTellWhatIsMissingWithoutGoal() {
        String result = TextoResumen.queFunciono(RESULTADO, Optional.empty(), NINGUNA, null, false, false);

        assertThat(result).doesNotContain("Te faltan").endsWith("Seguí con tus mini-objetivos de la semana que viene.");
    }

    @Test
    @DisplayName("La meta subida de las métricas nuevas se dice en sus unidades")
    void metaSubidaShouldSpeakTheUnitsOfTheNewMetrics() {
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO, new BigDecimal("0"),
                new BigDecimal("0"))).contains("0 productos bajo el mínimo");
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.CAPITAL_PARADO, new BigDecimal("24000"),
                new BigDecimal("18000"))).contains("$24.000 en mercadería parada", "$18.000 en mercadería parada");
        assertThat(TextoResumen.metaSubida(TipoMiniObjetivo.VARIEDAD_VENDIDA, new BigDecimal("3"),
                new BigDecimal("4"))).contains("3 productos distintos por semana", "4 productos distintos por semana");
    }

    @Test
    @DisplayName("Sin mini-objetivos manuales el resumen no dice nada de ellos")
    void manualesShouldBeEmptyWithoutManualMinis() {
        assertThat(TextoResumen.manuales(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Cuenta lo que la persona hizo, hizo en parte y no hizo")
    void manualesShouldCountTheAnswers() {
        String result = TextoResumen.manuales(List.of(
                new ManualCierre(MINI_ID, "Uno", HizoManual.SI, QuePaso.FUNCIONO),
                new ManualCierre(OTRO_MINI_ID, "Dos", HizoManual.SI, QuePaso.OTRA),
                new ManualCierre(MiniObjetivoId.nuevo(), "Tres", HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO),
                new ManualCierre(MiniObjetivoId.nuevo(), "Cuatro", HizoManual.NO, QuePaso.NO_FUNCIONO)));

        assertThat(result).isEqualTo("De tus acciones respondiste: 2 hechas, 1 en parte y 1 sin hacer.");
    }

    @Test
    @DisplayName("Dice qué acciones quedaron sin respuesta y que no cambian hasta que responda")
    void manualesShouldTellWhichOnesHaveNoAnswer() {
        String result = TextoResumen.manuales(List.of(
                new ManualCierre(MINI_ID, "Uno", HizoManual.SI, QuePaso.FUNCIONO),
                new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null)));

        assertThat(result).isEqualTo("De tus acciones respondiste: 1 hechas, 0 en parte y 0 sin hacer. "
                + "No me contaste cómo te fue con «Un cartel»: queda igual hasta que respondas.");
    }

    @Test
    @DisplayName("Si ninguna se respondió sólo dice cuáles esperan la respuesta")
    void manualesShouldOnlyListTheMissingAnswers() {
        String result = TextoResumen.manuales(List.of(new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null)));

        assertThat(result).isEqualTo("No me contaste cómo te fue con «Un cartel»: queda igual hasta que respondas.");
    }
}

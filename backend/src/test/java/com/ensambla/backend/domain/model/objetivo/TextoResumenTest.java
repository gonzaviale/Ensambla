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
    private static final ConteoDecisiones UNA_APLICADA_Y_UNA_DESCARTADA = new ConteoDecisiones(1, 1, 0, 0);
    private static final ConteoDecisiones VARIAS_DECISIONES = new ConteoDecisiones(2, 1, 1, 3);
    private static final ConteoDecisiones UNA_POSTERGADA = new ConteoDecisiones(0, 0, 0, 1);
    private static final BigDecimal MARGEN_LOGRADO = new BigDecimal("60");
    private static final BigDecimal CAPITAL_PARADO_LOGRADO = new BigDecimal("24000");
    private static final BigDecimal CAPITAL_PARADO_NUEVO = new BigDecimal("18000");
    private static final BigDecimal VARIEDAD_LOGRADA = new BigDecimal("3");
    private static final BigDecimal VARIEDAD_NUEVA = new BigDecimal("4");
    private static final String TITULO_NUEVO = "Vender más";

    private String texto;
    private MiniObjetivo anterior;
    private MiniObjetivo nuevo;

    @Test
    @DisplayName("La primera semana cuenta el resultado, que no hubo decisiones y cuánto falta")
    void laPrimeraSemanaCuentaElResultadoQueNoHuboDecisionesYCuantoFalta() {
        cuandoSeResumeLaSemana(Optional.empty(), NINGUNA, META, false, false);

        entoncesElTextoEs("Al cerrar la semana, lo que te queda de los últimos 30 días es $180.000. "
                + "No tomaste decisiones sobre tu objetivo. Te faltan $320.000 para tu meta.");
    }

    @Test
    @DisplayName("Si el resultado subió dice cuánto y lista las decisiones")
    void siElResultadoSubioDiceCuantoYListaLasDecisiones() {
        cuandoSeResumeLaSemana(Optional.of(RESULTADO_ANTERIOR_MENOR), UNA_APLICADA_Y_UNA_DESCARTADA, META, false,
                false);

        entoncesElTextoEs("Esta semana tu resultado subió $4.100: de $175.900 a $180.000. "
                + "Aplicaste 1 aviso y descartaste 1 aviso. Te faltan $320.000 para tu meta.");
    }

    @Test
    @DisplayName("Si el resultado bajó lo dice y, con la meta alcanzada, sugiere subirla")
    void siElResultadoBajoLoDiceYConLaMetaAlcanzadaSugiereSubirla() {
        cuandoSeResumeLaSemana(Optional.of(RESULTADO_ANTERIOR_MAYOR), VARIAS_DECISIONES, META, true,
                false);

        entoncesElTextoEs("Esta semana tu resultado bajó $2.000: de $182.000 a $180.000. "
                + "Aplicaste 2 avisos, descartaste 1 aviso, aceptaste 1 paso y dejaste para después 3 pasos. "
                + "¡Llegaste a tu meta! Podés subirla para seguir creciendo.");
    }

    @Test
    @DisplayName("Si el resultado no cambió y no hubo ventas, avisa que no se cambiaron los mini-objetivos")
    void siElResultadoNoCambioYNoHuboVentasAvisaQueNoSeCambiaronLosMiniObjetivos() {
        cuandoSeResumeLaSemana(Optional.of(RESULTADO), UNA_POSTERGADA, META, false, true);

        entoncesElTextoEs("Esta semana tu resultado se mantuvo en $180.000. Dejaste para después 1 paso. "
                + "No hubo ventas esta semana, así que no cambiamos tus mini-objetivos.");
    }

    @Test
    @DisplayName("Sin meta en pesos no dice cuánto falta y sigue con los mini-objetivos")
    void sinMetaEnPesosNoDiceCuantoFaltaYSigueConLosMiniObjetivos() {
        cuandoSeResumeLaSemana(Optional.empty(), NINGUNA, null, false, false);

        entoncesElTextoNoDiceCuantoFalta();
        entoncesElTextoTerminaCon("Seguí con tus mini-objetivos de la semana que viene.");
    }

    @Test
    @DisplayName("Explica que quitaste un mini-objetivo y con cuál lo reemplazamos")
    void explicaQueQuitasteUnMiniObjetivoYConCualLoReemplazamos() {
        dadoUnAnteriorYUnNuevo();

        cuandoSeExplicaElMotivo(MotivoReajuste.QUITADO, BigDecimal.ZERO);

        entoncesElTextoEs("Quitaste «Subile $250 a Yerba 1 kg». En su lugar te proponemos «Vender más».");
    }

    @Test
    @DisplayName("Explica que el producto ya no está en el cuaderno o le falta el precio o el costo")
    void explicaQueElProductoYaNoEstaEnElCuadernoOLeFaltaElPrecioOElCosto() {
        dadoUnAnteriorYUnNuevo();

        cuandoSeExplicaElMotivo(MotivoReajuste.PRODUCTO_FUERA, BigDecimal.ZERO);

        entoncesElTextoEs("«Yerba 1 kg» ya no está en tus secciones o le falta el precio o el costo. "
                + "Lo cambiamos por «Vender más».");
    }

    @Test
    @DisplayName("Explica que la decisión aplicada no hizo subir las ventas")
    void explicaQueLaDecisionAplicadaNoHizoSubirLasVentas() {
        dadoUnAnteriorYUnNuevo();

        cuandoSeExplicaElMotivo(MotivoReajuste.DECISION_SIN_EFECTO, BigDecimal.ZERO);

        entoncesElTextoEs("Aplicaste un aviso para «Subile $250 a Yerba 1 kg» y las ventas no subieron. "
                + "Probamos con «Vender más».");
    }

    @Test
    @DisplayName("Explica que hace semanas que el mini-objetivo no avanza")
    void explicaQueHaceSemanasQueElMiniObjetivoNoAvanza() {
        dadoUnAnteriorYUnNuevo();

        cuandoSeExplicaElMotivo(MotivoReajuste.SIN_AVANCE, BigDecimal.ZERO);

        entoncesElTextoEs("Hace 2 semanas que «Subile $250 a Yerba 1 kg» no avanza. Lo cambiamos por «Vender más».");
    }

    @Test
    @DisplayName("Uno logrado que ya no puede pedir más deja su lugar a otro")
    void unoLogradoQueYaNoPuedePedirMasDejaSuLugarAOtro() {
        dadoUnAnteriorLogradoDeMargen();

        cuandoSeExplicaElMotivo(MotivoReajuste.LOGRADO, MARGEN_LOGRADO);

        entoncesElTextoEs("Lo lograste: llegaste a 60 %. Como ya no se puede pedir más, ahora probamos con "
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
    void laMetaSubidaSeDiceEnPesosUnidadesOPuntosSegunElTipo(TipoMiniObjetivo tipo, BigDecimal lograda, BigDecimal nueva,
                                              String esperado) {
        cuandoSeDiceLaMetaSubida(tipo, lograda, nueva);

        entoncesElTextoEs(esperado);
    }

    @Test
    @DisplayName("La meta subida de los productos bajo el mínimo se dice en esa unidad")
    void laMetaSubidaDeLosProductosBajoElMinimoSeDiceEnEsaUnidad() {
        cuandoSeDiceLaMetaSubida(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO, BigDecimal.ZERO, BigDecimal.ZERO);

        entoncesElTextoContiene("0 productos bajo el mínimo");
    }

    @Test
    @DisplayName("La meta subida del capital parado se dice en pesos de mercadería parada")
    void laMetaSubidaDelCapitalParadoSeDiceEnPesosDeMercaderiaParada() {
        cuandoSeDiceLaMetaSubida(TipoMiniObjetivo.CAPITAL_PARADO, CAPITAL_PARADO_LOGRADO, CAPITAL_PARADO_NUEVO);

        entoncesElTextoContiene("$24.000 en mercadería parada", "$18.000 en mercadería parada");
    }

    @Test
    @DisplayName("La meta subida de la variedad vendida se dice en productos distintos por semana")
    void laMetaSubidaDeLaVariedadVendidaSeDiceEnProductosDistintosPorSemana() {
        cuandoSeDiceLaMetaSubida(TipoMiniObjetivo.VARIEDAD_VENDIDA, VARIEDAD_LOGRADA, VARIEDAD_NUEVA);

        entoncesElTextoContiene("3 productos distintos por semana", "4 productos distintos por semana");
    }

    @Test
    @DisplayName("Sin mini-objetivos manuales el resumen no dice nada de ellos")
    void sinMiniObjetivosManualesElResumenNoDiceNadaDeEllos() {
        cuandoSeResumenLosManuales(List.of());

        entoncesElTextoEstaVacio();
    }

    @Test
    @DisplayName("Cuenta lo que la persona hizo, hizo en parte y no hizo")
    void cuentaLoQueLaPersonaHizoHizoEnParteYNoHizo() {
        cuandoSeResumenLosManuales(cuatroManualesRespondidos());

        entoncesElTextoEs("De tus acciones respondiste: 2 hechas, 1 en parte y 1 sin hacer.");
    }

    @Test
    @DisplayName("Dice qué acciones quedaron sin respuesta y que no cambian hasta que responda")
    void diceQueAccionesQuedaronSinRespuestaYQueNoCambianHastaQueResponda() {
        cuandoSeResumenLosManuales(unManualRespondidoYUnoSinRespuesta());

        entoncesElTextoEs("De tus acciones respondiste: 1 hechas, 0 en parte y 0 sin hacer. "
                + "No me contaste cómo te fue con «Un cartel»: queda igual hasta que respondas.");
    }

    @Test
    @DisplayName("Si ninguna se respondió sólo dice cuáles esperan la respuesta")
    void siNingunaSeRespondioSoloDiceCualesEsperanLaRespuesta() {
        cuandoSeResumenLosManuales(soloUnManualSinRespuesta());

        entoncesElTextoEs("No me contaste cómo te fue con «Un cartel»: queda igual hasta que respondas.");
    }

    // --- arrange ---
    private void dadoUnAnteriorYUnNuevo() {
        anterior = miniObjetivo();
        nuevo = miniNuevo();
    }

    private void dadoUnAnteriorLogradoDeMargen() {
        dadoUnAnteriorYUnNuevo();
        anterior.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    // --- act ---
    private void cuandoSeResumeLaSemana(Optional<BigDecimal> anteriorResultado, ConteoDecisiones decisiones,
                                      BigDecimal meta, boolean llegoALaMeta, boolean sinVentas) {
        texto = TextoResumen.queFunciono(RESULTADO, anteriorResultado, decisiones, meta, llegoALaMeta, sinVentas);
    }

    private void cuandoSeExplicaElMotivo(MotivoReajuste motivo, BigDecimal valor) {
        texto = TextoResumen.motivo(motivo, anterior, valor, nuevo);
    }

    private void cuandoSeDiceLaMetaSubida(TipoMiniObjetivo tipo, BigDecimal lograda, BigDecimal nueva) {
        texto = TextoResumen.metaSubida(tipo, lograda, nueva);
    }

    private void cuandoSeResumenLosManuales(List<ManualCierre> manuales) {
        texto = TextoResumen.manuales(manuales);
    }

    // --- assert ---
    private void entoncesElTextoEs(String esperado) {
        assertThat(texto).isEqualTo(esperado);
    }

    private void entoncesElTextoContiene(String... fragmentos) {
        assertThat(texto).contains(fragmentos);
    }

    private void entoncesElTextoNoDiceCuantoFalta() {
        assertThat(texto).doesNotContain("Te faltan");
    }

    private void entoncesElTextoTerminaCon(String cierre) {
        assertThat(texto).endsWith(cierre);
    }

    private void entoncesElTextoEstaVacio() {
        assertThat(texto).isEmpty();
    }

    // --- helpers ---
    private static MiniObjetivo miniNuevo() {
        MiniObjetivo mini = miniObjetivo();
        mini.setTitulo(TITULO_NUEVO);
        return mini;
    }

    private static List<ManualCierre> cuatroManualesRespondidos() {
        return List.of(
                new ManualCierre(MINI_ID, "Uno", HizoManual.SI, QuePaso.FUNCIONO),
                new ManualCierre(OTRO_MINI_ID, "Dos", HizoManual.SI, QuePaso.OTRA),
                new ManualCierre(MiniObjetivoId.nuevo(), "Tres", HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO),
                new ManualCierre(MiniObjetivoId.nuevo(), "Cuatro", HizoManual.NO, QuePaso.NO_FUNCIONO));
    }

    private static List<ManualCierre> unManualRespondidoYUnoSinRespuesta() {
        return List.of(
                new ManualCierre(MINI_ID, "Uno", HizoManual.SI, QuePaso.FUNCIONO),
                new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null));
    }

    private static List<ManualCierre> soloUnManualSinRespuesta() {
        return List.of(new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null));
    }
}

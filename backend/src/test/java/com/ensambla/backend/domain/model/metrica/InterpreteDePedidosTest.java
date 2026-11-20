package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CATEGORIA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CLIENTE;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.DIA_DE_LA_SEMANA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.PRODUCTO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.CONTEO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.COSTO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.GANANCIA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.PROMEDIO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.SUMA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.UNIDADES;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.GASTOS;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.INVENTARIO;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.VENTAS;
import static com.ensambla.backend.domain.model.metrica.ParteDeLaReceta.AGRUPACION;
import static com.ensambla.backend.domain.model.metrica.ParteDeLaReceta.CALCULO;
import static com.ensambla.backend.domain.model.metrica.ParteDeLaReceta.FUENTE;
import static com.ensambla.backend.domain.model.metrica.ParteDeLaReceta.PERIODO;
import static com.ensambla.backend.domain.model.metrica.ParteDeLaReceta.SOLO_FIADO;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.HOY;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.MES;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.SEMANA;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.TODO;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static com.ensambla.backend.support.PedidosDePrueba.elInterpreteDeLaAplicacion;
import static com.ensambla.backend.support.PedidosDePrueba.unDiccionarioQueSoloConoceLasVentas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class InterpreteDePedidosTest {

    private InterpreteDePedidos interprete;

    private PedidoInterpretado interpretado;

    private Throwable rechazo;

    static Stream<Arguments> pedidosDeUnAlmacen() {
        return Stream.of(
                arguments("¿Cuánto vendí esta semana?",
                        unaReceta("Cuánto vendí esta semana", VENTAS, SUMA, NINGUNA, SEMANA, false)),
                arguments("qué producto vendo más este mes",
                        unaReceta("Qué producto vendo más este mes", VENTAS, UNIDADES, PRODUCTO, MES, false)),
                arguments("¿Cuánto me deben por cliente?",
                        unaReceta("Cuánto me deben por cliente", VENTAS, SUMA, CLIENTE, TODO, true)),
                arguments("En qué gasté más este mes",
                        unaReceta("En qué gasté más este mes", GASTOS, SUMA, CATEGORIA, MES, false)),
                arguments("cuántas ventas hice hoy",
                        unaReceta("Cuántas ventas hice hoy", VENTAS, CONTEO, NINGUNA, HOY, false)),
                arguments("Venta promedio de esta semana",
                        unaReceta("Venta promedio de esta semana", VENTAS, PROMEDIO, NINGUNA, SEMANA, false)),
                arguments("¿Cuánto stock me queda por producto?",
                        unaReceta("Cuánto stock me queda por producto", INVENTARIO, UNIDADES, PRODUCTO, TODO, false)),
                arguments("¿Qué día de la semana vendo más?",
                        unaReceta("Qué día de la semana vendo más", VENTAS, SUMA, DIA_DE_LA_SEMANA, MES, false)),
                arguments("cuánto gasté en total",
                        unaReceta("Cuánto gasté en total", GASTOS, SUMA, NINGUNA, TODO, false)),
                arguments("¿Cuánto vale mi mercadería?",
                        unaReceta("Cuánto vale mi mercadería", INVENTARIO, SUMA, NINGUNA, TODO, false)));
    }

    static Stream<Arguments> otrasFormasDePedir() {
        return Stream.of(
                arguments("cuantos productos tengo",
                        unaReceta("Cuantos productos tengo", INVENTARIO, CONTEO, NINGUNA, TODO, false)),
                arguments("¿Qué me queda?", unaReceta("Qué me queda", INVENTARIO, UNIDADES, NINGUNA, TODO, false)),
                arguments("cuánto facturé hoy", unaReceta("Cuánto facturé hoy", VENTAS, SUMA, NINGUNA, HOY, false)),
                arguments("cuánto cobré esta semana",
                        unaReceta("Cuánto cobré esta semana", VENTAS, SUMA, NINGUNA, SEMANA, false)),
                arguments("cuánto hice este mes",
                        unaReceta("Cuánto hice este mes", VENTAS, SUMA, NINGUNA, MES, false)),
                arguments("lo que me deben", unaReceta("Lo que me deben", VENTAS, SUMA, NINGUNA, TODO, true)),
                arguments("mis deudores", unaReceta("Mis deudores", VENTAS, SUMA, CLIENTE, TODO, true)),
                arguments("cuánto pagué este mes",
                        unaReceta("Cuánto pagué este mes", GASTOS, SUMA, NINGUNA, MES, false)),
                arguments("cantidad de compras de esta semana",
                        unaReceta("Cantidad de compras de esta semana", GASTOS, CONTEO, NINGUNA, SEMANA, false)),
                arguments("ventas por día", unaReceta("Ventas por día", VENTAS, SUMA, DIA_DE_LA_SEMANA, MES, false)),
                arguments("top productos vendidos este mes",
                        unaReceta("Top productos vendidos este mes", VENTAS, UNIDADES, PRODUCTO, MES, false)),
                arguments("ventas de los últimos 7 días",
                        unaReceta("Ventas de los últimos 7 días", VENTAS, SUMA, NINGUNA, SEMANA, false)));
    }

    static Stream<Arguments> comoTipeaLaGente() {
        return Stream.of(
                arguments("ganancias que me dejaron mis clientes este mes",
                        unaReceta("Ganancias que me dejaron mis clientes este mes", VENTAS, GANANCIA, CLIENTE, MES,
                                false)),
                arguments("cuánto me costó lo que vendí esta semana",
                        unaReceta("Cuánto me costó lo que vendí esta semana", VENTAS, COSTO, NINGUNA, SEMANA, false)),
                arguments("qué vendo más",
                        unaReceta("Qué vendo más", VENTAS, UNIDADES, PRODUCTO, MES, false)),
                arguments("cuánto vendí desde que abrí",
                        unaReceta("Cuánto vendí desde que abrí", VENTAS, SUMA, NINGUNA, TODO, false)),
                arguments("ventas de cada cliente",
                        unaReceta("Ventas de cada cliente", VENTAS, SUMA, CLIENTE, MES, false)),
                arguments("número de compras de hoy",
                        unaReceta("Número de compras de hoy", GASTOS, CONTEO, NINGUNA, HOY, false)),
                arguments("cuánto vendió el negocio en el mes",
                        unaReceta("Cuánto vendió el negocio en el mes", VENTAS, SUMA, NINGUNA, MES, false)));
    }

    static Stream<Arguments> loQueSeTomaPorDefecto() {
        return Stream.of(
                arguments("cuanto vendi el finde", List.of(CALCULO, AGRUPACION, PERIODO)),
                arguments("cuantos productos tengo", List.of(AGRUPACION)),
                arguments("cuanto me deben por cliente", List.of(CALCULO, PERIODO)),
                arguments("cuánto stock me queda por producto", List.of()),
                arguments("venta promedio de esta semana", List.of(AGRUPACION)));
    }

    static Stream<Arguments> loQueNoSeEntiende() {
        return Stream.of(
                arguments("cuanto vendi el finde", List.of("finde")),
                arguments("ventas del fin de semana", List.of("fin de semana")),
                arguments("¿Cuánto vendí ayer?", List.of("ayer")),
                arguments("cuánto vendí el mes pasado", List.of("mes pasado")),
                arguments("ventas por mes", List.of("por mes")),
                arguments("cuánto vendí en la feria", List.of("feria")),
                arguments("cuantos productos tengo", List.of()),
                arguments("cuanto me deben por cliente", List.of()));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Entiende los pedidos de todos los días de un almacén y arma la receta que piden")
    @MethodSource("pedidosDeUnAlmacen")
    void entiendeLosPedidosDeTodosLosDias(String pedido, DefinicionMetrica esperada) {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide(pedido);

        entoncesArmaLaReceta(esperada);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Entiende otras formas de pedir: facturé, cobré, me deben, pagué, compras, qué me queda y top")
    @MethodSource("otrasFormasDePedir")
    void entiendeOtrasFormasDePedir(String pedido, DefinicionMetrica esperada) {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide(pedido);

        entoncesArmaLaReceta(esperada);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Entiende cómo tipea la gente: dejaron, me costó, mis clientes, desde que abrí, cada cliente")
    @MethodSource("comoTipeaLaGente")
    void entiendeComoTipeaLaGente(String pedido, DefinicionMetrica esperada) {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide(pedido);

        entoncesArmaLaReceta(esperada);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Cuenta qué partes de la receta tomó por defecto porque el pedido no las decía")
    @MethodSource("loQueSeTomaPorDefecto")
    void cuentaLoQueTomoPorDefecto(String pedido, List<ParteDeLaReceta> porDefecto) {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide(pedido);

        entoncesTomoPorDefecto(porDefecto);
    }

    @Test
    @DisplayName("Cuenta qué partes de la receta salieron del pedido, también si es sólo lo fiado")
    void cuentaLoQueSalioDelPedido() {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide("cuanto me deben por cliente");

        entoncesSalieronDelPedido(List.of(FUENTE, AGRUPACION, SOLO_FIADO));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Avisa las palabras que no entendió, sin contar las vacías como «de» o «cuánto»")
    @MethodSource("loQueNoSeEntiende")
    void avisaLasPalabrasQueNoEntendio(String pedido, List<String> noEntendidas) {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide(pedido);

        entoncesNoEntendio(noEntendidas);
    }

    @Test
    @DisplayName("Un período que no sabe calcular, como el mes pasado, no se confunde con el mes: lo toma por defecto")
    void unPeriodoQueNoSabeCalcularNoSeConfundeConOtro() {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide("cuánto vendí el mes pasado");

        entoncesTomoPorDefecto(List.of(CALCULO, AGRUPACION, PERIODO));
    }

    @Test
    @DisplayName("Si no se da cuenta de qué tabla se habla, pide que se diga si son ventas, gastos o stock")
    void siNoSeEntiendeLaTablaPideQueSeDiga() {
        dadoElInterpreteDeLaAplicacion();

        cuandoSeIntentaPedir("hola, ¿cómo andás?");

        entoncesSeRechazaPorque(InterpreteDePedidos.NO_SE_ENTIENDE_LA_TABLA);
    }

    @Test
    @DisplayName("Si lo que se pide no se puede calcular, dice por qué")
    void siNoSePuedeCalcularDicePorQue() {
        dadoElInterpreteDeLaAplicacion();

        cuandoSeIntentaPedir("gastos por cliente");

        entoncesSeRechazaPorque("No se puede repartir gastos de esa manera");
    }

    @Test
    @DisplayName("Lo que el pedido no dice toma el valor de siempre: sumar, sin repartir y el mes")
    void loQueNoSeDiceTomaElValorDeSiempre() {
        dadoUnInterpreteQueSoloConoceLasVentas();

        cuandoSePide("ventas");

        entoncesArmaLaReceta(unaReceta("Ventas", VENTAS, SUMA, NINGUNA, MES, false));
    }

    private void dadoElInterpreteDeLaAplicacion() {
        interprete = elInterpreteDeLaAplicacion();
    }

    private void dadoUnInterpreteQueSoloConoceLasVentas() {
        interprete = new InterpreteDePedidos(unDiccionarioQueSoloConoceLasVentas());
    }

    private void cuandoSePide(String pedido) {
        interpretado = interprete.interpretar(pedido);
    }

    private void cuandoSeIntentaPedir(String pedido) {
        rechazo = catchThrowable(() -> cuandoSePide(pedido));
    }

    private void entoncesArmaLaReceta(DefinicionMetrica esperada) {
        assertThat(interpretado.receta()).isEqualTo(esperada);
    }

    private void entoncesTomoPorDefecto(List<ParteDeLaReceta> porDefecto) {
        assertThat(interpretado.porDefecto()).isEqualTo(porDefecto);
    }

    private void entoncesSalieronDelPedido(List<ParteDeLaReceta> delPedido) {
        assertThat(interpretado.delPedido()).isEqualTo(delPedido);
    }

    private void entoncesNoEntendio(List<String> noEntendidas) {
        assertThat(interpretado.noEntendidas()).isEqualTo(noEntendidas);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }
}

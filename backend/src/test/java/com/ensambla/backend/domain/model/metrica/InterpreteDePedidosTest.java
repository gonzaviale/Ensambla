package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CATEGORIA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CLIENTE;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.DIA_DE_LA_SEMANA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.PRODUCTO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.CONTEO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.PROMEDIO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.SUMA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.UNIDADES;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.GASTOS;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.INVENTARIO;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.VENTAS;
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

    private DefinicionMetrica receta;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("pedidosDeUnAlmacen")
    @DisplayName("Entiende los pedidos de todos los días de un almacén y arma la receta que piden")
    void entiendeLosPedidosDeTodosLosDias(String pedido, DefinicionMetrica esperada) {
        dadoElInterpreteDeLaAplicacion();

        cuandoSePide(pedido);

        entoncesArmaLaReceta(esperada);
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
        receta = interprete.interpretar(pedido);
    }

    private void cuandoSeIntentaPedir(String pedido) {
        rechazo = catchThrowable(() -> cuandoSePide(pedido));
    }

    private void entoncesArmaLaReceta(DefinicionMetrica esperada) {
        assertThat(receta).isEqualTo(esperada);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }
}

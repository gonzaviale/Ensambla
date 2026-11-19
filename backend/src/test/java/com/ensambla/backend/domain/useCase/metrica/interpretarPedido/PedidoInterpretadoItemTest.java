package com.ensambla.backend.domain.useCase.metrica.interpretarPedido;

import com.ensambla.backend.domain.model.metrica.DeDondeSale;
import com.ensambla.backend.domain.model.metrica.FormatoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.ParteDeLaReceta;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.interpretarPedido.PedidoInterpretadoItem.LecturaItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.PedidosDePrueba.unPedidoDelFindeInterpretadoComo;
import static org.assertj.core.api.Assertions.assertThat;

class PedidoInterpretadoItemTest {

    private static final ResultadoMetrica SIN_VENTAS = ResultadoMetrica.sinValor(FormatoMetrica.MONEDA,
            "No hay ventas en este período.", new DeDondeSale(FuenteMetrica.VENTAS, 0, 0, null, null));

    private PedidoInterpretadoItem interpretado;

    @Test
    @DisplayName("Copia la receta, el resultado y cómo se entendió el pedido")
    void copiaLaRecetaElResultadoYComoSeEntendio() {
        cuandoSeCopiaElPedidoDelFinde();

        entoncesTieneLaRecetaYLaLectura(new LecturaItem(List.of(ParteDeLaReceta.FUENTE),
                List.of(ParteDeLaReceta.CALCULO, ParteDeLaReceta.AGRUPACION, ParteDeLaReceta.PERIODO),
                List.of("finde")));
    }

    private void cuandoSeCopiaElPedidoDelFinde() {
        interpretado = PedidoInterpretadoItem.de(unPedidoDelFindeInterpretadoComo(lasVentasDelMes()), SIN_VENTAS, true);
    }

    private void entoncesTieneLaRecetaYLaLectura(LecturaItem lectura) {
        assertThat(interpretado.definicion()).isEqualTo(DefinicionItem.de(lasVentasDelMes()));
        assertThat(interpretado.resultado().motivo()).isEqualTo("No hay ventas en este período.");
        assertThat(interpretado.lectura()).isEqualTo(lectura);
        assertThat(interpretado.yaLaTenes()).isTrue();
    }
}

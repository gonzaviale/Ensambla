package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TextoMiniObjetivoTest {

    @Test
    @DisplayName("Subir un precio dice cuánto, lo que deja hoy, cuánto se vende y cuánto suma por mes")
    void subirPrecioShouldWriteTheNumbers() {
        TextoMiniObjetivo result = TextoMiniObjetivo.subirPrecio(
                "Yerba 1 kg", decimal("250"), decimal("900"), decimal("21.43"), decimal("14.0"), decimal("6786.00"));

        assertThat(result.titulo()).isEqualTo("Subile $250 a Yerba 1 kg");
        assertThat(result.justificacion()).isEqualTo(
                "Te deja $900 por unidad (21,4 %) y vendés cerca de 14 por semana. Con $250 más, aunque se vendan "
                        + "algunos menos, sumás cerca de $6.786 por mes.");
    }

    @Test
    @DisplayName("Mover un producto parado dice cuánto hay guardado, cuánto vale y a qué ritmo venderlo")
    void moverParadoShouldWriteTheNumbers() {
        TextoMiniObjetivo result = TextoMiniObjetivo.moverParado(
                "Fideos 500 g", decimal("40"), decimal("32000.00"), decimal("10"), decimal("3"));

        assertThat(result.titulo()).isEqualTo("Mové «Fideos 500 g»");
        assertThat(result.justificacion()).isEqualTo(
                "En el último mes no se vendió y tenés 40 guardados ($32.000 en mercadería). Bajale $10 y apuntá a 3 "
                        + "por semana.");
    }

    @Test
    @DisplayName("Poner a la vista un producto dice lo que deja por unidad y cuánto suma si se vende más")
    void ponerALaVistaShouldWriteTheNumbers() {
        TextoMiniObjetivo result = TextoMiniObjetivo.ponerALaVista(
                "Aceite 900 ml", decimal("850"), decimal("5"), decimal("6"), decimal("3643.00"));

        assertThat(result.titulo()).isEqualTo("Poné a la vista «Aceite 900 ml»");
        assertThat(result.justificacion()).isEqualTo(
                "Es de los que más te dejan por unidad: $850. Si pasás de 5 a 6 por semana, sumás cerca de $3.643 "
                        + "por mes.");
    }

    @Test
    @DisplayName("Recortar un gasto dice cuánto se gastó, la meta y cuánto se ahorra por mes")
    void recortarGastoShouldWriteTheNumbers() {
        TextoMiniObjetivo result = TextoMiniObjetivo.recortarGasto(
                "Servicios", decimal("40000.00"), decimal("36000.00"), decimal("4000.00"));

        assertThat(result.titulo()).isEqualTo("Gastar un 10 % menos en Servicios");
        assertThat(result.justificacion()).isEqualTo(
                "Gastaste $40.000 en el último mes en Servicios. Con $36.000 ahorrás $4.000 por mes.");
    }

    @Test
    @DisplayName("Mejorar el margen dice cuánto queda de cada $100 y cuántos puntos más se buscan")
    void mejorarMargenShouldWriteTheNumbers() {
        TextoMiniObjetivo result = TextoMiniObjetivo.mejorarMargen(
                decimal("35.71"), decimal("37.71"), decimal("14000.00"));

        assertThat(result.titulo()).isEqualTo("Ganar 2 puntos más en lo que vendés");
        assertThat(result.justificacion()).isEqualTo(
                "Hoy de cada $100 que vendés te quedan $35,7 después de pagar la mercadería. Con $37,7 sumás cerca "
                        + "de $14.000 por mes.");
    }

    @Test
    @DisplayName("Vender más por semana dice lo vendido, la meta y cuánto suma por mes")
    void ventasSemanalesShouldWriteTheNumbers() {
        TextoMiniObjetivo result = TextoMiniObjetivo.ventasSemanales(
                decimal("163333.00"), decimal("179666.30"), decimal("25000.00"));

        assertThat(result.titulo()).isEqualTo("Vender un 10 % más por semana");
        assertThat(result.justificacion()).isEqualTo(
                "En los últimos 7 días vendiste $163.333. Con $179.666 por semana sumás cerca de $25.000 por mes.");
    }

    // --- helpers ---
    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}

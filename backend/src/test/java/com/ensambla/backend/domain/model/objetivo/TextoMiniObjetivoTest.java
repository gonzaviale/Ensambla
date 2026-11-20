package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TextoMiniObjetivoTest {

    private TextoMiniObjetivo texto;

    @Test
    @DisplayName("Subir un precio dice cuánto, lo que deja hoy, cuánto se vende y cuánto suma por mes")
    void subirUnPrecioDiceCuantoLoQueDejaHoyCuantoSeVendeYCuantoSumaPorMes() {
        cuandoSeEscribeSubirPrecio();

        entoncesElTextoEs("Subile $250 a Yerba 1 kg",
                "Te deja $900 por unidad (21,4 %) y vendés cerca de 14 por semana. Con $250 más, aunque se vendan "
                        + "algunos menos, sumás cerca de $6.786 por mes.");
    }

    @Test
    @DisplayName("Mover un producto parado dice cuánto hay guardado, cuánto vale y a qué ritmo venderlo")
    void moverUnProductoParadoDiceCuantoHayGuardadoCuantoValeYAQueRitmoVenderlo() {
        cuandoSeEscribeMoverParado();

        entoncesElTextoEs("Volvé a vender «Fideos 500 g»",
                "En el último mes no se vendió y tenés 40 guardados ($32.000 en mercadería). Bajale $10 y apuntá a 3 "
                        + "por semana.");
    }

    @Test
    @DisplayName("Poner a la vista un producto dice lo que deja por unidad y cuánto suma si se vende más")
    void ponerALaVistaUnProductoDiceLoQueDejaPorUnidadYCuantoSumaSiSeVendeMas() {
        cuandoSeEscribePonerALaVista();

        entoncesElTextoEs("Poné a la vista «Aceite 900 ml»",
                "Es de los que más te dejan por unidad: $850. Si pasás de 5 a 6 por semana, sumás cerca de $3.643 "
                        + "por mes.");
    }

    @Test
    @DisplayName("Recortar un gasto dice cuánto se gastó, la meta y cuánto se ahorra por mes")
    void recortarUnGastoDiceCuantoSeGastoLaMetaYCuantoSeAhorraPorMes() {
        cuandoSeEscribeRecortarGasto();

        entoncesElTextoEs("Gastar un 10 % menos en Servicios",
                "Gastaste $40.000 en el último mes en Servicios. Con $36.000 ahorrás $4.000 por mes.");
    }

    @Test
    @DisplayName("Mejorar el margen dice cuánto queda de cada $100 y cuántos puntos más se buscan")
    void mejorarElMargenDiceCuantoQuedaDeCada100YCuantosPuntosMasSeBuscan() {
        cuandoSeEscribeMejorarMargen();

        entoncesElTextoEs("Ganar 2 puntos más en lo que vendés",
                "Hoy de cada $100 que vendés te quedan $35,7 después de pagar la mercadería. Con $37,7 sumás cerca "
                        + "de $14.000 por mes.");
    }

    @Test
    @DisplayName("Vender más por semana dice lo vendido, la meta y cuánto suma por mes")
    void venderMasPorSemanaDiceLoVendidoLaMetaYCuantoSumaPorMes() {
        cuandoSeEscribeVentasSemanales();

        entoncesElTextoEs("Vender un 10 % más por semana",
                "En los últimos 7 días vendiste $163.333. Con $179.666 por semana sumás cerca de $25.000 por mes.");
    }

    @Test
    @DisplayName("Reponer stock dice cuántos productos están bajo el mínimo y que el objetivo es llegar a cero")
    void reponerStockDiceCuantosProductosEstanBajoElMinimoYQueElObjetivoEsLlegarA() {
        cuandoSeEscribeReponerStock();

        entoncesElTextoEs("Reponé lo que está por debajo de tu mínimo",
                "Productos en su mínimo o por debajo: hoy son 4. Reponelos y bajá ese número a 0 para no quedarte "
                        + "sin stock.");
    }

    @Test
    @DisplayName("Capital parado dice cuánto hay parado, a cuánto bajarlo y cuánto se libera")
    void capitalParadoDiceCuantoHayParadoACuantoBajarloYCuantoSeLibera() {
        cuandoSeEscribeCapitalParado();

        entoncesElTextoEs("Liberá plata parada en mercadería",
                "Tenés $32.000 en mercadería que no se vendió en el último mes. Si la bajás a $24.000 liberás $8.000 "
                        + "para comprar lo que sí rota.");
    }

    @Test
    @DisplayName("Variedad dice cuántos productos distintos se venden por semana y a cuántos llegar")
    void variedadDiceCuantosProductosDistintosSeVendenPorSemanaYACuantosLlegar() {
        cuandoSeEscribeVariedadVendida();

        entoncesElTextoEs("Vendé más variedad por semana",
                "Productos distintos que vendiste en los últimos 7 días: 2. Si llegás a 3 le mostrás más opciones a "
                        + "tus clientes.");
    }

    // --- act ---
    private void cuandoSeEscribeSubirPrecio() {
        texto = TextoMiniObjetivo.subirPrecio(
                "Yerba 1 kg", decimal("250"), decimal("900"), decimal("21.43"), decimal("14.0"), decimal("6786.00"));
    }

    private void cuandoSeEscribeMoverParado() {
        texto = TextoMiniObjetivo.moverParado(
                "Fideos 500 g", decimal("40"), decimal("32000.00"), decimal("10"), decimal("3"));
    }

    private void cuandoSeEscribePonerALaVista() {
        texto = TextoMiniObjetivo.ponerALaVista(
                "Aceite 900 ml", decimal("850"), decimal("5"), decimal("6"), decimal("3643.00"));
    }

    private void cuandoSeEscribeRecortarGasto() {
        texto = TextoMiniObjetivo.recortarGasto(
                "Servicios", decimal("40000.00"), decimal("36000.00"), decimal("4000.00"));
    }

    private void cuandoSeEscribeMejorarMargen() {
        texto = TextoMiniObjetivo.mejorarMargen(decimal("35.71"), decimal("37.71"), decimal("14000.00"));
    }

    private void cuandoSeEscribeVentasSemanales() {
        texto = TextoMiniObjetivo.ventasSemanales(decimal("163333.00"), decimal("179666.30"), decimal("25000.00"));
    }

    private void cuandoSeEscribeReponerStock() {
        texto = TextoMiniObjetivo.reponerStock(decimal("4"));
    }

    private void cuandoSeEscribeCapitalParado() {
        texto = TextoMiniObjetivo.capitalParado(decimal("32000.00"), decimal("24000.00"));
    }

    private void cuandoSeEscribeVariedadVendida() {
        texto = TextoMiniObjetivo.variedadVendida(decimal("2"), decimal("3"));
    }

    // --- assert ---
    private void entoncesElTextoEs(String titulo, String justificacion) {
        assertThat(texto.titulo()).isEqualTo(titulo);
        assertThat(texto.justificacion()).isEqualTo(justificacion);
    }

    // --- helpers ---
    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}

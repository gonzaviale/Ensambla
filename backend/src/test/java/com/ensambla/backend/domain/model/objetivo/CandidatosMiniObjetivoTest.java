package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.NegocioDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.NegocioDePrueba.ACEITE;
import static com.ensambla.backend.support.NegocioDePrueba.FIDEOS;
import static com.ensambla.backend.support.NegocioDePrueba.YERBA;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CandidatosMiniObjetivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
    private static final UUID PRODUCTO = UUID.fromString("d0000000-0000-4000-8000-0000000000a1");
    private static final UUID CARAMELO = UUID.fromString("d0000000-0000-4000-8000-0000000000a2");

    private DatosDeProyecto negocio;
    private AnalisisZona zona = AnalisisZona.sinZona();
    private List<CandidatoMiniObjetivo> candidatos;
    private CandidatoMiniObjetivo candidato;
    private List<CandidatoMiniObjetivo> elegidos;

    @Test
    @DisplayName("Arma todos los candidatos con datos, del que más suma al que menos")
    void armarShouldListEveryCandidateFromTheBestToTheWorst() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenSeListanDelQueMasSumaAlQueMenos();
    }

    @Test
    @DisplayName("Cada candidato trae el aporte estimado que le corresponde")
    void armarShouldEstimateTheContributionOfEachCandidate() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenCadaCandidatoTraeSuAporteEstimado();
    }

    @Test
    @DisplayName("Subir el precio de la yerba: 5 % del precio hacia arriba al paso de $50, con la caída de ventas")
    void armarShouldBuildSubirPrecioFromTheProductWithLowMargin() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenSubirElPrecioDeLaYerbaEstaArmado();
    }

    @Test
    @DisplayName("Mover los fideos parados: un cuarto del stock en 4 semanas, con una rebaja de $10")
    void armarShouldBuildMoverParadoFromTheProductThatDoesNotSell() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenMoverLosFideosParadosEstaArmado();
    }

    @Test
    @DisplayName("Poner a la vista el producto que se vende: una unidad más por semana o un 20 % más")
    void armarShouldBuildPonerALaVistaFromTheProductThatSells() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenPonerALaVistaLaYerbaEstaArmado();
    }

    @Test
    @DisplayName("Recortar el gasto: un 10 % menos en cada categoría, sin la mercadería")
    void armarShouldBuildRecortarGastoFromTheCategories() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenRecortarElGastoEstaArmadoPorCategoria();
    }

    @Test
    @DisplayName("Mejorar el margen: dos puntos más sobre lo vendido en 30 días")
    void armarShouldBuildMejorarMargenFromTheBusinessMargin() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenMejorarElMargenEstaArmado();
    }

    @Test
    @DisplayName("Vender más por semana: un 10 % más, con el margen que tiene el negocio")
    void armarShouldBuildVentasSemanalesFromTheWeeklySales() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenVenderMasPorSemanaEstaArmado();
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no propone nada")
    void armarShouldReturnNothingWhenThereAreNoSales() {
        givenUnNegocio(sinVentas());

        whenSeArmanLosCandidatos();

        thenNoSeProponeNada();
    }

    @Test
    @DisplayName("No sube el precio de un producto con buen margen ni de uno que no se vende")
    void armarShouldNotRaiseThePriceOfAProductWithHealthyMarginOrWithoutSales() {
        givenUnNegocio(conBuenMargenYUnProductoSinVentas());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("No sube el precio si el aumento no suma: lo que se pierde en ventas se come lo que se gana")
    void armarShouldNotRaiseThePriceWhenTheLostSalesEatTheGain() {
        givenUnNegocio(conPrecioQueNoAguantaUnAumento());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("Con rango de zona no sube el precio por encima del máximo que se cobra en la zona")
    void armarShouldCapThePriceRaiseAtTheZoneMaximum() {
        givenUnNegocio(almacen());
        givenUnaZonaDondeLaYerbaSeCobraHasta("4300");

        whenSeArmanLosCandidatos();

        thenElAumentoDeLaYerbaQuedaTopeadoEnLaZona();
    }

    @Test
    @DisplayName("Con rango de zona no sube el precio de un producto que ya cobra el máximo")
    void armarShouldNotRaiseThePriceOfAProductThatAlreadyChargesTheZoneMaximum() {
        givenUnNegocio(almacen());
        givenUnaZonaDondeLaYerbaSeCobraHasta("4200");

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("No propone mover un producto sin stock ni uno que no admite una rebaja")
    void armarShouldNotProposeToMoveAProductWithoutStockOrWithoutRoomForADiscount() {
        givenUnNegocio(conProductosParadosQueNoSePuedenMover());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.MOVER_PARADO);
    }

    @Test
    @DisplayName("No propone poner a la vista un producto que se vende pero no deja plata por unidad")
    void armarShouldNotProposeToShowAProductThatLosesMoney() {
        givenUnNegocio(conUnProductoQueNoDejaGanancia());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.PONER_A_LA_VISTA);
    }

    @Test
    @DisplayName("No propone mejorar el margen si ya es muy alto")
    void armarShouldNotProposeToImproveAMarginThatIsAlreadyHigh() {
        givenUnNegocio(conMargenMuyAlto());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Sin costos cargados no hay margen que mejorar y no propone nada")
    void armarShouldNotProposeAnythingWithoutCosts() {
        givenUnNegocio(sinCostosCargados());

        whenSeArmanLosCandidatos();

        thenNoSeProponeNada();
    }

    @Test
    @DisplayName("No propone vender más por semana si no se vendió esta semana")
    void armarShouldNotProposeMoreWeeklySalesWithoutASaleThisWeek() {
        givenUnNegocio(sinVentasEstaSemana());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.VENTAS_SEMANALES);
    }

    @Test
    @DisplayName("No propone vender más por semana si el negocio pierde en cada venta")
    void armarShouldNotProposeMoreWeeklySalesWhenEverySaleLosesMoney() {
        givenUnNegocio(conPerdidaEnCadaVenta());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.VENTAS_SEMANALES);
    }

    @Test
    @DisplayName("Elige los 3 de mayor aporte sin repetir el tipo ni el producto")
    void elegirShouldTakeTheBestThreeWithoutRepeatingTypeOrProduct() {
        givenUnNegocio(almacen());

        whenSeArmanYSeEligen(List.of(), 3);

        thenSeEligenLosTiposEnOrden(TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES,
                TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Elige sólo la cantidad pedida")
    void elegirShouldTakeOnlyTheRequestedAmount() {
        givenUnNegocio(almacen());

        whenSeArmanYSeEligen(List.of(), 2);

        thenSeEligenEstaCantidad(2);
    }

    @Test
    @DisplayName("Elige menos de los que pide cuando no hay más que no choquen")
    void elegirShouldTakeFewerWhenThereAreNoMoreThatDoNotClash() {
        givenUnNegocio(almacen());

        whenSeArmanYSeEligen(List.of(), CandidatosMiniObjetivo.TODOS);

        thenSeEligenLosTiposEnOrden(TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES,
                TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.RECORTAR_GASTO, TipoMiniObjetivo.MOVER_PARADO,
                TipoMiniObjetivo.CAPITAL_PARADO, TipoMiniObjetivo.VARIEDAD_VENDIDA);
    }

    @Test
    @DisplayName("No elige el tipo ni el producto de los mini-objetivos que ya ocupan un lugar")
    void elegirShouldSkipTheTypesAndProductsOfTheOccupiedPlaces() {
        givenUnNegocio(almacen());

        whenSeArmanYSeEligen(List.of(miniDeVentas(), miniDeLaYerba()), 3);

        thenSeEligenLosQueNoChocanConLosOcupados();
    }

    @Test
    @DisplayName("Las alternativas no son el mismo mini-objetivo ni repiten el tipo de los otros en curso")
    void alternativasShouldExcludeTheCurrentOneAndTheTypesOfTheOthers() {
        givenUnNegocio(almacen());
        MiniObjetivo actual = miniDeLaYerba();

        whenSePidenLasAlternativas(actual, List.of(actual, otroMiniDeVentas()), CandidatosMiniObjetivo.TODOS);

        thenLasAlternativasExcluyenAlActualYLosTiposDeLosOtros();
    }

    @Test
    @DisplayName("Las alternativas pueden ser del mismo tipo que el actual con otro producto")
    void alternativasShouldAllowTheSameTypeWithAnotherProduct() {
        givenUnNegocio(almacen());
        MiniObjetivo actual = miniPonerALaVistaLaYerba();

        whenSePidenLasAlternativas(actual, List.of(actual), CandidatosMiniObjetivo.TODOS);

        thenHayUnaAlternativaDelMismoTipoConOtroProducto();
    }

    @Test
    @DisplayName("Ofrece como mucho tres alternativas")
    void alternativasShouldOfferAtMostThree() {
        givenUnNegocio(almacen());
        MiniObjetivo actual = miniPonerALaVistaLaYerba();

        whenSePidenLasAlternativas(actual, List.of(actual), CandidatosMiniObjetivo.ALTERNATIVAS);

        thenSeOfrecenComoMuchoTresAlternativas();
    }

    @Test
    @DisplayName("Capital parado: la mercadería que no rota (Fideos: 40 × $800), pidiendo que baje un cuarto, sin aporte")
    void armarShouldProposeToFreeTheStuckCapital() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenSeProponeLiberarElCapitalParado();
    }

    @Test
    @DisplayName("Variedad: hoy se vendieron 2 de los 3 productos en la semana, la meta es 3")
    void armarShouldProposeToSellOneMoreProduct() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenSeProponeVenderUnProductoMas();
    }

    @Test
    @DisplayName("Productos bajo el mínimo: cuántos hay hoy y la meta es llegar a cero")
    void armarShouldProposeToReplenishTheProductsUnderTheMinimum() {
        givenUnNegocio(conMinimos(3, 5));

        whenSeArmanLosCandidatos();

        thenSeProponeReponerLoQueEstaBajoElMinimo();
    }

    @Test
    @DisplayName("Sin mínimos definidos no propone reponer")
    void armarShouldNotProposeToReplenishWithoutMinimums() {
        givenUnNegocio(almacen());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
    }

    @Test
    @DisplayName("Con todos los productos por encima del mínimo no propone reponer")
    void armarShouldNotProposeToReplenishWhenEveryProductIsAboveTheMinimum() {
        givenUnNegocio(conMinimos(30, 5));

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
    }

    @Test
    @DisplayName("Si todo lo que hay en stock se vendió no hay capital parado que proponer")
    void armarShouldNotProposeStuckCapitalWhenEverythingSells() {
        givenUnNegocio(conUnSoloProductoQueRota());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.CAPITAL_PARADO);
    }

    @Test
    @DisplayName("Si ya se vende toda la variedad que hay no se pide un producto más")
    void armarShouldNotProposeMoreVarietyThanTheProductsThereAre() {
        givenUnNegocio(conUnSoloProductoQueRota());

        whenSeArmanLosCandidatos();

        thenNoSePropone(TipoMiniObjetivo.VARIEDAD_VENDIDA);
    }

    @Test
    @DisplayName("Las métricas nuevas no pisan a un mini-objetivo de producto: no tienen producto")
    void elegirShouldAllowTheNewMetricsNextToProductMinis() {
        givenUnNegocio(almacen());

        whenSeArmanYSeEligen(List.of(miniDeLaYerba()), CandidatosMiniObjetivo.TODOS);

        thenSeEligenTambienLasMetricasSinProducto();
    }

    // --- arrange ---
    private void givenUnNegocio(DatosDeProyecto datos) {
        negocio = datos;
    }

    private void givenUnaZonaDondeLaYerbaSeCobraHasta(String maximo) {
        zona = new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto("Yerba 1 kg", 5, 5,
                new BigDecimal("3900"), new BigDecimal("4100"), new BigDecimal(maximo), BigDecimal.TEN)));
    }

    // --- act ---
    private void whenSeArmanLosCandidatos() {
        candidatos = armar();
    }

    private void whenSeArmanYSeEligen(List<MiniObjetivo> ocupados, int cantidad) {
        elegidos = CandidatosMiniObjetivo.elegir(armar(), ocupados, cantidad);
    }

    private void whenSePidenLasAlternativas(MiniObjetivo actual, List<MiniObjetivo> enCurso, int cantidad) {
        elegidos = CandidatosMiniObjetivo.alternativas(armar(), actual, enCurso, cantidad);
    }

    // --- assert ---
    private void thenSeListanDelQueMasSumaAlQueMenos() {
        assertThat(candidatos).extracting(CandidatoMiniObjetivo::tipo, CandidatoMiniObjetivo::referencia)
                .containsExactly(
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.VENTAS_SEMANALES, null),
                        tuple(TipoMiniObjetivo.SUBIR_PRECIO, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.MEJORAR_MARGEN, null),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Alquiler"),
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Aceite 900 ml"),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Servicios"),
                        tuple(TipoMiniObjetivo.MOVER_PARADO, "Fideos 500 g"),
                        tuple(TipoMiniObjetivo.CAPITAL_PARADO, null),
                        tuple(TipoMiniObjetivo.VARIEDAD_VENDIDA, null));
    }

    private void thenCadaCandidatoTraeSuAporteEstimado() {
        assertThat(candidatos).extracting(CandidatoMiniObjetivo::aporteEstimado).satisfiesExactly(
                aporte -> assertThat(aporte).isEqualByComparingTo("11571.43"),
                aporte -> assertThat(aporte).isEqualByComparingTo("7394.75"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6789.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6180.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6000.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("4071.43"),
                aporte -> assertThat(aporte).isEqualByComparingTo("4000.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("3728.57"),
                aporte -> assertThat(aporte).isEqualByComparingTo("0"),
                aporte -> assertThat(aporte).isEqualByComparingTo("0"));
    }

    private void thenSubirElPrecioDeLaYerbaEstaArmado() {
        CandidatoMiniObjetivo subirPrecio = candidatoDe(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(subirPrecio.registroId()).isEqualTo(RegistroId.de(YERBA));
        assertThat(subirPrecio.referencia()).isEqualTo("Yerba 1 kg");
        assertThat(subirPrecio.valorInicial()).isEqualByComparingTo("12600.00");
        assertThat(subirPrecio.meta()).isEqualByComparingTo("14184.10");
        assertThat(subirPrecio.aporteEstimado()).isEqualByComparingTo("6789.00");
        assertThat(subirPrecio.titulo()).isEqualTo("Subile $250 a Yerba 1 kg");
        assertThat(subirPrecio.textoConIa()).isFalse();
    }

    private void thenMoverLosFideosParadosEstaArmado() {
        CandidatoMiniObjetivo moverParado = candidatoDe(TipoMiniObjetivo.MOVER_PARADO);
        assertThat(moverParado.registroId()).isEqualTo(RegistroId.de(FIDEOS));
        assertThat(moverParado.valorInicial()).isEqualByComparingTo("0");
        assertThat(moverParado.meta()).isEqualByComparingTo("3");
        assertThat(moverParado.aporteEstimado()).isEqualByComparingTo("3728.57");
        assertThat(moverParado.titulo()).isEqualTo("Mové «Fideos 500 g»");
    }

    private void thenPonerALaVistaLaYerbaEstaArmado() {
        CandidatoMiniObjetivo ponerALaVista = candidatoDe(TipoMiniObjetivo.PONER_A_LA_VISTA);
        assertThat(ponerALaVista.referencia()).isEqualTo("Yerba 1 kg");
        assertThat(ponerALaVista.valorInicial()).isEqualByComparingTo("14");
        assertThat(ponerALaVista.meta()).isEqualByComparingTo("17");
        assertThat(ponerALaVista.aporteEstimado()).isEqualByComparingTo("11571.43");
    }

    private void thenRecortarElGastoEstaArmadoPorCategoria() {
        List<CandidatoMiniObjetivo> recortes = candidatos.stream()
                .filter(candidatoDeLaLista -> candidatoDeLaLista.tipo() == TipoMiniObjetivo.RECORTAR_GASTO)
                .toList();
        assertThat(recortes).hasSize(2);
        assertThat(recortes.getFirst().registroId()).isNull();
        assertThat(recortes.getFirst().referencia()).isEqualTo("Alquiler");
        assertThat(recortes.getFirst().valorInicial()).isEqualByComparingTo("60000");
        assertThat(recortes.getFirst().meta()).isEqualByComparingTo("54000");
        assertThat(recortes.getFirst().aporteEstimado()).isEqualByComparingTo("6000");
    }

    private void thenMejorarElMargenEstaArmado() {
        CandidatoMiniObjetivo margen = candidatoDe(TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(margen.registroId()).isNull();
        assertThat(margen.valorInicial()).isEqualByComparingTo("23.62");
        assertThat(margen.meta()).isEqualByComparingTo("25.62");
        assertThat(margen.aporteEstimado()).isEqualByComparingTo("6180.00");
    }

    private void thenVenderMasPorSemanaEstaArmado() {
        CandidatoMiniObjetivo ventas = candidatoDe(TipoMiniObjetivo.VENTAS_SEMANALES);
        assertThat(ventas.valorInicial()).isEqualByComparingTo("73050");
        assertThat(ventas.meta()).isEqualByComparingTo("80355");
        assertThat(ventas.aporteEstimado()).isEqualByComparingTo("7394.75");
    }

    private void thenNoSeProponeNada() {
        assertThat(candidatos).isEmpty();
    }

    private void thenNoSePropone(TipoMiniObjetivo tipo) {
        assertThat(candidatos).extracting(CandidatoMiniObjetivo::tipo).doesNotContain(tipo);
    }

    private void thenElAumentoDeLaYerbaQuedaTopeadoEnLaZona() {
        CandidatoMiniObjetivo subirPrecio = candidatoDe(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(subirPrecio.titulo()).isEqualTo("Subile $100 a Yerba 1 kg");
        assertThat(subirPrecio.aporteEstimado()).isEqualByComparingTo("3144.00");
    }

    private void thenSeEligenLosTiposEnOrden(TipoMiniObjetivo... tipos) {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo).containsExactly(tipos);
    }

    private void thenSeEligenEstaCantidad(int cantidad) {
        assertThat(elegidos).hasSize(cantidad);
    }

    private void thenSeEligenLosQueNoChocanConLosOcupados() {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo).containsExactly(
                TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.RECORTAR_GASTO,
                TipoMiniObjetivo.PONER_A_LA_VISTA);
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::referencia)
                .containsExactly(null, "Alquiler", "Aceite 900 ml");
    }

    private void thenLasAlternativasExcluyenAlActualYLosTiposDeLosOtros() {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo, CandidatoMiniObjetivo::referencia)
                .containsExactly(
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.MEJORAR_MARGEN, null),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Alquiler"),
                        tuple(TipoMiniObjetivo.MOVER_PARADO, "Fideos 500 g"),
                        tuple(TipoMiniObjetivo.CAPITAL_PARADO, null),
                        tuple(TipoMiniObjetivo.VARIEDAD_VENDIDA, null));
    }

    private void thenHayUnaAlternativaDelMismoTipoConOtroProducto() {
        assertThat(elegidos).anySatisfy(alternativa -> {
            assertThat(alternativa.tipo()).isEqualTo(TipoMiniObjetivo.PONER_A_LA_VISTA);
            assertThat(alternativa.referencia()).isEqualTo("Aceite 900 ml");
        });
    }

    private void thenSeOfrecenComoMuchoTresAlternativas() {
        assertThat(elegidos).hasSize(CandidatosMiniObjetivo.ALTERNATIVAS);
    }

    private void thenSeProponeLiberarElCapitalParado() {
        CandidatoMiniObjetivo capitalParado = candidatoDe(TipoMiniObjetivo.CAPITAL_PARADO);
        assertThat(capitalParado.valorInicial()).isEqualByComparingTo("32000");
        assertThat(capitalParado.meta()).isEqualByComparingTo("24000");
        assertThat(capitalParado.aporteEstimado()).isEqualByComparingTo("0");
        assertThat(capitalParado.registroId()).isNull();
        assertThat(capitalParado.referencia()).isNull();
        assertThat(capitalParado.titulo()).isEqualTo("Liberá plata parada en mercadería");
        assertThat(capitalParado.justificacion()).contains("$32.000", "$24.000", "$8.000");
    }

    private void thenSeProponeVenderUnProductoMas() {
        CandidatoMiniObjetivo variedad = candidatoDe(TipoMiniObjetivo.VARIEDAD_VENDIDA);
        assertThat(variedad.valorInicial()).isEqualByComparingTo("2");
        assertThat(variedad.meta()).isEqualByComparingTo("3");
        assertThat(variedad.aporteEstimado()).isEqualByComparingTo("0");
        assertThat(variedad.titulo()).isEqualTo("Vendé más variedad por semana");
    }

    private void thenSeProponeReponerLoQueEstaBajoElMinimo() {
        CandidatoMiniObjetivo reponer = candidatoDe(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
        assertThat(reponer.valorInicial()).isEqualByComparingTo("1");
        assertThat(reponer.meta()).isEqualByComparingTo("0");
        assertThat(reponer.aporteEstimado()).isEqualByComparingTo("0");
        assertThat(reponer.titulo()).isEqualTo("Reponé lo que está por debajo de tu mínimo");
    }

    private void thenSeEligenTambienLasMetricasSinProducto() {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo)
                .contains(TipoMiniObjetivo.CAPITAL_PARADO, TipoMiniObjetivo.VARIEDAD_VENDIDA);
    }

    // --- helpers ---
    private List<CandidatoMiniObjetivo> armar() {
        MedidorMiniObjetivo medidor = MedidorMiniObjetivo.de(
                List.of(negocio), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
        return CandidatosMiniObjetivo.armar(medidor, zona);
    }

    private CandidatoMiniObjetivo candidatoDe(TipoMiniObjetivo tipo) {
        return candidatos.stream()
                .filter(candidatoDeLaLista -> candidatoDeLaLista.tipo() == tipo)
                .findFirst()
                .orElseThrow();
    }

    private static MiniObjetivo miniDe(TipoMiniObjetivo tipo, RegistroId registroId, String referencia) {
        MiniObjetivo mini = miniObjetivo();
        mini.setTipo(tipo);
        mini.setRegistroId(registroId);
        mini.setReferencia(referencia);
        return mini;
    }

    private static MiniObjetivo miniDeVentas() {
        return miniDe(TipoMiniObjetivo.VENTAS_SEMANALES, null, null);
    }

    private static MiniObjetivo otroMiniDeVentas() {
        MiniObjetivo otro = miniDeVentas();
        otro.setId(OTRO_MINI_ID);
        return otro;
    }

    private static MiniObjetivo miniDeLaYerba() {
        return miniDe(TipoMiniObjetivo.SUBIR_PRECIO, RegistroId.de(YERBA), "Yerba 1 kg");
    }

    private static MiniObjetivo miniPonerALaVistaLaYerba() {
        return miniDe(TipoMiniObjetivo.PONER_A_LA_VISTA, RegistroId.de(YERBA), "Yerba 1 kg");
    }

    private static DatosDeProyecto sinVentas() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(FIDEOS, "Fideos 500 g", 40, 1100, 800)
                .gasto("2026-11-01", 5000, "Servicios")
                .build();
    }

    private static DatosDeProyecto conBuenMargenYUnProductoSinVentas() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(ACEITE, "Aceite 900 ml", 20, 2850, 1900)
                .producto(FIDEOS, "Fideos 500 g", 40, 1100, 800)
                .venta(ACEITE, 5, 2850, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto conPrecioQueNoAguantaUnAumento() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 40, 30)
                .venta(PRODUCTO, 10, 40, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto conProductosParadosQueNoSePuedenMover() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(FIDEOS, "Fideos 500 g", 0, 1100, 800)
                .producto(CARAMELO, "Caramelo", 50, 100, 90)
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 10, 4200, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto conUnProductoQueNoDejaGanancia() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 900, 1000)
                .venta(PRODUCTO, 10, 900, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto conMargenMuyAlto() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, 300)
                .venta(PRODUCTO, 10, 1000, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto sinCostosCargados() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, null)
                .venta(PRODUCTO, 10, 1000, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto sinVentasEstaSemana() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, 700)
                .venta(PRODUCTO, 10, 1000, "2026-10-20")
                .build();
    }

    private static DatosDeProyecto conPerdidaEnCadaVenta() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, 1200)
                .venta(PRODUCTO, 10, 1000, "2026-11-09")
                .build();
    }

    private static DatosDeProyecto conUnSoloProductoQueRota() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 3, 4200, "2026-11-08")
                .build();
    }

    private static DatosDeProyecto conMinimos(int stockYerba, int minimoYerba) {
        return NegocioDePrueba.cuaderno("Almacén")
                .productoConMinimo(YERBA, "Yerba 1 kg", stockYerba, minimoYerba, 4200, 3300)
                .productoConMinimo(FIDEOS, "Fideos 500 g", 40, 10, 1100, 800)
                .venta(YERBA, 3, 4200, "2026-11-08")
                .venta(FIDEOS, 2, 1100, "2026-11-09")
                .build();
    }
}

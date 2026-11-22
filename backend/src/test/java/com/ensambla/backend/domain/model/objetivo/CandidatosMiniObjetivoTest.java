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
    void armaTodosLosCandidatosConDatosDelQueMasSumaAlQueMenos() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesSeListanDelQueMasSumaAlQueMenos();
    }

    @Test
    @DisplayName("Cada candidato trae el aporte estimado que le corresponde")
    void cadaCandidatoTraeElAporteEstimadoQueLeCorresponde() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesCadaCandidatoTraeSuAporteEstimado();
    }

    @Test
    @DisplayName("Subir el precio de la yerba: 5 % del precio hacia arriba al paso de $50, con la caída de ventas")
    void subirElPrecioDeLaYerba5DelPrecioHaciaArribaAlPasoDe50ConLaCaidaDeVentas() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesSubirElPrecioDeLaYerbaEstaArmado();
    }

    @Test
    @DisplayName("Mover los fideos parados: un cuarto del stock en 4 semanas, con una rebaja de $10")
    void moverLosFideosParadosUnCuartoDelStockEn4SemanasConUnaRebajaDe10() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesMoverLosFideosParadosEstaArmado();
    }

    @Test
    @DisplayName("Poner a la vista el producto que se vende: una unidad más por semana o un 20 % más")
    void ponerALaVistaElProductoQueSeVendeUnaUnidadMasPorSemanaOUn20Mas() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesPonerALaVistaLaYerbaEstaArmado();
    }

    @Test
    @DisplayName("Recortar el gasto: un 10 % menos en cada categoría, sin la mercadería ni el alquiler")
    void recortarElGastoUn10MenosEnCadaCategoriaSinLaMercaderiaNiElAlquiler() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesRecortarElGastoEstaArmadoPorCategoria();
    }

    @Test
    @DisplayName("Mejorar el margen: dos puntos más sobre lo vendido en 30 días")
    void mejorarElMargenDosPuntosMasSobreLoVendidoEn30Dias() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesMejorarElMargenEstaArmado();
    }

    @Test
    @DisplayName("Vender más por semana: un 10 % más, con el margen que tiene el negocio")
    void venderMasPorSemanaUn10MasConElMargenQueTieneElNegocio() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesVenderMasPorSemanaEstaArmado();
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no propone nada")
    void sinVentasEnLosUltimos30DiasNoProponeNada() {
        dadoUnNegocio(sinVentas());

        cuandoSeArmanLosCandidatos();

        entoncesNoSeProponeNada();
    }

    @Test
    @DisplayName("No sube el precio de un producto con buen margen ni de uno que no se vende")
    void noSubeElPrecioDeUnProductoConBuenMargenNiDeUnoQueNoSeVende() {
        dadoUnNegocio(conBuenMargenYUnProductoSinVentas());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("No sube el precio si el aumento no suma: lo que se pierde en ventas se come lo que se gana")
    void noSubeElPrecioSiElAumentoNoSumaLoQueSePierdeEnVentasSeComeLoQueSeGana() {
        dadoUnNegocio(conPrecioQueNoAguantaUnAumento());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("Con rango de zona no sube el precio por encima del máximo que se cobra en la zona")
    void conRangoDeZonaNoSubeElPrecioPorEncimaDelMaximoQueSeCobraEnLaZona() {
        dadoUnNegocio(almacen());
        dadoUnaZonaDondeLaYerbaSeCobraHasta("4300");

        cuandoSeArmanLosCandidatos();

        entoncesElAumentoDeLaYerbaQuedaTopeadoEnLaZona();
    }

    @Test
    @DisplayName("Con rango de zona no sube el precio de un producto que ya cobra el máximo")
    void conRangoDeZonaNoSubeElPrecioDeUnProductoQueYaCobraElMaximo() {
        dadoUnNegocio(almacen());
        dadoUnaZonaDondeLaYerbaSeCobraHasta("4200");

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("No propone mover un producto sin stock ni uno que no admite una rebaja")
    void noProponeMoverUnProductoSinStockNiUnoQueNoAdmiteUnaRebaja() {
        dadoUnNegocio(conProductosParadosQueNoSePuedenMover());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.MOVER_PARADO);
    }

    @Test
    @DisplayName("No propone poner a la vista un producto que se vende pero no deja plata por unidad")
    void noProponePonerALaVistaUnProductoQueSeVendePeroNoDejaPlataPorUnidad() {
        dadoUnNegocio(conUnProductoQueNoDejaGanancia());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.PONER_A_LA_VISTA);
    }

    @Test
    @DisplayName("No propone mejorar el margen si ya es muy alto")
    void noProponeMejorarElMargenSiYaEsMuyAlto() {
        dadoUnNegocio(conMargenMuyAlto());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Sin costos cargados no hay margen que mejorar y no propone nada")
    void sinCostosCargadosNoHayMargenQueMejorarYNoProponeNada() {
        dadoUnNegocio(sinCostosCargados());

        cuandoSeArmanLosCandidatos();

        entoncesNoSeProponeNada();
    }

    @Test
    @DisplayName("No propone vender más por semana si no se vendió esta semana")
    void noProponeVenderMasPorSemanaSiNoSeVendioEstaSemana() {
        dadoUnNegocio(sinVentasEstaSemana());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.VENTAS_SEMANALES);
    }

    @Test
    @DisplayName("No propone vender más por semana si el negocio pierde en cada venta")
    void noProponeVenderMasPorSemanaSiElNegocioPierdeEnCadaVenta() {
        dadoUnNegocio(conPerdidaEnCadaVenta());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.VENTAS_SEMANALES);
    }

    @Test
    @DisplayName("Elige los 3 de mayor aporte sin repetir el tipo ni el producto")
    void eligeLos3DeMayorAporteSinRepetirElTipoNiElProducto() {
        dadoUnNegocio(almacen());

        cuandoSeArmanYSeEligen(List.of(), 3);

        entoncesSeEligenLosTiposEnOrden(TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES,
                TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Elige sólo la cantidad pedida")
    void eligeSoloLaCantidadPedida() {
        dadoUnNegocio(almacen());

        cuandoSeArmanYSeEligen(List.of(), 2);

        entoncesSeEligenEstaCantidad(2);
    }

    @Test
    @DisplayName("Elige menos de los que pide cuando no hay más que no choquen")
    void eligeMenosDeLosQuePideCuandoNoHayMasQueNoChoquen() {
        dadoUnNegocio(almacen());

        cuandoSeArmanYSeEligen(List.of(), CandidatosMiniObjetivo.TODOS);

        entoncesSeEligenLosTiposEnOrden(TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES,
                TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.RECORTAR_GASTO, TipoMiniObjetivo.MOVER_PARADO,
                TipoMiniObjetivo.CAPITAL_PARADO, TipoMiniObjetivo.VARIEDAD_VENDIDA);
    }

    @Test
    @DisplayName("No elige el tipo ni el producto de los mini-objetivos que ya ocupan un lugar")
    void noEligeElTipoNiElProductoDeLosMiniObjetivosQueYaOcupanUnLugar() {
        dadoUnNegocio(almacen());

        cuandoSeArmanYSeEligen(List.of(miniDeVentas(), miniDeLaYerba()), 3);

        entoncesSeEligenLosQueNoChocanConLosOcupados();
    }

    @Test
    @DisplayName("Las alternativas no son el mismo mini-objetivo ni repiten el tipo de los otros en curso")
    void lasAlternativasNoSonElMismoMiniObjetivoNiRepitenElTipoDeLosOtrosEnCurso() {
        dadoUnNegocio(almacen());
        MiniObjetivo actual = miniDeLaYerba();

        cuandoSePidenLasAlternativas(actual, List.of(actual, otroMiniDeVentas()), CandidatosMiniObjetivo.TODOS);

        entoncesLasAlternativasExcluyenAlActualYLosTiposDeLosOtros();
    }

    @Test
    @DisplayName("Las alternativas pueden ser del mismo tipo que el actual con otro producto")
    void lasAlternativasPuedenSerDelMismoTipoQueElActualConOtroProducto() {
        dadoUnNegocio(almacen());
        MiniObjetivo actual = miniPonerALaVistaLaYerba();

        cuandoSePidenLasAlternativas(actual, List.of(actual), CandidatosMiniObjetivo.TODOS);

        entoncesHayUnaAlternativaDelMismoTipoConOtroProducto();
    }

    @Test
    @DisplayName("Ofrece como mucho tres alternativas")
    void ofreceComoMuchoTresAlternativas() {
        dadoUnNegocio(almacen());
        MiniObjetivo actual = miniPonerALaVistaLaYerba();

        cuandoSePidenLasAlternativas(actual, List.of(actual), CandidatosMiniObjetivo.ALTERNATIVAS);

        entoncesSeOfrecenComoMuchoTresAlternativas();
    }

    @Test
    @DisplayName("Capital parado: la mercadería que no rota (Fideos: 40 × $800), pidiendo que baje un cuarto, sin aporte")
    void capitalParadoLaMercaderiaQueNoRotaFideos40800PidiendoQueBajeUnCuartoSin() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesSeProponeLiberarElCapitalParado();
    }

    @Test
    @DisplayName("Variedad: hoy se vendieron 2 de los 3 productos en la semana, la meta es 3")
    void variedadHoySeVendieron2DeLos3ProductosEnLaSemanaLaMetaEs3() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesSeProponeVenderUnProductoMas();
    }

    @Test
    @DisplayName("Productos bajo el mínimo: cuántos hay hoy y la meta es llegar a cero")
    void productosBajoElMinimoCuantosHayHoyYLaMetaEsLlegarACero() {
        dadoUnNegocio(conMinimos(3, 5));

        cuandoSeArmanLosCandidatos();

        entoncesSeProponeReponerLoQueEstaBajoElMinimo();
    }

    @Test
    @DisplayName("Sin mínimos definidos no propone reponer")
    void sinMinimosDefinidosNoProponeReponer() {
        dadoUnNegocio(almacen());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
    }

    @Test
    @DisplayName("Con todos los productos por encima del mínimo no propone reponer")
    void conTodosLosProductosPorEncimaDelMinimoNoProponeReponer() {
        dadoUnNegocio(conMinimos(30, 5));

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
    }

    @Test
    @DisplayName("Si todo lo que hay en stock se vendió no hay capital parado que proponer")
    void siTodoLoQueHayEnStockSeVendioNoHayCapitalParadoQueProponer() {
        dadoUnNegocio(conUnSoloProductoQueRota());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.CAPITAL_PARADO);
    }

    @Test
    @DisplayName("Si ya se vende toda la variedad que hay no se pide un producto más")
    void siYaSeVendeTodaLaVariedadQueHayNoSePideUnProductoMas() {
        dadoUnNegocio(conUnSoloProductoQueRota());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.VARIEDAD_VENDIDA);
    }

    @Test
    @DisplayName("Las métricas nuevas no pisan a un mini-objetivo de producto: no tienen producto")
    void lasMetricasNuevasNoPisanAUnMiniObjetivoDeProductoNoTienenProducto() {
        dadoUnNegocio(almacen());

        cuandoSeArmanYSeEligen(List.of(miniDeLaYerba()), CandidatosMiniObjetivo.TODOS);

        entoncesSeEligenTambienLasMetricasSinProducto();
    }

    @Test
    @DisplayName("No propone recortar gastos fijos como el alquiler, los sueldos o los impuestos")
    void noProponeRecortarGastosFijosComoElAlquilerLosSueldosOLosImpuestos() {
        dadoUnNegocio(conSoloGastosFijos());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.RECORTAR_GASTO);
    }

    @Test
    @DisplayName("No propone poner a la vista un producto que ya llegó a su mínimo: primero hay que reponerlo")
    void noProponePonerALaVistaUnProductoQueYaLlegoASuMinimoPrimeroHayQueReponerlo() {
        dadoUnNegocio(conMinimos(3, 5));

        cuandoSeArmanLosCandidatos();

        entoncesNoSeProponePonerALaVista("Yerba 1 kg");
    }

    @Test
    @DisplayName("Propone poner a la vista un producto que está por encima de su mínimo")
    void proponePonerALaVistaUnProductoQueEstaPorEncimaDeSuMinimo() {
        dadoUnNegocio(conMinimos(30, 5));

        cuandoSeArmanLosCandidatos();

        entoncesSeProponePonerALaVista("Yerba 1 kg");
    }

    @Test
    @DisplayName("No propone volver a vender un producto parado que ya llegó a su mínimo")
    void noProponeVolverAVenderUnProductoParadoQueYaLlegoASuMinimo() {
        dadoUnNegocio(conUnParadoEnSuMinimo());

        cuandoSeArmanLosCandidatos();

        entoncesNoSePropone(TipoMiniObjetivo.MOVER_PARADO);
    }

    // --- arrange ---
    private void dadoUnNegocio(DatosDeProyecto datos) {
        negocio = datos;
    }

    private void dadoUnaZonaDondeLaYerbaSeCobraHasta(String maximo) {
        zona = new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto("Yerba 1 kg", 5, 5,
                new BigDecimal("3900"), new BigDecimal("4100"), new BigDecimal(maximo), BigDecimal.TEN)));
    }

    // --- act ---
    private void cuandoSeArmanLosCandidatos() {
        candidatos = armar();
    }

    private void cuandoSeArmanYSeEligen(List<MiniObjetivo> ocupados, int cantidad) {
        elegidos = CandidatosMiniObjetivo.elegir(armar(), ocupados, cantidad);
    }

    private void cuandoSePidenLasAlternativas(MiniObjetivo actual, List<MiniObjetivo> enCurso, int cantidad) {
        elegidos = CandidatosMiniObjetivo.alternativas(armar(), actual, enCurso, cantidad);
    }

    // --- assert ---
    private void entoncesSeListanDelQueMasSumaAlQueMenos() {
        assertThat(candidatos).extracting(CandidatoMiniObjetivo::tipo, CandidatoMiniObjetivo::referencia)
                .containsExactly(
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.VENTAS_SEMANALES, null),
                        tuple(TipoMiniObjetivo.SUBIR_PRECIO, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.MEJORAR_MARGEN, null),
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Aceite 900 ml"),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Servicios"),
                        tuple(TipoMiniObjetivo.MOVER_PARADO, "Fideos 500 g"),
                        tuple(TipoMiniObjetivo.CAPITAL_PARADO, null),
                        tuple(TipoMiniObjetivo.VARIEDAD_VENDIDA, null));
    }

    private void entoncesCadaCandidatoTraeSuAporteEstimado() {
        assertThat(candidatos).extracting(CandidatoMiniObjetivo::aporteEstimado).satisfiesExactly(
                aporte -> assertThat(aporte).isEqualByComparingTo("11571.43"),
                aporte -> assertThat(aporte).isEqualByComparingTo("7394.75"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6789.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6180.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("4071.43"),
                aporte -> assertThat(aporte).isEqualByComparingTo("4000.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("3728.57"),
                aporte -> assertThat(aporte).isEqualByComparingTo("0"),
                aporte -> assertThat(aporte).isEqualByComparingTo("0"));
    }

    private void entoncesSubirElPrecioDeLaYerbaEstaArmado() {
        CandidatoMiniObjetivo subirPrecio = candidatoDe(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(subirPrecio.registroId()).isEqualTo(RegistroId.de(YERBA));
        assertThat(subirPrecio.referencia()).isEqualTo("Yerba 1 kg");
        assertThat(subirPrecio.valorInicial()).isEqualByComparingTo("12600.00");
        assertThat(subirPrecio.meta()).isEqualByComparingTo("14184.10");
        assertThat(subirPrecio.aporteEstimado()).isEqualByComparingTo("6789.00");
        assertThat(subirPrecio.titulo()).isEqualTo("Subile $250 a Yerba 1 kg");
        assertThat(subirPrecio.textoConIa()).isFalse();
    }

    private void entoncesMoverLosFideosParadosEstaArmado() {
        CandidatoMiniObjetivo moverParado = candidatoDe(TipoMiniObjetivo.MOVER_PARADO);
        assertThat(moverParado.registroId()).isEqualTo(RegistroId.de(FIDEOS));
        assertThat(moverParado.valorInicial()).isEqualByComparingTo("0");
        assertThat(moverParado.meta()).isEqualByComparingTo("3");
        assertThat(moverParado.aporteEstimado()).isEqualByComparingTo("3728.57");
        assertThat(moverParado.titulo()).isEqualTo("Volvé a vender «Fideos 500 g»");
    }

    private void entoncesPonerALaVistaLaYerbaEstaArmado() {
        CandidatoMiniObjetivo ponerALaVista = candidatoDe(TipoMiniObjetivo.PONER_A_LA_VISTA);
        assertThat(ponerALaVista.referencia()).isEqualTo("Yerba 1 kg");
        assertThat(ponerALaVista.valorInicial()).isEqualByComparingTo("14");
        assertThat(ponerALaVista.meta()).isEqualByComparingTo("17");
        assertThat(ponerALaVista.aporteEstimado()).isEqualByComparingTo("11571.43");
    }

    private void entoncesRecortarElGastoEstaArmadoPorCategoria() {
        List<CandidatoMiniObjetivo> recortes = candidatos.stream()
                .filter(candidatoDeLaLista -> candidatoDeLaLista.tipo() == TipoMiniObjetivo.RECORTAR_GASTO)
                .toList();
        assertThat(recortes).hasSize(1);
        assertThat(recortes.getFirst().registroId()).isNull();
        assertThat(recortes.getFirst().referencia()).isEqualTo("Servicios");
        assertThat(recortes.getFirst().valorInicial()).isEqualByComparingTo("40000");
        assertThat(recortes.getFirst().meta()).isEqualByComparingTo("36000");
        assertThat(recortes.getFirst().aporteEstimado()).isEqualByComparingTo("4000");
    }

    private void entoncesMejorarElMargenEstaArmado() {
        CandidatoMiniObjetivo margen = candidatoDe(TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(margen.registroId()).isNull();
        assertThat(margen.valorInicial()).isEqualByComparingTo("23.62");
        assertThat(margen.meta()).isEqualByComparingTo("25.62");
        assertThat(margen.aporteEstimado()).isEqualByComparingTo("6180.00");
    }

    private void entoncesVenderMasPorSemanaEstaArmado() {
        CandidatoMiniObjetivo ventas = candidatoDe(TipoMiniObjetivo.VENTAS_SEMANALES);
        assertThat(ventas.valorInicial()).isEqualByComparingTo("73050");
        assertThat(ventas.meta()).isEqualByComparingTo("80355");
        assertThat(ventas.aporteEstimado()).isEqualByComparingTo("7394.75");
    }

    private void entoncesNoSeProponeNada() {
        assertThat(candidatos).isEmpty();
    }

    private void entoncesNoSePropone(TipoMiniObjetivo tipo) {
        assertThat(candidatos).extracting(CandidatoMiniObjetivo::tipo).doesNotContain(tipo);
    }

    private void entoncesElAumentoDeLaYerbaQuedaTopeadoEnLaZona() {
        CandidatoMiniObjetivo subirPrecio = candidatoDe(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(subirPrecio.titulo()).isEqualTo("Subile $100 a Yerba 1 kg");
        assertThat(subirPrecio.aporteEstimado()).isEqualByComparingTo("3144.00");
    }

    private void entoncesSeEligenLosTiposEnOrden(TipoMiniObjetivo... tipos) {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo).containsExactly(tipos);
    }

    private void entoncesSeEligenEstaCantidad(int cantidad) {
        assertThat(elegidos).hasSize(cantidad);
    }

    private void entoncesSeEligenLosQueNoChocanConLosOcupados() {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo).containsExactly(
                TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.PONER_A_LA_VISTA,
                TipoMiniObjetivo.RECORTAR_GASTO);
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::referencia)
                .containsExactly(null, "Aceite 900 ml", "Servicios");
    }

    private void entoncesLasAlternativasExcluyenAlActualYLosTiposDeLosOtros() {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo, CandidatoMiniObjetivo::referencia)
                .containsExactly(
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.MEJORAR_MARGEN, null),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Servicios"),
                        tuple(TipoMiniObjetivo.MOVER_PARADO, "Fideos 500 g"),
                        tuple(TipoMiniObjetivo.CAPITAL_PARADO, null),
                        tuple(TipoMiniObjetivo.VARIEDAD_VENDIDA, null));
    }

    private void entoncesHayUnaAlternativaDelMismoTipoConOtroProducto() {
        assertThat(elegidos).anySatisfy(alternativa -> {
            assertThat(alternativa.tipo()).isEqualTo(TipoMiniObjetivo.PONER_A_LA_VISTA);
            assertThat(alternativa.referencia()).isEqualTo("Aceite 900 ml");
        });
    }

    private void entoncesSeOfrecenComoMuchoTresAlternativas() {
        assertThat(elegidos).hasSize(CandidatosMiniObjetivo.ALTERNATIVAS);
    }

    private void entoncesSeProponeLiberarElCapitalParado() {
        CandidatoMiniObjetivo capitalParado = candidatoDe(TipoMiniObjetivo.CAPITAL_PARADO);
        assertThat(capitalParado.valorInicial()).isEqualByComparingTo("32000");
        assertThat(capitalParado.meta()).isEqualByComparingTo("24000");
        assertThat(capitalParado.aporteEstimado()).isEqualByComparingTo("0");
        assertThat(capitalParado.registroId()).isNull();
        assertThat(capitalParado.referencia()).isNull();
        assertThat(capitalParado.titulo()).isEqualTo("Liberá plata parada en mercadería");
        assertThat(capitalParado.justificacion()).contains("$32.000", "$24.000", "$8.000");
    }

    private void entoncesSeProponeVenderUnProductoMas() {
        CandidatoMiniObjetivo variedad = candidatoDe(TipoMiniObjetivo.VARIEDAD_VENDIDA);
        assertThat(variedad.valorInicial()).isEqualByComparingTo("2");
        assertThat(variedad.meta()).isEqualByComparingTo("3");
        assertThat(variedad.aporteEstimado()).isEqualByComparingTo("0");
        assertThat(variedad.titulo()).isEqualTo("Vendé más variedad por semana");
    }

    private void entoncesSeProponeReponerLoQueEstaBajoElMinimo() {
        CandidatoMiniObjetivo reponer = candidatoDe(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
        assertThat(reponer.valorInicial()).isEqualByComparingTo("1");
        assertThat(reponer.meta()).isEqualByComparingTo("0");
        assertThat(reponer.aporteEstimado()).isEqualByComparingTo("0");
        assertThat(reponer.titulo()).isEqualTo("Reponé lo que está por debajo de tu mínimo");
    }

    private void entoncesNoSeProponePonerALaVista(String producto) {
        assertThat(ponerALaVista()).doesNotContain(producto);
    }

    private void entoncesSeProponePonerALaVista(String producto) {
        assertThat(ponerALaVista()).contains(producto);
    }

    private void entoncesSeEligenTambienLasMetricasSinProducto() {
        assertThat(elegidos).extracting(CandidatoMiniObjetivo::tipo)
                .contains(TipoMiniObjetivo.CAPITAL_PARADO, TipoMiniObjetivo.VARIEDAD_VENDIDA);
    }

    // --- helpers ---
    private List<CandidatoMiniObjetivo> armar() {
        MedidorMiniObjetivo medidor = MedidorMiniObjetivo.de(
                List.of(negocio), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
        return CandidatosMiniObjetivo.armar(medidor, zona);
    }

    private List<String> ponerALaVista() {
        return candidatos.stream()
                .filter(candidatoDeLaLista -> candidatoDeLaLista.tipo() == TipoMiniObjetivo.PONER_A_LA_VISTA)
                .map(CandidatoMiniObjetivo::referencia)
                .toList();
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

    private static DatosDeProyecto conSoloGastosFijos() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 3, 4200, "2026-11-08")
                .gasto("2026-11-01", 500000, "Alquiler")
                .gasto("2026-11-02", 300000, "Sueldo empleada")
                .gasto("2026-11-03", 80000, "Impuestos municipales")
                .build();
    }

    private static DatosDeProyecto conUnParadoEnSuMinimo() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .productoConMinimo(FIDEOS, "Fideos 500 g", 2, 10, 1100, 800)
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

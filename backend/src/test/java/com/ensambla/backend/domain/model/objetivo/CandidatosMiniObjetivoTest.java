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

    // --- armar: el almacén de ejemplo ---

    @Test
    @DisplayName("Arma todos los candidatos con datos, del que más suma al que menos")
    void armarShouldListEveryCandidateFromTheBestToTheWorst() {
        List<CandidatoMiniObjetivo> result = armar(almacen());

        assertThat(result).extracting(CandidatoMiniObjetivo::tipo, CandidatoMiniObjetivo::referencia)
                .containsExactly(
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.VENTAS_SEMANALES, null),
                        tuple(TipoMiniObjetivo.SUBIR_PRECIO, "Yerba 1 kg"),
                        tuple(TipoMiniObjetivo.MEJORAR_MARGEN, null),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Alquiler"),
                        tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Aceite 900 ml"),
                        tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Servicios"),
                        tuple(TipoMiniObjetivo.MOVER_PARADO, "Fideos 500 g"));
        assertThat(result).extracting(CandidatoMiniObjetivo::aporteEstimado).satisfiesExactly(
                aporte -> assertThat(aporte).isEqualByComparingTo("11571.43"),
                aporte -> assertThat(aporte).isEqualByComparingTo("7394.75"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6789.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6180.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("6000.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("4071.43"),
                aporte -> assertThat(aporte).isEqualByComparingTo("4000.00"),
                aporte -> assertThat(aporte).isEqualByComparingTo("3728.57"));
    }

    @Test
    @DisplayName("Subir el precio de la yerba: 5 % del precio hacia arriba al paso de $50, con la caída de ventas")
    void armarShouldBuildSubirPrecioFromTheProductWithLowMargin() {
        CandidatoMiniObjetivo result = candidatoDe(armar(almacen()), TipoMiniObjetivo.SUBIR_PRECIO);

        assertThat(result.registroId()).isEqualTo(RegistroId.de(YERBA));
        assertThat(result.referencia()).isEqualTo("Yerba 1 kg");
        assertThat(result.valorInicial()).isEqualByComparingTo("12600.00");
        assertThat(result.meta()).isEqualByComparingTo("14184.10");
        assertThat(result.aporteEstimado()).isEqualByComparingTo("6789.00");
        assertThat(result.titulo()).isEqualTo("Subile $250 a Yerba 1 kg");
        assertThat(result.textoConIa()).isFalse();
    }

    @Test
    @DisplayName("Mover los fideos parados: un cuarto del stock en 4 semanas, con una rebaja de $10")
    void armarShouldBuildMoverParadoFromTheProductThatDoesNotSell() {
        CandidatoMiniObjetivo result = candidatoDe(armar(almacen()), TipoMiniObjetivo.MOVER_PARADO);

        assertThat(result.registroId()).isEqualTo(RegistroId.de(FIDEOS));
        assertThat(result.valorInicial()).isEqualByComparingTo("0");
        assertThat(result.meta()).isEqualByComparingTo("3");
        assertThat(result.aporteEstimado()).isEqualByComparingTo("3728.57");
        assertThat(result.titulo()).isEqualTo("Mové «Fideos 500 g»");
    }

    @Test
    @DisplayName("Poner a la vista el producto que se vende: una unidad más por semana o un 20 % más")
    void armarShouldBuildPonerALaVistaFromTheProductThatSells() {
        CandidatoMiniObjetivo result = armar(almacen()).stream()
                .filter(candidato -> candidato.tipo() == TipoMiniObjetivo.PONER_A_LA_VISTA)
                .findFirst()
                .orElseThrow();

        assertThat(result.referencia()).isEqualTo("Yerba 1 kg");
        assertThat(result.valorInicial()).isEqualByComparingTo("14");
        assertThat(result.meta()).isEqualByComparingTo("17");
        assertThat(result.aporteEstimado()).isEqualByComparingTo("11571.43");
    }

    @Test
    @DisplayName("Recortar el gasto: un 10 % menos en cada categoría, sin la mercadería")
    void armarShouldBuildRecortarGastoFromTheCategories() {
        List<CandidatoMiniObjetivo> result = armar(almacen()).stream()
                .filter(candidato -> candidato.tipo() == TipoMiniObjetivo.RECORTAR_GASTO)
                .toList();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().registroId()).isNull();
        assertThat(result.getFirst().referencia()).isEqualTo("Alquiler");
        assertThat(result.getFirst().valorInicial()).isEqualByComparingTo("60000");
        assertThat(result.getFirst().meta()).isEqualByComparingTo("54000");
        assertThat(result.getFirst().aporteEstimado()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("Mejorar el margen: dos puntos más sobre lo vendido en 30 días")
    void armarShouldBuildMejorarMargenFromTheBusinessMargin() {
        CandidatoMiniObjetivo result = candidatoDe(armar(almacen()), TipoMiniObjetivo.MEJORAR_MARGEN);

        assertThat(result.registroId()).isNull();
        assertThat(result.valorInicial()).isEqualByComparingTo("23.62");
        assertThat(result.meta()).isEqualByComparingTo("25.62");
        assertThat(result.aporteEstimado()).isEqualByComparingTo("6180.00");
    }

    @Test
    @DisplayName("Vender más por semana: un 10 % más, con el margen que tiene el negocio")
    void armarShouldBuildVentasSemanalesFromTheWeeklySales() {
        CandidatoMiniObjetivo result = candidatoDe(armar(almacen()), TipoMiniObjetivo.VENTAS_SEMANALES);

        assertThat(result.valorInicial()).isEqualByComparingTo("73050");
        assertThat(result.meta()).isEqualByComparingTo("80355");
        assertThat(result.aporteEstimado()).isEqualByComparingTo("7394.75");
    }

    // --- armar: lo que no se propone ---

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no propone nada")
    void armarShouldReturnNothingWhenThereAreNoSales() {
        DatosDeProyecto sinVentas = NegocioDePrueba.cuaderno("Almacén")
                .producto(FIDEOS, "Fideos 500 g", 40, 1100, 800)
                .gasto("2026-11-01", 5000, "Servicios")
                .build();

        assertThat(armar(sinVentas)).isEmpty();
    }

    @Test
    @DisplayName("No sube el precio de un producto con buen margen ni de uno que no se vende")
    void armarShouldNotRaiseThePriceOfAProductWithHealthyMarginOrWithoutSales() {
        DatosDeProyecto sano = NegocioDePrueba.cuaderno("Almacén")
                .producto(ACEITE, "Aceite 900 ml", 20, 2850, 1900)
                .producto(FIDEOS, "Fideos 500 g", 40, 1100, 800)
                .venta(ACEITE, 5, 2850, "2026-11-09")
                .build();

        assertThat(armar(sano)).noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("No sube el precio si el aumento no suma: lo que se pierde en ventas se come lo que se gana")
    void armarShouldNotRaiseThePriceWhenTheLostSalesEatTheGain() {
        DatosDeProyecto caro = NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 40, 30)
                .venta(PRODUCTO, 10, 40, "2026-11-09")
                .build();

        assertThat(armar(caro)).noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("Con rango de zona no sube el precio por encima del máximo que se cobra en la zona")
    void armarShouldCapThePriceRaiseAtTheZoneMaximum() {
        AnalisisZona zona = zonaConYerbaHasta("4300");

        CandidatoMiniObjetivo result = candidatoDe(armar(almacen(), zona), TipoMiniObjetivo.SUBIR_PRECIO);

        assertThat(result.titulo()).isEqualTo("Subile $100 a Yerba 1 kg");
        assertThat(result.aporteEstimado()).isEqualByComparingTo("3144.00");
    }

    @Test
    @DisplayName("Con rango de zona no sube el precio de un producto que ya cobra el máximo")
    void armarShouldNotRaiseThePriceOfAProductThatAlreadyChargesTheZoneMaximum() {
        AnalisisZona zona = zonaConYerbaHasta("4200");

        assertThat(armar(almacen(), zona)).noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.SUBIR_PRECIO);
    }

    @Test
    @DisplayName("No propone mover un producto sin stock ni uno que no admite una rebaja")
    void armarShouldNotProposeToMoveAProductWithoutStockOrWithoutRoomForADiscount() {
        DatosDeProyecto parados = NegocioDePrueba.cuaderno("Almacén")
                .producto(FIDEOS, "Fideos 500 g", 0, 1100, 800)
                .producto(PRODUCTO, "Caramelo", 50, 100, 90)
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 10, 4200, "2026-11-09")
                .build();

        assertThat(armar(parados)).noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.MOVER_PARADO);
    }

    @Test
    @DisplayName("No propone poner a la vista un producto que se vende pero no deja plata por unidad")
    void armarShouldNotProposeToShowAProductThatLosesMoney() {
        DatosDeProyecto sinGanancia = NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 900, 1000)
                .venta(PRODUCTO, 10, 900, "2026-11-09")
                .build();

        assertThat(armar(sinGanancia)).noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.PONER_A_LA_VISTA);
    }

    @Test
    @DisplayName("No propone mejorar el margen si ya es muy alto ni si no hay costos cargados")
    void armarShouldNotProposeToImproveAMarginThatIsAlreadyHighOrUnknown() {
        DatosDeProyecto margenAlto = NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, 300)
                .venta(PRODUCTO, 10, 1000, "2026-11-09")
                .build();
        DatosDeProyecto sinCostos = NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, null)
                .venta(PRODUCTO, 10, 1000, "2026-11-09")
                .build();

        assertThat(armar(margenAlto)).noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(armar(sinCostos)).isEmpty();
    }

    @Test
    @DisplayName("No propone vender más por semana si no se vendió esta semana ni si el negocio pierde en cada venta")
    void armarShouldNotProposeMoreWeeklySalesWithoutASaleThisWeekOrWithoutMargin() {
        DatosDeProyecto sinVentasEstaSemana = NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, 700)
                .venta(PRODUCTO, 10, 1000, "2026-10-20")
                .build();
        DatosDeProyecto conPerdida = NegocioDePrueba.cuaderno("Almacén")
                .producto(PRODUCTO, "Alfajor", 10, 1000, 1200)
                .venta(PRODUCTO, 10, 1000, "2026-11-09")
                .build();

        assertThat(armar(sinVentasEstaSemana))
                .noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.VENTAS_SEMANALES);
        assertThat(armar(conPerdida))
                .noneMatch(candidato -> candidato.tipo() == TipoMiniObjetivo.VENTAS_SEMANALES);
    }

    // --- elegir ---

    @Test
    @DisplayName("Elige los 3 de mayor aporte sin repetir el tipo ni el producto")
    void elegirShouldTakeTheBestThreeWithoutRepeatingTypeOrProduct() {
        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.elegir(armar(almacen()), List.of(), 3);

        assertThat(result).extracting(CandidatoMiniObjetivo::tipo).containsExactly(
                TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES,
                TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Elige sólo la cantidad pedida")
    void elegirShouldTakeOnlyTheRequestedAmount() {
        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.elegir(armar(almacen()), List.of(), 2);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Elige menos de los que pide cuando no hay más que no choquen")
    void elegirShouldTakeFewerWhenThereAreNoMoreThatDoNotClash() {
        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.elegir(armar(almacen()), List.of(),
                CandidatosMiniObjetivo.TODOS);

        assertThat(result).extracting(CandidatoMiniObjetivo::tipo).containsExactly(
                TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES,
                TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.RECORTAR_GASTO, TipoMiniObjetivo.MOVER_PARADO);
    }

    @Test
    @DisplayName("No elige el tipo ni el producto de los mini-objetivos que ya ocupan un lugar")
    void elegirShouldSkipTheTypesAndProductsOfTheOccupiedPlaces() {
        MiniObjetivo ventas = miniDe(TipoMiniObjetivo.VENTAS_SEMANALES, null, null);
        MiniObjetivo yerba = miniDe(TipoMiniObjetivo.SUBIR_PRECIO, RegistroId.de(YERBA), "Yerba 1 kg");

        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.elegir(
                armar(almacen()), List.of(ventas, yerba), 3);

        assertThat(result).extracting(CandidatoMiniObjetivo::tipo).containsExactly(
                TipoMiniObjetivo.MEJORAR_MARGEN, TipoMiniObjetivo.RECORTAR_GASTO,
                TipoMiniObjetivo.PONER_A_LA_VISTA);
        assertThat(result).extracting(CandidatoMiniObjetivo::referencia)
                .containsExactly(null, "Alquiler", "Aceite 900 ml");
    }

    // --- alternativas ---

    @Test
    @DisplayName("Las alternativas no son el mismo mini-objetivo ni repiten el tipo de los otros en curso")
    void alternativasShouldExcludeTheCurrentOneAndTheTypesOfTheOthers() {
        MiniObjetivo actual = miniDe(TipoMiniObjetivo.SUBIR_PRECIO, RegistroId.de(YERBA), "Yerba 1 kg");
        MiniObjetivo otro = miniDe(TipoMiniObjetivo.VENTAS_SEMANALES, null, null);
        otro.setId(OTRO_MINI_ID);

        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.alternativas(
                armar(almacen()), actual, List.of(actual, otro), CandidatosMiniObjetivo.TODOS);

        assertThat(result).extracting(CandidatoMiniObjetivo::tipo, CandidatoMiniObjetivo::referencia).containsExactly(
                tuple(TipoMiniObjetivo.PONER_A_LA_VISTA, "Yerba 1 kg"),
                tuple(TipoMiniObjetivo.MEJORAR_MARGEN, null),
                tuple(TipoMiniObjetivo.RECORTAR_GASTO, "Alquiler"),
                tuple(TipoMiniObjetivo.MOVER_PARADO, "Fideos 500 g"));
    }

    @Test
    @DisplayName("Las alternativas pueden ser del mismo tipo que el actual con otro producto")
    void alternativasShouldAllowTheSameTypeWithAnotherProduct() {
        MiniObjetivo actual = miniDe(TipoMiniObjetivo.PONER_A_LA_VISTA, RegistroId.de(YERBA), "Yerba 1 kg");

        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.alternativas(
                armar(almacen()), actual, List.of(actual), CandidatosMiniObjetivo.TODOS);

        assertThat(result).anySatisfy(candidato -> {
            assertThat(candidato.tipo()).isEqualTo(TipoMiniObjetivo.PONER_A_LA_VISTA);
            assertThat(candidato.referencia()).isEqualTo("Aceite 900 ml");
        });
    }

    @Test
    @DisplayName("Ofrece como mucho tres alternativas")
    void alternativasShouldOfferAtMostThree() {
        MiniObjetivo actual = miniDe(TipoMiniObjetivo.PONER_A_LA_VISTA, RegistroId.de(YERBA), "Yerba 1 kg");

        List<CandidatoMiniObjetivo> result = CandidatosMiniObjetivo.alternativas(
                armar(almacen()), actual, List.of(actual), CandidatosMiniObjetivo.ALTERNATIVAS);

        assertThat(result).hasSize(CandidatosMiniObjetivo.ALTERNATIVAS);
    }

    // --- helpers ---
    private static List<CandidatoMiniObjetivo> armar(DatosDeProyecto datos) {
        return armar(datos, AnalisisZona.sinZona());
    }

    private static List<CandidatoMiniObjetivo> armar(DatosDeProyecto datos, AnalisisZona zona) {
        MedidorMiniObjetivo medidor = MedidorMiniObjetivo.de(
                List.of(datos), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
        return CandidatosMiniObjetivo.armar(medidor, zona);
    }

    private static CandidatoMiniObjetivo candidatoDe(List<CandidatoMiniObjetivo> candidatos, TipoMiniObjetivo tipo) {
        return candidatos.stream().filter(candidato -> candidato.tipo() == tipo).findFirst().orElseThrow();
    }

    private static AnalisisZona zonaConYerbaHasta(String maximo) {
        return new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto("Yerba 1 kg", 5, 5,
                new BigDecimal("3900"), new BigDecimal("4100"), new BigDecimal(maximo), BigDecimal.TEN)));
    }

    private static MiniObjetivo miniDe(TipoMiniObjetivo tipo, RegistroId registroId, String referencia) {
        MiniObjetivo mini = miniObjetivo();
        mini.setTipo(tipo);
        mini.setRegistroId(registroId);
        mini.setReferencia(referencia);
        return mini;
    }
}

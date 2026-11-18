package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ZonasDePrueba.NOMBRE_ZONA;
import static com.ensambla.backend.support.ZonasDePrueba.nombresDe;
import static com.ensambla.backend.support.ZonasDePrueba.unAnalisisDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unProductoDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unProductoDeZonaCobradoPor;
import static org.assertj.core.api.Assertions.assertThat;

class AnalisisZonaTest {

    private static final int MINIMO = 3;
    private static final List<String> PROPIOS = List.of("Aceite Natura 1 L", "Yerba Playadito 1 kg");

    private AnalisisZona analisis;
    private AnalisisZona resultado;
    private Optional<AgregadoZonaProducto> rango;
    private List<AgregadoZonaProducto> ranking;

    @Test
    @DisplayName("Sin zona no tiene nombre, comercios ni productos")
    void sinZonaNoTieneNombreComerciosNiProductos() {
        cuandoSeArmaUnAnalisisSinZona();

        entoncesNoTieneNombreNiComerciosNiProductos();
    }

    @Test
    @DisplayName("Quita los productos cuando hay menos comercios que el mínimo")
    void quitaLosProductosCuandoHayMenosComerciosQueElMinimo() {
        dadoUnAnalisisDeComerciosConUnaYerba(2);

        cuandoSeQuitanLosProductosPorDebajoDelMinimo();

        entoncesQuedanElNombreYLosComerciosSinProductos();
    }

    @Test
    @DisplayName("Conserva los productos cuando los comercios alcanzan el mínimo")
    void conservaLosProductosCuandoLosComerciosAlcanzanElMinimo() {
        dadoUnAnalisisDeComerciosConUnaYerba(MINIMO);

        cuandoSeQuitanLosProductosPorDebajoDelMinimo();

        entoncesSeConservaElMismoAnalisis();
    }

    @Test
    @DisplayName("Ordena por comercios que venden y desempata por unidades por semana")
    void ordenaPorComerciosQueVendenYDesempataPorUnidadesPorSemana() {
        dadoUnAnalisisDeDiezComerciosConAzucarYerbaYFideos();

        cuandoSeOrdenaPorVentas();

        entoncesLosProductosVanEnEsteOrden("Yerba", "Fideos", "Azúcar");
        entoncesSeConservanElNombreYLosDiezComercios();
    }

    @Test
    @DisplayName("Encuentra el producto de la zona sin importar tildes ni mayúsculas")
    void encuentraElProductoDeLaZonaSinImportarTildesNiMayusculas() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(unProductoDeZona("Azúcar Ledesma 1 kg", 8, "5"));

        cuandoSeBuscaElRangoDe("AZUCAR ledesma 1 KG");

        entoncesSeEncuentraElProducto();
    }

    @Test
    @DisplayName("No da rango de un producto que la zona no tiene")
    void noDaRangoDeUnProductoQueLaZonaNoTiene() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(unProductoDeZona("Azúcar Ledesma 1 kg", 8, "5"));

        cuandoSeBuscaElRangoDe("Yerba");

        entoncesNoSeEncuentraElProducto();
    }

    @Test
    @DisplayName("No da rango si la zona tiene menos comercios que el mínimo")
    void noDaRangoSiLaZonaTieneMenosComerciosQueElMinimo() {
        dadoUnAnalisisConUnaYerbaVendidaPor(2, 8);

        cuandoSeBuscaElRangoDe("Yerba");

        entoncesNoSeEncuentraElProducto();
    }

    @Test
    @DisplayName("No da rango si el producto lo cobran menos comercios que el mínimo")
    void noDaRangoSiElProductoLoCobranMenosComerciosQueElMinimo() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(unProductoDeZonaCobradoPor("Yerba", 2, 2, "5"));

        cuandoSeBuscaElRangoDe("Yerba");

        entoncesNoSeEncuentraElProducto();
    }

    @Test
    @DisplayName("El ranking de más vendidos va por comercios que venden y desempata por unidades por semana")
    void elRankingDeMasVendidosVaPorComerciosQueVendenYDesempataPorUnidadesPorSemana() {
        dadoUnAnalisisDeDiezComerciosConAzucarYerbaYFideos();

        cuandoSePideElRankingDeMasVendidos();

        entoncesElRankingVaEnEsteOrden("Yerba", "Fideos", "Azúcar");
    }

    @Test
    @DisplayName("El ranking deja afuera lo que venden menos comercios que el mínimo")
    void elRankingDejaAfueraLoQueVendenMenosComerciosQueElMinimo() {
        dadoUnAnalisisDeDiezComerciosConUnaYerbaYUnaSalPocoVendida();

        cuandoSePideElRankingDeMasVendidos();

        entoncesElRankingVaEnEsteOrden("Yerba");
    }

    @Test
    @DisplayName("No hay ranking cuando la zona tiene menos comercios que el mínimo")
    void noHayRankingCuandoLaZonaTieneMenosComerciosQueElMinimo() {
        dadoUnAnalisisDeComerciosConUnaYerba(2);

        cuandoSePideElRankingDeMasVendidos();

        entoncesNoHayRanking();
    }

    @Test
    @DisplayName("Pone primero los productos que el negocio tiene y deja cada grupo en su orden")
    void ponePrimeroLosProductosQueElNegocioTieneYDejaCadaGrupoEnSuOrden() {
        dadoUnAnalisisDeCuatroProductosDeMarca();

        cuandoSePonenPrimeroLosPropios();

        entoncesLosProductosVanEnEsteOrden("Yerba Playadito 500 g", "Aceite Natura 900 ml",
                "Azúcar Ledesma 1 kg", "Fideos Matarazzo 500 g");
        entoncesSeConservanElNombreYLosDiezComercios();
    }

    @Test
    @DisplayName("Pone primero los productos que el negocio no tiene y deja los propios al final")
    void ponePrimeroLosProductosQueElNegocioNoTieneYDejaLosPropiosAlFinal() {
        dadoUnAnalisisDeCuatroProductosDeMarca();

        cuandoSePonenPrimeroLosQueFaltan();

        entoncesLosProductosVanEnEsteOrden("Azúcar Ledesma 1 kg", "Fideos Matarazzo 500 g",
                "Yerba Playadito 500 g", "Aceite Natura 900 ml");
    }

    @Test
    @DisplayName("Sin productos propios deja los de la zona como estaban")
    void sinProductosPropiosDejaLosDeLaZonaComoEstaban() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(unProductoDeZona("Yerba", 9, "5"),
                unProductoDeZona("Azúcar", 8, "5"));

        cuandoSePonenPrimeroLosPropiosSinNingunoCargado();

        entoncesLosProductosVanEnEsteOrden("Yerba", "Azúcar");
    }

    // --- dado ---
    private void dadoUnAnalisisDeComerciosConUnaYerba(long comercios) {
        analisis = unAnalisisDeZona(comercios, unProductoDeZona("Yerba", 2, "5"));
    }

    private void dadoUnAnalisisConUnaYerbaVendidaPor(long comercios, int comerciosVenden) {
        analisis = unAnalisisDeZona(comercios, unProductoDeZona("Yerba", comerciosVenden, "5"));
    }

    private void dadoUnAnalisisDeDiezComerciosConUnProducto(AgregadoZonaProducto... productos) {
        analisis = unAnalisisDeZona(10, productos);
    }

    private void dadoUnAnalisisDeDiezComerciosConAzucarYerbaYFideos() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(unProductoDeZona("Azúcar", 4, "20"),
                unProductoDeZona("Yerba", 9, "5"), unProductoDeZona("Fideos", 4, "30"));
    }

    private void dadoUnAnalisisDeDiezComerciosConUnaYerbaYUnaSalPocoVendida() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(unProductoDeZona("Yerba", 9, "5"), unProductoDeZona("Sal", 2, "50"));
    }

    private void dadoUnAnalisisDeCuatroProductosDeMarca() {
        dadoUnAnalisisDeDiezComerciosConUnProducto(
                unProductoDeZona("Azúcar Ledesma 1 kg", 9, "5"),
                unProductoDeZona("Yerba Playadito 500 g", 8, "5"),
                unProductoDeZona("Fideos Matarazzo 500 g", 7, "5"),
                unProductoDeZona("Aceite Natura 900 ml", 6, "5"));
    }

    // --- cuando ---
    private void cuandoSeArmaUnAnalisisSinZona() {
        resultado = AnalisisZona.sinZona();
    }

    private void cuandoSeQuitanLosProductosPorDebajoDelMinimo() {
        resultado = analisis.sinProductosSiHayMenosDe(MINIMO);
    }

    private void cuandoSeOrdenaPorVentas() {
        resultado = analisis.ordenadoPorVentas();
    }

    private void cuandoSeBuscaElRangoDe(String producto) {
        rango = analisis.productoConRango(producto);
    }

    private void cuandoSePideElRankingDeMasVendidos() {
        ranking = analisis.masVendidos();
    }

    private void cuandoSePonenPrimeroLosPropios() {
        resultado = analisis.conLosPropiosPrimero(PROPIOS);
    }

    private void cuandoSePonenPrimeroLosQueFaltan() {
        resultado = analisis.conLosQueFaltanPrimero(PROPIOS);
    }

    private void cuandoSePonenPrimeroLosPropiosSinNingunoCargado() {
        resultado = analisis.conLosPropiosPrimero(List.of());
    }

    // --- entonces ---
    private void entoncesNoTieneNombreNiComerciosNiProductos() {
        assertThat(resultado.nombre()).isEmpty();
        assertThat(resultado.comercios()).isZero();
        assertThat(resultado.productos()).isEmpty();
    }

    private void entoncesQuedanElNombreYLosComerciosSinProductos() {
        assertThat(resultado.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(resultado.comercios()).isEqualTo(2);
        assertThat(resultado.productos()).isEmpty();
    }

    private void entoncesSeConservaElMismoAnalisis() {
        assertThat(resultado).isSameAs(analisis);
    }

    private void entoncesSeConservanElNombreYLosDiezComercios() {
        assertThat(resultado.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(resultado.comercios()).isEqualTo(10);
    }

    private void entoncesLosProductosVanEnEsteOrden(String... nombres) {
        assertThat(nombresDe(resultado)).containsExactly(nombres);
    }

    private void entoncesSeEncuentraElProducto() {
        assertThat(rango).isPresent();
    }

    private void entoncesNoSeEncuentraElProducto() {
        assertThat(rango).isEmpty();
    }

    private void entoncesElRankingVaEnEsteOrden(String... nombres) {
        assertThat(ranking).extracting(AgregadoZonaProducto::nombre).containsExactly(nombres);
    }

    private void entoncesNoHayRanking() {
        assertThat(ranking).isEmpty();
    }
}

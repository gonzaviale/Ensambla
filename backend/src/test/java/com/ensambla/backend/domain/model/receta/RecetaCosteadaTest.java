package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.RecetasDePrueba.AZUCAR_ID;
import static com.ensambla.backend.support.RecetasDePrueba.BLOQUE_INSUMOS_ID;
import static com.ensambla.backend.support.RecetasDePrueba.BORRADO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.HARINA_ID;
import static com.ensambla.backend.support.RecetasDePrueba.LEVADURA_ID;
import static com.ensambla.backend.support.RecetasDePrueba.PRECIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.losCamposDeLosInsumos;
import static com.ensambla.backend.support.RecetasDePrueba.unInsumoConPrecio;
import static com.ensambla.backend.support.RecetasDePrueba.unInsumoConPrecioQueNoEsUnNumero;
import static com.ensambla.backend.support.RecetasDePrueba.unaLinea;
import static com.ensambla.backend.support.RecetasDePrueba.unaRecetaDe;
import static org.assertj.core.api.Assertions.assertThat;

class RecetaCosteadaTest {

    private Receta receta;
    private List<Registro> insumos;
    private RecetaCosteada costeada;

    @Test
    @DisplayName("El costo es la suma de cantidad por precio de cada insumo, sin redondear")
    void elCostoEsLaSumaDeCantidadPorPrecioDeCadaInsumoSinRedondear() {
        dadoUnaRecetaDeHarinaYAzucar();
        dadoLosInsumosHarinaYAzucarConPrecio();

        cuandoSeCosteaLaReceta();

        entoncesElCostoEsLaSumaDeLosSubtotalesYLaRecetaEstaCompleta();
        entoncesCadaLineaTieneSuPrecioYSuSubtotal();
    }

    @Test
    @DisplayName("Un insumo sin precio no suma y deja la receta incompleta")
    void unInsumoSinPrecioNoSumaYDejaLaRecetaIncompleta() {
        dadoUnaRecetaDeHarinaYLevadura();
        dadoUnInsumoDeHarinaConPrecioYUnaLevaduraConPrecioQueNoEsNumero();

        cuandoSeCosteaLaReceta();

        entoncesSoloSumaLaHarinaYLaRecetaQuedaIncompleta();
        entoncesLaLevaduraQuedaSinPrecioNiSubtotal();
    }

    @Test
    @DisplayName("Un insumo que ya no existe se avisa como eliminado y deja la receta incompleta")
    void unInsumoQueYaNoExisteSeAvisaComoEliminadoYDejaLaRecetaIncompleta() {
        dadoUnaRecetaDeUnInsumoBorrado();
        dadoQueNoQuedaNingunInsumo();

        cuandoSeCosteaLaReceta();

        entoncesElInsumoSeAvisaComoEliminadoYLaRecetaQuedaIncompleta();
    }

    @Test
    @DisplayName("La receta vacía cuesta cero y no está incompleta")
    void laRecetaVaciaCuestaCeroYNoEstaIncompleta() {
        cuandoSeArmaLaRecetaCosteadaVacia();

        entoncesCuestaCeroSinLineasNiTablaDeInsumos();
    }

    // --- dado ---
    private void dadoUnaRecetaDeHarinaYAzucar() {
        receta = unaRecetaDe(unaLinea(HARINA_ID, "0.5", "kg"), unaLinea(AZUCAR_ID, "2", null));
    }

    private void dadoLosInsumosHarinaYAzucarConPrecio() {
        insumos = List.of(
                unInsumoConPrecio(HARINA_ID, "Harina", 1200),
                unInsumoConPrecio(AZUCAR_ID, "Azúcar", 850.5));
    }

    private void dadoUnaRecetaDeHarinaYLevadura() {
        receta = unaRecetaDe(unaLinea(HARINA_ID, "1", null), unaLinea(LEVADURA_ID, "1", null));
    }

    private void dadoUnInsumoDeHarinaConPrecioYUnaLevaduraConPrecioQueNoEsNumero() {
        insumos = List.of(
                unInsumoConPrecio(HARINA_ID, "Harina", 100),
                unInsumoConPrecioQueNoEsUnNumero(LEVADURA_ID, "Levadura"));
    }

    private void dadoUnaRecetaDeUnInsumoBorrado() {
        receta = unaRecetaDe(unaLinea(BORRADO_ID, "1", null));
    }

    private void dadoQueNoQuedaNingunInsumo() {
        insumos = List.of();
    }

    // --- cuando ---
    private void cuandoSeCosteaLaReceta() {
        costeada = RecetaCosteada.de(receta, BLOQUE_INSUMOS_ID, insumos, losCamposDeLosInsumos());
    }

    private void cuandoSeArmaLaRecetaCosteadaVacia() {
        costeada = RecetaCosteada.vacia();
    }

    // --- entonces ---
    private void entoncesElCostoEsLaSumaDeLosSubtotalesYLaRecetaEstaCompleta() {
        assertThat(costeada.costo()).isEqualByComparingTo("2301");
        assertThat(costeada.incompleta()).isFalse();
        assertThat(costeada.campoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(costeada.bloqueInsumosId()).isEqualTo(BLOQUE_INSUMOS_ID);
    }

    private void entoncesCadaLineaTieneSuPrecioYSuSubtotal() {
        assertThat(costeada.lineas()).extracting(RecetaCosteada.Linea::insumo).containsExactly("Harina", "Azúcar");
        assertThat(costeada.lineas().get(0).subtotal()).isEqualByComparingTo("600");
        assertThat(costeada.lineas().get(0).precioUnitario()).isEqualByComparingTo("1200");
        assertThat(costeada.lineas().get(0).unidad()).isEqualTo("kg");
        assertThat(costeada.lineas().get(1).subtotal()).isEqualByComparingTo("1701");
    }

    private void entoncesSoloSumaLaHarinaYLaRecetaQuedaIncompleta() {
        assertThat(costeada.costo()).isEqualByComparingTo("100");
        assertThat(costeada.incompleta()).isTrue();
    }

    private void entoncesLaLevaduraQuedaSinPrecioNiSubtotal() {
        assertThat(costeada.lineas().get(1).insumo()).isEqualTo("Levadura");
        assertThat(costeada.lineas().get(1).precioUnitario()).isNull();
        assertThat(costeada.lineas().get(1).subtotal()).isNull();
    }

    private void entoncesElInsumoSeAvisaComoEliminadoYLaRecetaQuedaIncompleta() {
        assertThat(costeada.costo()).isEqualByComparingTo("0");
        assertThat(costeada.incompleta()).isTrue();
        assertThat(costeada.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.insumo()).isEqualTo("Insumo eliminado");
            assertThat(linea.subtotal()).isNull();
        });
    }

    private void entoncesCuestaCeroSinLineasNiTablaDeInsumos() {
        assertThat(costeada.lineas()).isEmpty();
        assertThat(costeada.costo()).isEqualByComparingTo("0");
        assertThat(costeada.incompleta()).isFalse();
        assertThat(costeada.campoPrecioId()).isNull();
        assertThat(costeada.bloqueInsumosId()).isNull();
    }
}

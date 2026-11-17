package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static org.assertj.core.api.Assertions.assertThat;

class CandidatoMiniObjetivoTest {

    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    private CandidatoMiniObjetivo conTexto;
    private CandidatoMiniObjetivo otro;
    private boolean esElMismo;
    private boolean chocan;

    @Test
    @DisplayName("Con el texto de la IA cambia el título y la justificación y lo marca")
    void conTextoDeIaShouldReplaceTheTextAndMarkIt() {
        whenSeLeDaElTextoDeLaIa();

        thenElTextoEsElDeLaIa();
    }

    @Test
    @DisplayName("Con el texto de la IA no se tocan los números")
    void conTextoDeIaShouldKeepTheNumbers() {
        whenSeLeDaElTextoDeLaIa();

        thenLosNumerosNoCambian();
    }

    @Test
    @DisplayName("Es el mismo cuando coinciden el tipo, el producto y la referencia")
    void esElMismoShouldBeTrueWhenTypeProductAndReferenceMatch() {
        whenSeCompara(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO);

        thenEsElMismo();
    }

    @Test
    @DisplayName("No es el mismo si cambia el tipo")
    void esElMismoShouldBeFalseWhenTypeDiffers() {
        whenSeCompara(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO);

        thenNoEsElMismo();
    }

    @Test
    @DisplayName("No es el mismo si cambia el producto")
    void esElMismoShouldBeFalseWhenProductDiffers() {
        whenSeCompara(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, PRODUCTO);

        thenNoEsElMismo();
    }

    @Test
    @DisplayName("No es el mismo si cambia la referencia")
    void esElMismoShouldBeFalseWhenReferenceDiffers() {
        whenSeCompara(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, "Otra yerba");

        thenNoEsElMismo();
    }

    @Test
    @DisplayName("Chocan si miden el mismo tipo")
    void chocaConShouldBeTrueForTheSameType() {
        givenOtro(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, "Fideos");

        whenSeComparaSiChocan(candidato());

        thenChocan();
    }

    @Test
    @DisplayName("Chocan si miden el mismo producto aunque sean de otro tipo")
    void chocaConShouldBeTrueForTheSameProduct() {
        givenOtro(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO);

        whenSeComparaSiChocan(candidato());

        thenChocan();
    }

    @Test
    @DisplayName("No chocan si cambian el tipo y el producto")
    void chocaConShouldBeFalseWhenTypeAndProductDiffer() {
        givenOtro(TipoMiniObjetivo.PONER_A_LA_VISTA, OTRO_PRODUCTO_ID, "Fideos");

        whenSeComparaSiChocan(candidato());

        thenNoChocan();
    }

    @Test
    @DisplayName("Dos métricas del negocio, sin producto, de distinto tipo no chocan")
    void chocaConShouldBeFalseForBusinessMetricsOfDifferentTypes() {
        otro = candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "100");

        whenSeComparaSiChocan(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "100"));

        thenNoChocan();
    }

    @Test
    @DisplayName("Una métrica del negocio y una de un producto de distinto tipo no chocan")
    void chocaConShouldBeFalseForABusinessMetricAndAProductMetric() {
        otro = candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "100");

        whenSeComparaSiChocan(candidato());

        thenNoChocan();
    }

    // --- arrange ---
    private void givenOtro(TipoMiniObjetivo tipo, RegistroId productoId, String referencia) {
        otro = new CandidatoMiniObjetivo(
                tipo, productoId, referencia, VALOR_INICIAL, META_MINI, APORTE, "t", "j", false);
    }

    // --- act ---
    private void whenSeLeDaElTextoDeLaIa() {
        conTexto = candidato().conTextoDeIa(new TextoMiniObjetivo("Un título", "Una razón"));
    }

    private void whenSeCompara(TipoMiniObjetivo tipo, RegistroId productoId, String referencia) {
        esElMismo = candidato().esElMismo(tipo, productoId, referencia);
    }

    private void whenSeComparaSiChocan(CandidatoMiniObjetivo candidato) {
        chocan = candidato.chocaCon(otro);
    }

    // --- assert ---
    private void thenElTextoEsElDeLaIa() {
        assertThat(conTexto.titulo()).isEqualTo("Un título");
        assertThat(conTexto.justificacion()).isEqualTo("Una razón");
        assertThat(conTexto.textoConIa()).isTrue();
    }

    private void thenLosNumerosNoCambian() {
        assertThat(conTexto.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(conTexto.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(conTexto.referencia()).isEqualTo(PRODUCTO);
        assertThat(conTexto.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(conTexto.meta()).isEqualTo(META_MINI);
        assertThat(conTexto.aporteEstimado()).isEqualTo(APORTE);
    }

    private void thenEsElMismo() {
        assertThat(esElMismo).isTrue();
    }

    private void thenNoEsElMismo() {
        assertThat(esElMismo).isFalse();
    }

    private void thenChocan() {
        assertThat(chocan).isTrue();
    }

    private void thenNoChocan() {
        assertThat(chocan).isFalse();
    }
}

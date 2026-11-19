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
    void conElTextoDeLaIaCambiaElTituloYLaJustificacionYLoMarca() {
        cuandoSeLeDaElTextoDeLaIa();

        entoncesElTextoEsElDeLaIa();
    }

    @Test
    @DisplayName("Con el texto de la IA no se tocan los números")
    void conElTextoDeLaIaNoSeTocanLosNumeros() {
        cuandoSeLeDaElTextoDeLaIa();

        entoncesLosNumerosNoCambian();
    }

    @Test
    @DisplayName("Es el mismo cuando coinciden el tipo, el producto y la referencia")
    void esElMismoCuandoCoincidenElTipoElProductoYLaReferencia() {
        cuandoSeCompara(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO);

        entoncesEsElMismo();
    }

    @Test
    @DisplayName("No es el mismo si cambia el tipo")
    void noEsElMismoSiCambiaElTipo() {
        cuandoSeCompara(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO);

        entoncesNoEsElMismo();
    }

    @Test
    @DisplayName("No es el mismo si cambia el producto")
    void noEsElMismoSiCambiaElProducto() {
        cuandoSeCompara(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, PRODUCTO);

        entoncesNoEsElMismo();
    }

    @Test
    @DisplayName("No es el mismo si cambia la referencia")
    void noEsElMismoSiCambiaLaReferencia() {
        cuandoSeCompara(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, "Otra yerba");

        entoncesNoEsElMismo();
    }

    @Test
    @DisplayName("Chocan si miden el mismo tipo")
    void chocanSiMidenElMismoTipo() {
        dadoOtro(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, "Fideos");

        cuandoSeComparaSiChocan(candidato());

        entoncesChocan();
    }

    @Test
    @DisplayName("Chocan si miden el mismo producto aunque sean de otro tipo")
    void chocanSiMidenElMismoProductoAunqueSeanDeOtroTipo() {
        dadoOtro(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO);

        cuandoSeComparaSiChocan(candidato());

        entoncesChocan();
    }

    @Test
    @DisplayName("No chocan si cambian el tipo y el producto")
    void noChocanSiCambianElTipoYElProducto() {
        dadoOtro(TipoMiniObjetivo.PONER_A_LA_VISTA, OTRO_PRODUCTO_ID, "Fideos");

        cuandoSeComparaSiChocan(candidato());

        entoncesNoChocan();
    }

    @Test
    @DisplayName("Dos métricas del negocio, sin producto, de distinto tipo no chocan")
    void dosMetricasDelNegocioSinProductoDeDistintoTipoNoChocan() {
        otro = candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "100");

        cuandoSeComparaSiChocan(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "100"));

        entoncesNoChocan();
    }

    @Test
    @DisplayName("Una métrica del negocio y una de un producto de distinto tipo no chocan")
    void unaMetricaDelNegocioYUnaDeUnProductoDeDistintoTipoNoChocan() {
        otro = candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "100");

        cuandoSeComparaSiChocan(candidato());

        entoncesNoChocan();
    }

    // --- arrange ---
    private void dadoOtro(TipoMiniObjetivo tipo, RegistroId productoId, String referencia) {
        otro = new CandidatoMiniObjetivo(
                tipo, productoId, referencia, VALOR_INICIAL, META_MINI, APORTE, "t", "j", false);
    }

    // --- act ---
    private void cuandoSeLeDaElTextoDeLaIa() {
        conTexto = candidato().conTextoDeIa(new TextoMiniObjetivo("Un título", "Una razón"));
    }

    private void cuandoSeCompara(TipoMiniObjetivo tipo, RegistroId productoId, String referencia) {
        esElMismo = candidato().esElMismo(tipo, productoId, referencia);
    }

    private void cuandoSeComparaSiChocan(CandidatoMiniObjetivo candidato) {
        chocan = candidato.chocaCon(otro);
    }

    // --- assert ---
    private void entoncesElTextoEsElDeLaIa() {
        assertThat(conTexto.titulo()).isEqualTo("Un título");
        assertThat(conTexto.justificacion()).isEqualTo("Una razón");
        assertThat(conTexto.textoConIa()).isTrue();
    }

    private void entoncesLosNumerosNoCambian() {
        assertThat(conTexto.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(conTexto.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(conTexto.referencia()).isEqualTo(PRODUCTO);
        assertThat(conTexto.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(conTexto.meta()).isEqualTo(META_MINI);
        assertThat(conTexto.aporteEstimado()).isEqualTo(APORTE);
    }

    private void entoncesEsElMismo() {
        assertThat(esElMismo).isTrue();
    }

    private void entoncesNoEsElMismo() {
        assertThat(esElMismo).isFalse();
    }

    private void entoncesChocan() {
        assertThat(chocan).isTrue();
    }

    private void entoncesNoChocan() {
        assertThat(chocan).isFalse();
    }
}

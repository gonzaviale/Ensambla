package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaTest {

    private int totalPaginas;
    private Pagina<Integer> transformada;

    @Test
    @DisplayName("Una consulta sin resultados tiene una página vacía")
    void unaConsultaSinResultadosTieneUnaPaginaVacia() {
        cuandoSeCuentanLasPaginasDe(0);

        entoncesHayPaginas(1);
    }

    @Test
    @DisplayName("Redondea hacia arriba la cantidad de páginas")
    void redondeaHaciaArribaLaCantidadDePaginas() {
        cuandoSeCuentanLasPaginasDe(45);

        entoncesHayPaginas(3);
    }

    @Test
    @DisplayName("Una cantidad exacta de páginas no suma una página de más")
    void unaCantidadExactaDePaginasNoSumaUnaPaginaDeMas() {
        cuandoSeCuentanLasPaginasDe(40);

        entoncesHayPaginas(2);
    }

    @Test
    @DisplayName("Transforma el contenido y conserva los datos de la página")
    void transformaElContenidoYConservaLosDatosDeLaPagina() {
        cuandoSeTransformaElContenidoALargos();

        entoncesSeConservanLosDatosDeLaPaginaConElContenidoTransformado();
    }

    // --- cuando ---
    private void cuandoSeCuentanLasPaginasDe(long totalElementos) {
        totalPaginas = new Pagina<String>(List.of(), 0, 20, totalElementos).totalPaginas();
    }

    private void cuandoSeTransformaElContenidoALargos() {
        transformada = new Pagina<>(List.of("a", "bb"), 2, 20, 42).map(String::length);
    }

    // --- entonces ---
    private void entoncesHayPaginas(int esperadas) {
        assertThat(totalPaginas).isEqualTo(esperadas);
    }

    private void entoncesSeConservanLosDatosDeLaPaginaConElContenidoTransformado() {
        assertThat(transformada.contenido()).containsExactly(1, 2);
        assertThat(transformada.numero()).isEqualTo(2);
        assertThat(transformada.tamanio()).isEqualTo(20);
        assertThat(transformada.totalElementos()).isEqualTo(42);
    }
}

package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaTest {

    @Test
    @DisplayName("Una consulta sin resultados tiene una página vacía")
    void totalPaginasShouldBeOneWhenThereAreNoElements() {
        assertThat(pagina(0).totalPaginas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redondea hacia arriba la cantidad de páginas")
    void totalPaginasShouldRoundUp() {
        assertThat(pagina(45).totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Una cantidad exacta de páginas no suma una página de más")
    void totalPaginasShouldBeExactWhenDivisible() {
        assertThat(pagina(40).totalPaginas()).isEqualTo(2);
    }

    // --- helpers ---
    private Pagina<String> pagina(long total) {
        return new Pagina<>(List.of(), 0, 20, total);
    }
}

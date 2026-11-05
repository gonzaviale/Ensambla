package com.ensambla.backend.domain.useCase.reposicion;

import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.ACEITE_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.datosCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultarReposicionResponseTest {

    @Test
    @DisplayName("Copia todos los datos de cada producto y deja sin proveedor al que no tiene")
    void deShouldCopyEveryFieldOfEachProduct() {
        Faltantes faltantes = Faltantes.de(datosCon(inventarioCon(
                producto(YERBA_ID, "Yerba", 3, 10, "Molinos"),
                producto(ACEITE_ID, "Aceite", 0, 10, null))));

        ConsultarReposicionResponse result = ConsultarReposicionResponse.de(faltantes);

        assertThat(result.grupos()).extracting(grupo -> grupo.proveedor()).containsExactly("Molinos", null);
        assertThat(result.grupos().get(0).productos()).singleElement().satisfies(producto -> {
            assertThat(producto.id()).isEqualTo(YERBA_ID);
            assertThat(producto.nombre()).isEqualTo("Yerba");
            assertThat(producto.cantidadActual()).isEqualByComparingTo("3");
            assertThat(producto.cantidadAPedir()).isEqualByComparingTo("17");
            assertThat(producto.situacion()).isEqualTo("Quedan 3 (mínimo 10)");
        });
    }

    @Test
    @DisplayName("Sin faltantes la lista de grupos queda vacía")
    void deShouldReturnNoGroupsWhenNothingIsMissing() {
        ConsultarReposicionResponse result = ConsultarReposicionResponse.de(new Faltantes(List.of()));

        assertThat(result.grupos()).isEmpty();
    }
}

package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FuenteCatalogoTest {

    private FuenteCatalogo fuente;

    private Throwable rechazo;

    @Test
    @DisplayName("La clave sepa elige el catálogo SEPA")
    void laClaveSepaEligeElCatalogoSepa() {
        cuandoSeEligeLaFuente("sepa");

        entoncesLaFuenteEs(FuenteCatalogo.SEPA);
    }

    @Test
    @DisplayName("La clave openfoodfacts elige Open Food Facts")
    void laClaveOpenfoodfactsEligeOpenFoodFacts() {
        cuandoSeEligeLaFuente("openfoodfacts");

        entoncesLaFuenteEs(FuenteCatalogo.OPEN_FOOD_FACTS);
    }

    @Test
    @DisplayName("Una clave que no es de una fuente pública configurada se rechaza")
    void unaClaveDesconocidaSeRechaza() {
        cuandoSeIntentaElegirLaFuente("https://otro.test");

        entoncesSeRechazaPorqueElCatalogoNoEstaDisponible();
    }

    @Test
    @DisplayName("Sin clave no hay catálogo disponible")
    void sinClaveSeRechaza() {
        cuandoSeIntentaElegirLaFuente(null);

        entoncesSeRechazaPorqueElCatalogoNoEstaDisponible();
    }

    // --- cuando ---
    private void cuandoSeEligeLaFuente(String clave) {
        fuente = FuenteCatalogo.de(clave);
    }

    private void cuandoSeIntentaElegirLaFuente(String clave) {
        rechazo = catchThrowable(() -> FuenteCatalogo.de(clave));
    }

    // --- entonces ---
    private void entoncesLaFuenteEs(FuenteCatalogo esperada) {
        assertThat(fuente).isEqualTo(esperada);
    }

    private void entoncesSeRechazaPorqueElCatalogoNoEstaDisponible() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("El catálogo indicado no está disponible.");
    }
}

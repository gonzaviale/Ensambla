package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampoMonedaTest extends CampoNumericoTest {

    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.MONEDA;
    }

    @Test
    @DisplayName("Una moneda no puede cambiar de tipo si una fórmula la usa")
    void validarCambioDeTipoShouldRejectWhenAFormulaUsesIt() {
        Campo precio = givenPrecioEnMoneda();
        List<Campo> camposDelBloque = List.of(precio, givenFormulaQueUsa(precio));

        assertThatThrownBy(() -> precio.validarCambioDeTipo(camposDelBloque))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("la fórmula «Total» lo usa");
    }

    private Campo givenPrecioEnMoneda() {
        return Campo.builder().id(PRECIO_ID).tipo(TipoCampo.MONEDA).nombre("Precio").build();
    }

    private Campo givenFormulaQueUsa(Campo precio) {
        return Campo.builder()
                .tipo(TipoCampo.FORMULA)
                .nombre("Total")
                .formula("{" + precio.getId() + "} * 2")
                .build();
    }
}

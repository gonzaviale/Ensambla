package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class CampoNumeroTest extends CampoNumericoTest {

    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.NUMERO;
    }

    @Test
    @DisplayName("Un número puede seguir siendo número aunque una fórmula lo use")
    void validarCambioDeTipoShouldAllowTheChangeWhenAFormulaUsesIt() {
        Campo precio = givenPrecioUsadoPorUnaFormula();
        List<Campo> camposDelBloque = List.of(precio, givenFormulaQueUsa(precio));

        assertThatCode(() -> precio.validarCambioDeTipo(camposDelBloque)).doesNotThrowAnyException();
    }

    private Campo givenPrecioUsadoPorUnaFormula() {
        return Campo.builder().id(PRECIO_ID).tipo(TipoCampo.NUMERO).nombre("Precio").build();
    }

    private Campo givenFormulaQueUsa(Campo precio) {
        return Campo.builder()
                .tipo(TipoCampo.FORMULA)
                .nombre("Total")
                .formula("{" + precio.getId() + "} * 2")
                .build();
    }
}

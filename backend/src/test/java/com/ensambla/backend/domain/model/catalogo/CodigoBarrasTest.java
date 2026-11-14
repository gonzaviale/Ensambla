package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoBarrasTest {

    @Test
    @DisplayName("Admite GTIN válidos y conserva ceros iniciales al comparar presentaciones equivalentes")
    void identidadShouldNormalizeValidCodes() {
        for (String codigo : new String[]{"77903754", "00000077903754", "036000291452", "7790895000997"}) {
            CodigoBarras result = new CodigoBarras(codigo);

            assertThat(result.valor()).isEqualTo(codigo);
            assertThat(result.identidad()).hasSize(14).endsWith(codigo);
        }
    }

    @Test
    @DisplayName("Rechaza nulos, longitudes, caracteres, ceros y dígitos de control inválidos")
    void constructorShouldRejectInvalidCode() {
        for (String codigo : new String[]{null, "", "123", "abcd5678", "00000000", "77903755"}) {
            assertThatThrownBy(() -> new CodigoBarras(codigo)).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    @DisplayName("Sólo admite las dos fuentes públicas configuradas")
    void fuenteShouldRejectUnknownSource() {
        assertThat(FuenteCatalogo.de("sepa")).isEqualTo(FuenteCatalogo.SEPA);
        assertThat(FuenteCatalogo.de("openfoodfacts")).isEqualTo(FuenteCatalogo.OPEN_FOOD_FACTS);

        assertThatThrownBy(() -> FuenteCatalogo.de("https://otro.test"))
                .isInstanceOf(BusinessRuleException.class);
    }
}

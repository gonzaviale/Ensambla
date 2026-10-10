package com.ensambla.backend.domain.model.enums;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DireccionOrdenTest {

    @Test
    @DisplayName("Sin valor ordena de forma descendente")
    void desdeShouldReturnDescWhenValueIsNullOrBlank() {
        assertThat(DireccionOrden.desde(null)).isEqualTo(DireccionOrden.DESC);
        assertThat(DireccionOrden.desde("  ")).isEqualTo(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Interpreta asc y desc sin distinguir mayúsculas ni espacios")
    void desdeShouldParseAscAndDesc() {
        assertThat(DireccionOrden.desde("ASC")).isEqualTo(DireccionOrden.ASC);
        assertThat(DireccionOrden.desde(" desc ")).isEqualTo(DireccionOrden.DESC);
    }

    @Test
    @DisplayName("Rechaza cualquier otro valor")
    void desdeShouldRejectUnknownValue() {
        assertThatThrownBy(() -> DireccionOrden.desde("arriba"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La dirección del orden tiene que ser asc o desc");
    }
}

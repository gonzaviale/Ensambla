package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormulaTest {

    private static final UUID CANTIDAD_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID PRECIO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Multiplica dos campos del registro")
    void evaluarShouldMultiplyFields() {
        Formula formula = Formula.parsear(ref(CANTIDAD_ID) + " * " + ref(PRECIO_ID));

        BigDecimal result = formula.evaluar(valores(Map.of(CANTIDAD_ID, "3", PRECIO_ID, "12.5")));

        assertThat(result).isEqualByComparingTo("37.5");
    }

    @Test
    @DisplayName("Respeta la precedencia, los paréntesis y el menos unario")
    void evaluarShouldRespectPrecedenceParenthesesAndUnaryMinus() {
        assertThat(evaluar("2 + 3 * 4")).isEqualByComparingTo("14");
        assertThat(evaluar("(2 + 3) * 4")).isEqualByComparingTo("20");
        assertThat(evaluar("-(2 - 5) - 1")).isEqualByComparingTo("2");
        assertThat(evaluar("10 / 4")).isEqualByComparingTo("2.5");
    }

    @Test
    @DisplayName("Devuelve el resultado sin ceros de más ni notación científica")
    void evaluarShouldStripTrailingZeros() {
        assertThat(evaluar("1500 * 1").toString()).isEqualTo("1500");
        assertThat(evaluar("2.50 * 1").toString()).isEqualTo("2.5");
        assertThat(evaluar("0 * 5").toString()).isEqualTo("0");
    }

    @Test
    @DisplayName("Queda sin valor cuando falta un dato o se divide por cero")
    void evaluarShouldReturnNullWhenAValueIsMissingOrDividesByZero() {
        Formula conDato = Formula.parsear(ref(CANTIDAD_ID) + " * 2");
        Formula negada = Formula.parsear("-" + ref(CANTIDAD_ID));
        Formula dividida = Formula.parsear("5 / 0");

        assertThat(conDato.evaluar(id -> null)).isNull();
        assertThat(negada.evaluar(id -> null)).isNull();
        assertThat(Formula.parsear("2 * " + ref(CANTIDAD_ID)).evaluar(id -> null)).isNull();
        assertThat(dividida.evaluar(id -> null)).isNull();
    }

    @Test
    @DisplayName("Informa los campos que usa, sin repetir")
    void referenciasShouldListUsedFieldsOnce() {
        Formula formula = Formula.parsear(ref(CANTIDAD_ID) + " * " + ref(PRECIO_ID) + " + " + ref(CANTIDAD_ID));

        assertThat(formula.referencias()).containsExactly(CANTIDAD_ID, PRECIO_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "2 +", "(2 + 3", "2 3", "abc", "*", ".", "{no-es-un-id}",
            "{c0000000-0000-4000-8000-000000000001", "{c0000000-0000-4000-8000-000000000001 + 1", "{zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz}", "2 + )"})
    @DisplayName("Rechaza una expresión mal armada")
    void parsearShouldRejectMalformedExpressions(String texto) {
        assertThatThrownBy(() -> Formula.parsear(texto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rechaza una expresión nula")
    void parsearShouldRejectNull() {
        assertThatThrownBy(() -> Formula.parsear(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Armá la fórmula");
    }

    @Test
    @DisplayName("Rechaza una expresión demasiado larga")
    void parsearShouldRejectTooLongExpressions() {
        String larga = "1 + ".repeat(Formula.MAX_CARACTERES) + "1";

        assertThatThrownBy(() -> Formula.parsear(larga))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Máximo " + Formula.MAX_CARACTERES + " caracteres");
    }

    // --- helpers ---
    private BigDecimal evaluar(String texto) {
        return Formula.parsear(texto).evaluar(id -> null);
    }

    private static String ref(UUID id) {
        return "{" + id + "}";
    }

    private static java.util.function.Function<UUID, BigDecimal> valores(Map<UUID, String> valores) {
        return id -> new BigDecimal(valores.get(id));
    }
}

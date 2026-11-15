package com.ensambla.backend.domain.model.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CuotaObjetivosIaTest {

    @Test
    @DisplayName("Con cuota disponible dice cuántos objetivos quedan")
    void restantesShouldBeTheLimitMinusTheUsed() {
        CuotaObjetivosIa result = new CuotaObjetivosIa(2, 5);

        assertThat(result.restantes()).isEqualTo(3);
        assertThat(result.agotada()).isFalse();
    }

    @Test
    @DisplayName("Con todos los objetivos usados la cuota está agotada y no quedan")
    void agotadaShouldBeTrueWhenTheLimitIsReached() {
        CuotaObjetivosIa result = new CuotaObjetivosIa(5, 5);

        assertThat(result.restantes()).isZero();
        assertThat(result.agotada()).isTrue();
    }

    @Test
    @DisplayName("Usar de más no da restantes negativos")
    void restantesShouldNeverBeNegative() {
        CuotaObjetivosIa result = new CuotaObjetivosIa(7, 5);

        assertThat(result.restantes()).isZero();
        assertThat(result.agotada()).isTrue();
    }

    @Test
    @DisplayName("Sin límite nunca se agota y no hay restantes que contar")
    void withoutLimitShouldNeverRunOut() {
        CuotaObjetivosIa result = new CuotaObjetivosIa(500, null);

        assertThat(result.restantes()).isNull();
        assertThat(result.agotada()).isFalse();
    }
}

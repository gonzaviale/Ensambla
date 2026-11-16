package com.ensambla.backend.domain.model.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CuotaObjetivosIaTest {

    private CuotaObjetivosIa cuota;

    @Test
    @DisplayName("Con cuota disponible dice cuántos objetivos quedan")
    void restantesShouldBeTheLimitMinusTheUsed() {
        whenSeArmaLaCuota(2, 5);

        thenQuedan(3);
        thenNoEstaAgotada();
    }

    @Test
    @DisplayName("Con todos los objetivos usados la cuota está agotada y no quedan")
    void agotadaShouldBeTrueWhenTheLimitIsReached() {
        whenSeArmaLaCuota(5, 5);

        thenNoQuedanObjetivos();
        thenEstaAgotada();
    }

    @Test
    @DisplayName("Usar de más no da restantes negativos")
    void restantesShouldNeverBeNegative() {
        whenSeArmaLaCuota(7, 5);

        thenNoQuedanObjetivos();
        thenEstaAgotada();
    }

    @Test
    @DisplayName("Sin límite nunca se agota y no hay restantes que contar")
    void withoutLimitShouldNeverRunOut() {
        whenSeArmaLaCuota(500, null);

        thenNoHayRestantesQueContar();
        thenNoEstaAgotada();
    }

    // --- act ---
    private void whenSeArmaLaCuota(int usados, Integer limite) {
        cuota = new CuotaObjetivosIa(usados, limite);
    }

    // --- assert ---
    private void thenQuedan(int restantes) {
        assertThat(cuota.restantes()).isEqualTo(restantes);
    }

    private void thenNoQuedanObjetivos() {
        assertThat(cuota.restantes()).isZero();
    }

    private void thenNoHayRestantesQueContar() {
        assertThat(cuota.restantes()).isNull();
    }

    private void thenEstaAgotada() {
        assertThat(cuota.agotada()).isTrue();
    }

    private void thenNoEstaAgotada() {
        assertThat(cuota.agotada()).isFalse();
    }
}

package com.ensambla.backend.domain.model.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CuotaObjetivosIaTest {

    private CuotaObjetivosIa cuota;

    @Test
    @DisplayName("Con cuota disponible dice cuántos objetivos quedan")
    void conCuotaDisponibleDiceCuantosObjetivosQuedan() {
        cuandoSeArmaLaCuota(2, 5);

        entoncesQuedan(3);
        entoncesNoEstaAgotada();
    }

    @Test
    @DisplayName("Con todos los objetivos usados la cuota está agotada y no quedan")
    void conTodosLosObjetivosUsadosLaCuotaEstaAgotadaYNoQuedan() {
        cuandoSeArmaLaCuota(5, 5);

        entoncesNoQuedanObjetivos();
        entoncesEstaAgotada();
    }

    @Test
    @DisplayName("Usar de más no da restantes negativos")
    void usarDeMasNoDaRestantesNegativos() {
        cuandoSeArmaLaCuota(7, 5);

        entoncesNoQuedanObjetivos();
        entoncesEstaAgotada();
    }

    @Test
    @DisplayName("Sin límite nunca se agota y no hay restantes que contar")
    void sinLimiteNuncaSeAgotaYNoHayRestantesQueContar() {
        cuandoSeArmaLaCuota(500, null);

        entoncesNoHayRestantesQueContar();
        entoncesNoEstaAgotada();
    }

    // --- act ---
    private void cuandoSeArmaLaCuota(int usados, Integer limite) {
        cuota = new CuotaObjetivosIa(usados, limite);
    }

    // --- assert ---
    private void entoncesQuedan(int restantes) {
        assertThat(cuota.restantes()).isEqualTo(restantes);
    }

    private void entoncesNoQuedanObjetivos() {
        assertThat(cuota.restantes()).isZero();
    }

    private void entoncesNoHayRestantesQueContar() {
        assertThat(cuota.restantes()).isNull();
    }

    private void entoncesEstaAgotada() {
        assertThat(cuota.agotada()).isTrue();
    }

    private void entoncesNoEstaAgotada() {
        assertThat(cuota.agotada()).isFalse();
    }
}

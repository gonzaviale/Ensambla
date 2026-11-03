package com.ensambla.backend.domain.model.precio;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CambioPrecioTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-20T01:00:00Z");

    @Test
    @DisplayName("Registra el cambio entre dos precios distintos")
    void entreShouldReturnChangeWhenPricesDiffer() {
        CambioPrecio cambio = whenEntre("1000", "1200");

        assertThat(cambio).isEqualTo(new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, new BigDecimal("1000"),
                new BigDecimal("1200"), AHORA));
    }

    @Test
    @DisplayName("El primer precio queda sin precio anterior, también cuando el anterior no era un precio")
    void entreShouldLeaveAnteriorEmptyWhenThereWasNoPrice() {
        assertThat(whenEntre(null, "1200").anterior()).isNull();
        assertThat(whenEntre("0", "1200").anterior()).isNull();
    }

    @Test
    @DisplayName("No hay cambio si el precio es el mismo, aunque cambie la escala")
    void entreShouldReturnNullWhenPriceIsTheSame() {
        assertThat(whenEntre("1000", "1000.00")).isNull();
    }

    @Test
    @DisplayName("No hay cambio si el precio nuevo falta o no es positivo")
    void entreShouldReturnNullWhenNewPriceIsNotAPrice() {
        assertThat(whenEntre("1000", null)).isNull();
        assertThat(whenEntre("1000", "0")).isNull();
    }

    @Test
    @DisplayName("Sólo es un aumento si el precio subió respecto de uno anterior")
    void esAumentoShouldBeTrueOnlyWhenPriceRose() {
        assertThat(whenEntre("1000", "1200").esAumento()).isTrue();
        assertThat(whenEntre("1200", "1000").esAumento()).isFalse();
        assertThat(whenEntre(null, "1200").esAumento()).isFalse();
    }

    @Test
    @DisplayName("La fecha es el día argentino del cambio, aunque en UTC ya sea el día siguiente")
    void fechaShouldBeTheArgentinaDay() {
        assertThat(whenEntre("1000", "1200").fecha()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    // --- act ---
    private static CambioPrecio whenEntre(String anterior, String nuevo) {
        return CambioPrecio.entre(NEGOCIO_ID, REGISTRO_ID, anterior == null ? null : new BigDecimal(anterior),
                nuevo == null ? null : new BigDecimal(nuevo), AHORA);
    }
}

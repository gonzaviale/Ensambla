package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PromocionDePrueba.contexto;
import static org.assertj.core.api.Assertions.assertThat;

/** Lo que cumple toda idea de promoción; cada regla la hereda y suma lo propio. */
abstract class ReglaPromocionContratoTest {

    protected abstract ReglaPromocion crear();

    /** Un negocio donde la regla sí propone algo. */
    protected abstract ContextoPromocion contextoQueAplica();

    @Test
    @DisplayName("Sin productos no propone nada")
    void evaluarShouldBeEmptyWhenThereAreNoProducts() {
        Optional<Promocion> result = crear().evaluar(contexto(List.of()));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Cuando aplica propone una promoción con título, gancho y al menos un producto")
    void evaluarShouldProposeACompletePromotionWhenItApplies() {
        Optional<Promocion> result = crear().evaluar(contextoQueAplica());

        assertThat(result).hasValueSatisfying(promocion -> {
            assertThat(promocion.clave()).isNotBlank();
            assertThat(promocion.titulo()).isNotBlank();
            assertThat(promocion.gancho()).isNotBlank();
            assertThat(promocion.productos()).isNotEmpty();
        });
    }
}

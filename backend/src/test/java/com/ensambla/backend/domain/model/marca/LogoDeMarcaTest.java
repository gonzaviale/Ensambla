package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogoDeMarcaTest {

    private static final byte[] IMAGEN = {1, 2, 3};

    @Test
    @DisplayName("Acepta una imagen PNG o JPG que no pasa el tamaño máximo")
    void constructorShouldAcceptSmallPngAndJpg() {
        assertThat(new LogoDeMarca(IMAGEN, "image/png").tipoMime()).isEqualTo("image/png");
        assertThat(new LogoDeMarca(IMAGEN, "image/jpeg").contenido()).isEqualTo(IMAGEN);
    }

    @Test
    @DisplayName("Rechaza un logo vacío o ausente")
    void constructorShouldRejectEmpty() {
        assertThatThrownBy(() -> new LogoDeMarca(new byte[0], "image/png"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("vacío");
        assertThatThrownBy(() -> new LogoDeMarca(null, "image/png")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza un logo de más de 2 MB")
    void constructorShouldRejectTooBig() {
        byte[] enorme = new byte[LogoDeMarca.TAMANIO_MAXIMO + 1];

        assertThatThrownBy(() -> new LogoDeMarca(enorme, "image/png"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("2 MB");
    }

    @Test
    @DisplayName("Rechaza un tipo que no es PNG ni JPG, o que no se informó")
    void constructorShouldRejectOtherTypes() {
        assertThatThrownBy(() -> new LogoDeMarca(IMAGEN, "image/gif")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new LogoDeMarca(IMAGEN, null)).isInstanceOf(BusinessRuleException.class);
    }
}

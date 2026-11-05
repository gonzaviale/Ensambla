package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoDeCanalesTest {

    private final CatalogoDeCanales catalogo = new CatalogoDeCanales(
            List.of(new CanalInstagram(), new CanalEstadoWhatsapp(), new CanalMensajeAClientes()));

    @Test
    @DisplayName("Devuelve todos los canales en el orden en que se registraron")
    void todosShouldKeepTheRegistrationOrder() {
        List<CanalPromocion> result = catalogo.todos();

        assertThat(result).extracting(CanalPromocion::clave).containsExactly("instagram", "estado", "clientes");
    }

    @Test
    @DisplayName("Busca el canal por su clave")
    void porClaveShouldFindTheChannel() {
        CanalPromocion result = catalogo.porClave("estado");

        assertThat(result).isInstanceOf(CanalEstadoWhatsapp.class);
    }

    @Test
    @DisplayName("Una clave que no conoce se rechaza")
    void porClaveShouldRejectAnUnknownChannel() {
        assertThatThrownBy(() -> catalogo.porClave("telegram"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No conocemos ese canal");
    }
}

package com.ensambla.backend.domain.model.aviso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SeccionLoQuePideLaZonaTest extends SeccionResumenContratoTest {

    @Override
    protected SeccionResumen crear() {
        return new SeccionLoQuePideLaZona();
    }

    @Override
    protected Aviso avisoQueCuenta() {
        return aviso(TipoAviso.CONSUMO_ZONA, 0);
    }

    @Override
    protected Aviso avisoQueNoCuenta() {
        return aviso(TipoAviso.STOCK, 0);
    }

    @Test
    @DisplayName("Con uno solo lo dice en singular")
    void textoShouldUseTheSingularForOne() {
        Optional<String> result = crear().texto(List.of(avisoQueCuenta()));

        assertThat(result).contains("1 producto que se vende cerca tuyo y todavía no tenés");
    }

    @Test
    @DisplayName("Con varios lo dice en plural")
    void textoShouldUseThePluralForSeveral() {
        Optional<String> result = crear().texto(List.of(avisoQueCuenta(), avisoQueCuenta()));

        assertThat(result).contains("2 productos que se venden cerca tuyo y todavía no tenés");
    }
}

package com.ensambla.backend.domain.model.aviso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SeccionAvisosDePrecioTest extends SeccionResumenContratoTest {

    @Override
    protected SeccionResumen crear() {
        return new SeccionAvisosDePrecio();
    }

    @Override
    protected Aviso avisoQueCuenta() {
        return aviso(TipoAviso.PRECIO_ZONA, 0);
    }

    @Override
    protected Aviso avisoQueNoCuenta() {
        return aviso(TipoAviso.STOCK, 0);
    }

    @Test
    @DisplayName("Con uno solo lo dice en singular")
    void textoShouldUseTheSingularForOne() {
        Optional<String> result = crear().texto(List.of(avisoQueCuenta()));

        assertThat(result).contains("1 aviso de precio");
    }

    @Test
    @DisplayName("Cuenta los cuatro tipos de aviso de precio y los dice en plural")
    void textoShouldCountEveryPriceAvisoType() {
        List<Aviso> avisos = List.of(
                aviso(TipoAviso.PRECIO_ZONA, 0), aviso(TipoAviso.ESTANCADO, 0),
                aviso(TipoAviso.MUY_VENDIDO, 0), aviso(TipoAviso.REVISAR_PRECIO, 0),
                aviso(TipoAviso.COMBO, 0));

        Optional<String> result = crear().texto(avisos);

        assertThat(result).contains("4 avisos de precio");
    }
}

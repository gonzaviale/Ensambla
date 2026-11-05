package com.ensambla.backend.domain.model.aviso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SeccionAgotadosTest extends SeccionResumenContratoTest {

    @Override
    protected SeccionResumen crear() {
        return new SeccionAgotados();
    }

    @Override
    protected Aviso avisoQueCuenta() {
        return aviso(TipoAviso.STOCK, 0);
    }

    @Override
    protected Aviso avisoQueNoCuenta() {
        return aviso(TipoAviso.STOCK, 3);
    }

    @Test
    @DisplayName("Con uno solo lo dice en singular")
    void textoShouldUseTheSingularForOne() {
        Optional<String> result = crear().texto(List.of(avisoQueCuenta()));

        assertThat(result).contains("1 producto agotado");
    }

    @Test
    @DisplayName("Con varios lo dice en plural, y una cantidad negativa también es agotado")
    void textoShouldUseThePluralForSeveral() {
        Optional<String> result = crear().texto(List.of(avisoQueCuenta(), aviso(TipoAviso.STOCK, -1)));

        assertThat(result).contains("2 productos agotados");
    }

    @Test
    @DisplayName("Un aviso que no es de stock no es un producto agotado aunque diga cero")
    void textoShouldIgnoreAvisosThatAreNotAboutStock() {
        Optional<String> result = crear().texto(List.of(aviso(TipoAviso.PRECIO_ZONA, 0)));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Un aviso de stock sin cantidad en sus datos no cuenta como agotado")
    void textoShouldIgnoreAStockAvisoWithoutQuantity() {
        Aviso sinCantidad = Aviso.builder().tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK)
                .estado(EstadoAviso.PENDIENTE).datos(Map.of()).build();

        Optional<String> result = crear().texto(List.of(sinCantidad));

        assertThat(result).isEmpty();
    }
}

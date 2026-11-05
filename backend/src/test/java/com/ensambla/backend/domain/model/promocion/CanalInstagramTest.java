package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PromocionDePrueba.promocionDeYerba;
import static org.assertj.core.api.Assertions.assertThat;

class CanalInstagramTest extends CanalPromocionContratoTest {

    @Override
    protected CanalPromocion crear() {
        return new CanalInstagram();
    }

    @Test
    @DisplayName("Se llama instagram")
    void claveShouldBeInstagram() {
        assertThat(crear().clave()).isEqualTo("instagram");
        assertThat(crear().nombre()).isEqualTo("Instagram");
    }

    @Test
    @DisplayName("Escribe título, gancho, la lista, la invitación al local y las etiquetas")
    void redactarShouldWriteAnInstagramPost() {
        String result = crear().redactar(NEGOCIO, promocionDeYerba());

        assertThat(result).isEqualTo("Tu producto estrella\nLo que más se vendió este mes\n\n"
                + "• Yerba Playadito 1 kg: $ 3.600\n\nTe esperamos en Almacén de Ana.\n\n"
                + "#ComercioDeBarrio #CompraLocal #HechoEnElBarrio");
    }
}

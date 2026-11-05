package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PromocionDePrueba.promocionDeYerba;
import static org.assertj.core.api.Assertions.assertThat;

class CanalMensajeAClientesTest extends CanalPromocionContratoTest {

    @Override
    protected CanalPromocion crear() {
        return new CanalMensajeAClientes();
    }

    @Test
    @DisplayName("Se llama clientes")
    void claveShouldBeClientes() {
        assertThat(crear().clave()).isEqualTo("clientes");
        assertThat(crear().nombre()).isEqualTo("Mensaje a clientes");
    }

    @Test
    @DisplayName("Saluda, cuenta lo que hay en el negocio, lista los productos y pregunta si se lo guarda")
    void redactarShouldWriteAPersonalMessage() {
        String result = crear().redactar(NEGOCIO, promocionDeYerba());

        assertThat(result).isEqualTo("¡Hola! Te cuento lo que hay en Almacén de Ana: tu producto estrella.\n\n"
                + "• Yerba Playadito 1 kg: $ 3.600\n\n¿Te guardo alguno? Respondeme por acá.");
    }
}

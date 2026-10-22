package com.ensambla.backend.domain.model.sistema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdSistemaTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final UUID ID = UUID.fromString("0b5e2c3a-6d8f-4f1e-9a47-3c2d1e0f9a8b");

    @Test
    @DisplayName("Un UUID se conserva tal cual")
    void aUuidShouldKeepUuid() {
        assertThat(IdSistema.aUuid(NEGOCIO_ID, ID.toString())).isEqualTo(ID);
    }

    @Test
    @DisplayName("Un UUID con el prefijo e que pone la pantalla vuelve al UUID original")
    void aUuidShouldStripFrontPrefix() {
        assertThat(IdSistema.aUuid(NEGOCIO_ID, "e" + ID)).isEqualTo(ID);
    }

    @Test
    @DisplayName("Ignora los espacios de los bordes")
    void aUuidShouldStripSpaces() {
        assertThat(IdSistema.aUuid(NEGOCIO_ID, "  " + ID + " ")).isEqualTo(ID);
    }

    @Test
    @DisplayName("Un id que no es UUID deriva siempre el mismo UUID dentro del mismo ámbito")
    void aUuidShouldDeriveDeterministicUuid() {
        UUID primero = IdSistema.aUuid(NEGOCIO_ID, "ventas");

        assertThat(IdSistema.aUuid(NEGOCIO_ID, "ventas")).isEqualTo(primero);
        assertThat(IdSistema.aUuid(NEGOCIO_ID, "compras")).isNotEqualTo(primero);
        assertThat(IdSistema.aUuid(OTRO_NEGOCIO_ID, "ventas")).isNotEqualTo(primero);
    }

    @Test
    @DisplayName("Una e seguida de algo que no es UUID también se deriva")
    void aUuidShouldDeriveWhenPrefixIsNotFollowedByUuid() {
        UUID derivado = IdSistema.aUuid(NEGOCIO_ID, "estante");

        assertThat(derivado.version()).isEqualTo(3);
    }

    @Test
    @DisplayName("La pantalla ve el UUID tal cual")
    void aFrontShouldReturnPlainUuid() {
        assertThat(IdSistema.aFront(ID)).isEqualTo(ID.toString());
    }
}

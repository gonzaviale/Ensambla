package com.ensambla.backend.domain.model.sistema;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdSistemaTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final BloqueId ID = BloqueId.de("0b5e2c3a-6d8f-4f1e-9a47-3c2d1e0f9a8b");

    @Test
    @DisplayName("Un UUID se conserva tal cual")
    void aIdShouldKeepUuid() {
        BloqueId result = bloque(NEGOCIO_ID, ID.toString());

        assertThat(result).isEqualTo(ID);
    }

    @Test
    @DisplayName("Un UUID con el prefijo e que pone la pantalla vuelve al UUID original")
    void aIdShouldStripFrontPrefix() {
        BloqueId result = bloque(NEGOCIO_ID, "e" + ID);

        assertThat(result).isEqualTo(ID);
    }

    @Test
    @DisplayName("Ignora los espacios de los bordes")
    void aIdShouldStripSpaces() {
        BloqueId result = bloque(NEGOCIO_ID, "  " + ID + " ");

        assertThat(result).isEqualTo(ID);
    }

    @Test
    @DisplayName("Un id que no es UUID deriva siempre el mismo UUID dentro del mismo ámbito")
    void aIdShouldDeriveDeterministicUuid() {
        BloqueId primero = bloque(NEGOCIO_ID, "ventas");

        assertThat(bloque(NEGOCIO_ID, "ventas")).isEqualTo(primero);
        assertThat(bloque(NEGOCIO_ID, "compras")).isNotEqualTo(primero);
        assertThat(bloque(OTRO_NEGOCIO_ID, "ventas")).isNotEqualTo(primero);
    }

    @Test
    @DisplayName("Una e seguida de algo que no es UUID también se deriva")
    void aIdShouldDeriveWhenPrefixIsNotFollowedByUuid() {
        BloqueId derivado = bloque(NEGOCIO_ID, "estante");

        assertThat(derivado.valor().version()).isEqualTo(3);
    }

    @Test
    @DisplayName("La pantalla ve el UUID tal cual")
    void aFrontShouldReturnPlainUuid() {
        assertThat(IdSistema.aFront(ID)).isEqualTo(ID.valor().toString());
    }

    // --- act ---
    private static BloqueId bloque(NegocioId ambito, String idDelFront) {
        return IdSistema.aId(ambito, idDelFront, BloqueId::de);
    }
}

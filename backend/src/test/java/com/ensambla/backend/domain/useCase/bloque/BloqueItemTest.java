package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueItemTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Pasa todos los datos del bloque a la respuesta")
    void bloqueItemShouldMapEveryField() {
        BloqueItem result = BloqueItem.de(givenBloque("Productos"));

        assertThat(result.id()).isEqualTo(BLOQUE_ID);
        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.nombre()).isEqualTo("Productos");
        assertThat(result.posicion()).isEqualTo((short) 1);
        assertThat(result.creadoEn()).isEqualTo(CREADO);
        assertThat(result.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    // --- helpers ---
    private Bloque givenBloque(String nombre) {
        return Bloque.builder()
                .id(BLOQUE_ID)
                .proyectoId(PROYECTO_ID)
                .nombre(nombre)
                .posicion((short) 1)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }
}

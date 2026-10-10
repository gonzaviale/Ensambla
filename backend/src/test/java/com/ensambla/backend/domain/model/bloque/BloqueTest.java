package com.ensambla.backend.domain.model.bloque;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Crea un bloque nuevo con el nombre normalizado, sin campos y con las dos fechas iguales")
    void nuevoShouldNormalizeNameAndSetDates() {
        Bloque result = Bloque.nuevo(NEGOCIO_ID, PROYECTO_ID, "  Clientes  ", (short) 3, AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getNombre()).isEqualTo("Clientes");
        assertThat(result.getPosicion()).isEqualTo((short) 3);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.getCampos()).isEmpty();
    }

    @Test
    @DisplayName("Renombra conservando la fecha de creación y actualizando la de modificación")
    void renombrarShouldPreserveCreationDate() {
        Bloque bloque = Bloque.nuevo(NEGOCIO_ID, PROYECTO_ID, "Clientes", (short) 0, AHORA);

        bloque.renombrar("  Compradores ", AHORA.plusHours(1));

        assertThat(bloque.getNombre()).isEqualTo("Compradores");
        assertThat(bloque.getCreadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }
}

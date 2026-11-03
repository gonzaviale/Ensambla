package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExportarResumenResponseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    @Test
    @DisplayName("El nombre es resumen-<sistema>-<fecha>.pdf, sin tildes ni espacios")
    void deShouldBuildAFileNameFromTheSystemName() {
        ExportarResumenResponse response = ExportarResumenResponse.de(new byte[] {1}, "Almacén de Doña Rosa!", HOY);

        assertThat(response.nombreArchivo()).isEqualTo("resumen-almacen-de-dona-rosa-20261019.pdf");
        assertThat(response.contenido()).containsExactly(1);
    }

    @Test
    @DisplayName("Si el nombre no deja letras ni números, el archivo se llama resumen")
    void deShouldFallBackToResumenWhenTheNameHasNoLetters() {
        assertThat(ExportarResumenResponse.de(new byte[0], "¡¿?!", HOY).nombreArchivo())
                .isEqualTo("resumen-20261019.pdf");
    }
}

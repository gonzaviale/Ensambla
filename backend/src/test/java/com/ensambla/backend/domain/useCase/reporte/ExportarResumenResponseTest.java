package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExportarResumenResponseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    private ExportarResumenResponse respuesta;

    @Test
    @DisplayName("El nombre es resumen-<sistema>-<fecha>.pdf, sin tildes ni espacios")
    void elNombreEsResumenSistemaFechaPdfSinTildesNiEspacios() {
        cuandoSeArmaLaRespuestaDe("Almacén de Doña Rosa!", new byte[] {1});

        entoncesElNombreEsElDelSistemaSinTildesNiEspaciosYConservaElContenido();
    }

    @Test
    @DisplayName("Si el nombre no deja letras ni números, el archivo se llama resumen")
    void siElNombreNoDejaLetrasNiNumerosElArchivoSeLlamaResumen() {
        cuandoSeArmaLaRespuestaDe("¡¿?!", new byte[0]);

        entoncesElArchivoSeLlamaSoloResumen();
    }

    // --- cuando ---
    private void cuandoSeArmaLaRespuestaDe(String sistema, byte[] contenido) {
        respuesta = ExportarResumenResponse.de(contenido, sistema, HOY);
    }

    // --- entonces ---
    private void entoncesElNombreEsElDelSistemaSinTildesNiEspaciosYConservaElContenido() {
        assertThat(respuesta.nombreArchivo()).isEqualTo("resumen-almacen-de-dona-rosa-20261019.pdf");
        assertThat(respuesta.contenido()).containsExactly(1);
    }

    private void entoncesElArchivoSeLlamaSoloResumen() {
        assertThat(respuesta.nombreArchivo()).isEqualTo("resumen-20261019.pdf");
    }
}

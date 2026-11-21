package com.ensambla.backend.domain.model.precio;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CambioPrecioTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-20T01:00:00Z");

    private CambioPrecio cambio;

    @Test
    @DisplayName("Registra el cambio entre dos precios distintos")
    void registraElCambioEntreDosPreciosDistintos() {
        cuandoCambiaElPrecio("1000", "1200");

        entoncesSeRegistraElCambioDeMilAMilDoscientos();
    }

    @Test
    @DisplayName("El primer precio queda sin precio anterior")
    void elPrimerPrecioQuedaSinPrecioAnterior() {
        cuandoCambiaElPrecio(null, "1200");

        entoncesNoHayPrecioAnterior();
    }

    @Test
    @DisplayName("Si el anterior no era un precio también queda sin precio anterior")
    void siElAnteriorNoEraUnPrecioTambienQuedaSinPrecioAnterior() {
        cuandoCambiaElPrecio("0", "1200");

        entoncesNoHayPrecioAnterior();
    }

    @Test
    @DisplayName("No hay cambio si el precio es el mismo, aunque cambie la escala")
    void noHayCambioSiElPrecioEsElMismoAunqueCambieLaEscala() {
        cuandoCambiaElPrecio("1000", "1000.00");

        entoncesNoHayCambio();
    }

    @Test
    @DisplayName("No hay cambio si el precio nuevo falta")
    void noHayCambioSiElPrecioNuevoFalta() {
        cuandoCambiaElPrecio("1000", null);

        entoncesNoHayCambio();
    }

    @Test
    @DisplayName("No hay cambio si el precio nuevo no es positivo")
    void noHayCambioSiElPrecioNuevoNoEsPositivo() {
        cuandoCambiaElPrecio("1000", "0");

        entoncesNoHayCambio();
    }

    @Test
    @DisplayName("Es un aumento si el precio subió respecto del anterior")
    void esUnAumentoSiElPrecioSubioRespectoDelAnterior() {
        cuandoCambiaElPrecio("1000", "1200");

        entoncesEsUnAumento();
    }

    @Test
    @DisplayName("No es un aumento si el precio bajó")
    void noEsUnAumentoSiElPrecioBajo() {
        cuandoCambiaElPrecio("1200", "1000");

        entoncesNoEsUnAumento();
    }

    @Test
    @DisplayName("El primer precio que se carga no es un aumento")
    void elPrimerPrecioQueSeCargaNoEsUnAumento() {
        cuandoCambiaElPrecio(null, "1200");

        entoncesNoEsUnAumento();
    }

    @Test
    @DisplayName("La fecha es el día argentino del cambio, aunque en UTC ya sea el día siguiente")
    void laFechaEsElDiaArgentinoDelCambioAunqueEnUtcYaSeaElDiaSiguiente() {
        cuandoCambiaElPrecio("1000", "1200");

        entoncesLaFechaEsElDiaArgentino();
    }

    // --- cuando ---
    private void cuandoCambiaElPrecio(String anterior, String nuevo) {
        cambio = CambioPrecio.entre(NEGOCIO_ID, REGISTRO_ID, precio(anterior), precio(nuevo), AHORA);
    }

    private static BigDecimal precio(String valor) {
        if (valor == null) {
            return null;
        }
        return new BigDecimal(valor);
    }

    // --- entonces ---
    private void entoncesSeRegistraElCambioDeMilAMilDoscientos() {
        assertThat(cambio).isEqualTo(new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, new BigDecimal("1000"),
                new BigDecimal("1200"), AHORA));
    }

    private void entoncesNoHayPrecioAnterior() {
        assertThat(cambio.anterior()).isNull();
    }

    private void entoncesNoHayCambio() {
        assertThat(cambio).isNull();
    }

    private void entoncesEsUnAumento() {
        assertThat(cambio.esAumento()).isTrue();
    }

    private void entoncesNoEsUnAumento() {
        assertThat(cambio.esAumento()).isFalse();
    }

    private void entoncesLaFechaEsElDiaArgentino() {
        assertThat(cambio.fecha()).isEqualTo(LocalDate.of(2026, 10, 19));
    }
}

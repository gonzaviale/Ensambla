package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeWhatsappColaTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");

    @Test
    @DisplayName("Reclamar un mensaje lo deja atendiéndose, cuenta el intento y fija hasta cuándo es de quien lo tomó")
    void reclamarShouldLockTheMessage() {
        MensajeWhatsapp mensaje = givenMensaje();

        mensaje.reclamar(AHORA, Duration.ofMinutes(2));

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIENDO);
        assertThat(mensaje.getIntentos()).isEqualTo(1);
        assertThat(mensaje.getReclamadoHasta()).isEqualTo(AHORA.plusMinutes(2));
    }

    @Test
    @DisplayName("Atender lo cierra con la hora y limpia un error anterior")
    void atenderShouldCloseTheMessage() {
        MensajeWhatsapp mensaje = givenMensaje();
        mensaje.fallar("antes falló", AHORA);

        mensaje.atender(AHORA.plusMinutes(1));

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
        assertThat(mensaje.getAtendidoEn()).isEqualTo(AHORA.plusMinutes(1));
        assertThat(mensaje.getError()).isNull();
    }

    @Test
    @DisplayName("Fallar lo deja fallido con el motivo y la hora")
    void fallarShouldRecordTheReason() {
        MensajeWhatsapp mensaje = givenMensaje();

        mensaje.fallar("se cayó el proveedor", AHORA);

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        assertThat(mensaje.getError()).isEqualTo("se cayó el proveedor");
        assertThat(mensaje.getAtendidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("La interpretación sólo está entendida con tabla, filas y confianza suficiente")
    void interpretacionShouldBeUnderstoodOnlyWithEnoughConfidence() {
        BloqueId bloqueId = BloqueId.de("b0000000-0000-4000-8000-000000000001");
        List<Map<CampoId, String>> filas = List.of(Map.of(CampoId.nuevo(), "x"));

        assertThat(new InterpretacionRegistros(bloqueId, filas, 0.6, null).entendida(0.6)).isTrue();
        assertThat(new InterpretacionRegistros(bloqueId, filas, 0.59, null).entendida(0.6)).isFalse();
        assertThat(new InterpretacionRegistros(null, filas, 0.9, null).entendida(0.6)).isFalse();
        assertThat(new InterpretacionRegistros(bloqueId, List.of(), 0.9, null).entendida(0.6)).isFalse();
        assertThat(InterpretacionRegistros.sinResultado(null).entendida(0.0)).isFalse();
    }

    private static MensajeWhatsapp givenMensaje() {
        return MensajeWhatsapp.recibido(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
                new MensajeEntrante("5491122334455", "ID1", TipoMensajeWhatsapp.TEXTO, "hola", null, null, AHORA));
    }
}

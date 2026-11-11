package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoModelTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final String TELEFONO = "5491199990000";

    @Test
    @DisplayName("Un cliente nuevo guarda su teléfono y la hora, y todavía no tiene último comercio")
    void clienteShouldStartWithoutLastBusiness() {
        ClienteWhatsapp cliente = ClienteWhatsapp.nuevo(TELEFONO, AHORA);

        assertThat(cliente.getTelefono()).isEqualTo(TELEFONO);
        assertThat(cliente.getCreadoEn()).isEqualTo(AHORA);
        assertThat(cliente.getId()).isNull();
        assertThat(cliente.getUltimoNegocioId()).isNull();
    }

    @Test
    @DisplayName("Un mensaje recibido de un cliente queda en la cola con lo que escribió")
    void mensajeShouldStartReceived() {
        MensajeCliente mensaje = givenMensaje();

        assertThat(mensaje.getTelefono()).isEqualTo(TELEFONO);
        assertThat(mensaje.getIdExterno()).isEqualTo("ID1");
        assertThat(mensaje.getTexto()).isEqualTo("hola");
        assertThat(mensaje.getRecibidoEn()).isEqualTo(AHORA);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.RECIBIDO);
    }

    @Test
    @DisplayName("Reclamar un mensaje lo deja atendiéndose, cuenta el intento y fija hasta cuándo es de quien lo tomó")
    void reclamarShouldLockTheMessage() {
        MensajeCliente mensaje = givenMensaje();

        mensaje.reclamar(AHORA, Duration.ofMinutes(2));

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIENDO);
        assertThat(mensaje.getIntentos()).isEqualTo(1);
        assertThat(mensaje.getReclamadoHasta()).isEqualTo(AHORA.plusMinutes(2));
    }

    @Test
    @DisplayName("Atender lo cierra con la hora y limpia un error anterior")
    void atenderShouldCloseTheMessage() {
        MensajeCliente mensaje = givenMensaje();
        mensaje.fallar("antes falló", AHORA);

        mensaje.atender(AHORA.plusMinutes(1));

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
        assertThat(mensaje.getAtendidoEn()).isEqualTo(AHORA.plusMinutes(1));
        assertThat(mensaje.getError()).isNull();
    }

    @Test
    @DisplayName("Fallar lo deja fallido con el motivo y la hora")
    void fallarShouldRecordTheReason() {
        MensajeCliente mensaje = givenMensaje();

        mensaje.fallar("se cayó el proveedor", AHORA);

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        assertThat(mensaje.getError()).isEqualTo("se cayó el proveedor");
        assertThat(mensaje.getAtendidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("La lista de clientes normaliza los números, ignora los vacíos y sólo permite a los que están")
    void politicaShouldNormalizeAndAllowOnlyListedNumbers() {
        PoliticaPedidosWhatsapp politica = PoliticaPedidosWhatsapp.de(List.of("+54 9 11 9999-0000", "541188887777", " "));

        assertThat(politica.clientesPermitidos()).containsExactlyInAnyOrder("5491199990000", "5491188887777");
        assertThat(politica.permite("5491199990000")).isTrue();
        assertThat(politica.permite("5491100001111")).isFalse();
    }

    @Test
    @DisplayName("La lista de clientes puede estar vacía y entonces no permite a nadie")
    void politicaShouldAllowNobodyWhenEmpty() {
        assertThat(PoliticaPedidosWhatsapp.de(List.of()).permite(TELEFONO)).isFalse();
        assertThat(new PoliticaPedidosWhatsapp(Set.of()).clientesPermitidos()).isEmpty();
    }

    @Test
    @DisplayName("Un número configurado que no es un teléfono rompe al arrancar en lugar de quedar ignorado")
    void politicaShouldRejectInvalidNumber() {
        assertThatThrownBy(() -> PoliticaPedidosWhatsapp.de(List.of("abc")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static MensajeCliente givenMensaje() {
        return MensajeCliente.recibido(new MensajeEntrante(TELEFONO, "ID1", TipoMensajeWhatsapp.TEXTO, "hola", null,
                null, AHORA));
    }
}

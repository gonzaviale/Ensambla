package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropuestaWhatsappTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId GASTOS = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final FilaPropuesta FILA = new FilaPropuesta(Map.of("c1", "5000"), Set.of());

    @Test
    @DisplayName("Pendiente nace abierta, con su tabla y filas, y vence a los 30 minutos")
    void pendienteShouldBeOpenForThirtyMinutes() {
        PropuestaWhatsapp propuesta = givenPendiente();

        assertThat(propuesta.estaAbierta()).isTrue();
        assertThat(propuesta.estaVigente(AHORA.plusMinutes(29))).isTrue();
        assertThat(propuesta.estaVigente(AHORA.plusMinutes(30))).isFalse();
        assertThat(propuesta.getBloqueId()).isEqualTo(GASTOS);
    }

    @Test
    @DisplayName("Una propuesta ya cerrada no está vigente aunque no haya pasado el tiempo")
    void estaVigenteShouldBeFalseWhenClosed() {
        PropuestaWhatsapp propuesta = givenPendiente();
        propuesta.descartar(AHORA);

        assertThat(propuesta.estaVigente(AHORA.plusMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("Elegir devuelve la tabla del número de la lista, que empieza en 1")
    void elegirShouldReturnTheNumberedTable() {
        PropuestaWhatsapp propuesta = givenEligiendo();

        assertThat(propuesta.elegir(1)).isEqualTo(GASTOS);
        assertThat(propuesta.elegir(2)).isEqualTo(VENTAS);
    }

    @Test
    @DisplayName("Elegir un número fuera de la lista, o sin lista, pide elegir de la lista")
    void elegirShouldRejectOutOfRange() {
        PropuestaWhatsapp propuesta = givenEligiendo();

        assertThatThrownBy(() -> propuesta.elegir(0)).hasMessage("Elegí un número de la lista");
        assertThatThrownBy(() -> propuesta.elegir(3)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> givenPendiente().elegir(1)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Pasar a pendiente guarda la tabla elegida y las filas")
    void aPendienteShouldKeepTableAndRows() {
        PropuestaWhatsapp propuesta = givenEligiendo();

        propuesta.aPendiente(VENTAS, List.of(FILA));

        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(propuesta.getBloqueId()).isEqualTo(VENTAS);
        assertThat(propuesta.getFilas()).containsExactly(FILA);
    }

    @Test
    @DisplayName("Confirmar, descartar y vencer cierran la propuesta con la hora")
    void closingShouldRecordStateAndTime() {
        PropuestaWhatsapp confirmada = givenPendiente();
        confirmada.confirmar(AHORA);
        PropuestaWhatsapp descartada = givenPendiente();
        descartada.descartar(AHORA);
        PropuestaWhatsapp vencida = givenPendiente();
        vencida.vencer(AHORA);

        assertThat(confirmada.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.CONFIRMADA);
        assertThat(descartada.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
        assertThat(confirmada.getCerradoEn()).isEqualTo(AHORA);
        assertThat(confirmada.estaAbierta()).isFalse();
    }

    @Test
    @DisplayName("No se puede confirmar una carga que todavía espera que se elija la tabla")
    void confirmarShouldRequireATable() {
        assertThatThrownBy(() -> givenEligiendo().confirmar(AHORA))
                .hasMessage("Primero elegí la tabla respondiendo con su número");
    }

    private static PropuestaWhatsapp givenPendiente() {
        return PropuestaWhatsapp.pendiente(NEGOCIO_ID, "5491122334455", OrigenPropuestaWhatsapp.TEXTO,
                "gasté 5000", GASTOS, List.of(FILA), AHORA);
    }

    private static PropuestaWhatsapp givenEligiendo() {
        return PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, "5491122334455", OrigenPropuestaWhatsapp.TEXTO,
                "gasté 5000", null, List.of(GASTOS, VENTAS), AHORA);
    }
}

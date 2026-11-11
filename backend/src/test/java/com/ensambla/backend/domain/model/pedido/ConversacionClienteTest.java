package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

class ConversacionClienteTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final ClienteId CLIENTE_ID = ClienteId.de("c0000000-0000-4000-8000-000000000001");
    private static final NegocioId PEPE = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId ROSA = NegocioId.de("a0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Una conversación nueva empieza eligiendo comercio, sin oferta ni opciones")
    void nuevaShouldStartChoosingTheBusiness() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);

        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        assertThat(conversacion.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(conversacion.ofreceElUltimo()).isFalse();
        assertThat(conversacion.ofreceOpciones()).isFalse();
    }

    @Test
    @DisplayName("Está vigente hasta que pasa el tiempo de espera desde la última vez que se habló")
    void estaVigenteShouldExpireAfterTheTimeout() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);

        assertThat(conversacion.estaVigente(AHORA.plus(ConversacionCliente.VIGENCIA).minusSeconds(1))).isTrue();
        assertThat(conversacion.estaVigente(AHORA.plus(ConversacionCliente.VIGENCIA))).isFalse();
    }

    @Test
    @DisplayName("Reiniciar vuelve a elegir comercio, ofrece el último y descarta las opciones")
    void reiniciarShouldOfferTheLastBusiness() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.ofrecerOpciones(List.of(PEPE, ROSA), AHORA);

        conversacion.reiniciar(ROSA, AHORA.plusMinutes(1));

        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        assertThat(conversacion.getNegocioId()).isEqualTo(ROSA);
        assertThat(conversacion.ofreceElUltimo()).isTrue();
        assertThat(conversacion.ofreceOpciones()).isFalse();
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(1));
    }

    @Test
    @DisplayName("Reiniciar sin comercio que ofrecer no ofrece nada")
    void reiniciarShouldOfferNothingWithoutBusiness() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);

        conversacion.reiniciar(null, AHORA);

        assertThat(conversacion.ofreceElUltimo()).isFalse();
    }

    @Test
    @DisplayName("Ofrecer opciones las numera desde 1, deja de ofrecer el último y no ofrece el último si ya está pidiendo")
    void ofrecerOpcionesShouldNumberTheBusinesses() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.reiniciar(PEPE, AHORA);

        conversacion.ofrecerOpciones(List.of(PEPE, ROSA), AHORA);

        assertThat(conversacion.ofreceOpciones()).isTrue();
        assertThat(conversacion.ofreceElUltimo()).isFalse();
        assertThat(conversacion.opcion(1)).contains(PEPE);
        assertThat(conversacion.opcion(2)).contains(ROSA);
        assertThat(conversacion.opcion(0)).isEmpty();
        assertThat(conversacion.opcion(3)).isEmpty();
    }

    @Test
    @DisplayName("Elegir un comercio pasa a pedirle, descarta las opciones y ya no se ofrece como último")
    void elegirShouldStartOrdering() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.ofrecerOpciones(List.of(PEPE, ROSA), AHORA);

        conversacion.elegir(ROSA, AHORA.plusMinutes(2));

        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.PIDIENDO);
        assertThat(conversacion.getNegocioId()).isEqualTo(ROSA);
        assertThat(conversacion.ofreceOpciones()).isFalse();
        assertThat(conversacion.ofreceElUltimo()).isFalse();
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(2));
    }

    @Test
    @DisplayName("Tocar la conversación sólo renueva la hora")
    void tocarShouldOnlyRefreshTheTime() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.elegir(PEPE, AHORA);

        conversacion.tocar(AHORA.plusMinutes(10));

        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.PIDIENDO);
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(10));
    }

    @Test
    @DisplayName("Armar la lista reemplaza a la anterior y elegir, reiniciar u ofrecer opciones la descartan")
    void armarListaShouldReplaceAndEveryRestartShouldDiscardIt() {
        List<LineaDeCompra> primera = List.of(LineaDeCompra.de(producto("Yerba", "3500", "10"), null, List.of()));
        List<LineaDeCompra> segunda = List.of(LineaDeCompra.de(producto("Aceite", "2000", "5"), null, List.of()));
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.elegir(PEPE, AHORA);

        conversacion.armarLista(primera, AHORA.plusMinutes(1));
        conversacion.armarLista(segunda, AHORA.plusMinutes(2));

        assertThat(conversacion.getLista()).isEqualTo(segunda);
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.PIDIENDO);
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(2));
        conversacion.elegir(ROSA, AHORA);
        assertThat(conversacion.getLista()).isEmpty();
        conversacion.armarLista(primera, AHORA);
        conversacion.reiniciar(null, AHORA);
        assertThat(conversacion.getLista()).isEmpty();
        conversacion.armarLista(primera, AHORA);
        conversacion.ofrecerOpciones(List.of(PEPE, ROSA), AHORA);
        assertThat(conversacion.getLista()).isEmpty();
    }

    @Test
    @DisplayName("Cambiar de marca reemplaza sólo ese renglón por la marca elegida y deja de esperar la elección")
    void cambiarMarcaShouldReplaceOnlyThatLine() {
        ProductoEnStock natura = producto("Aceite Natura 900 ml", "2400", "8");
        LineaDeCompra yerba = LineaDeCompra.de(producto("Yerba", "3500", "10"), null, List.of());
        LineaDeCompra aceite = LineaDeCompra.de(producto("Aceite Cocinero", "2000", "5"), null, List.of(natura));
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.elegir(PEPE, AHORA);
        conversacion.armarLista(List.of(yerba, aceite), AHORA);
        assertThat(conversacion.tieneLista()).isTrue();
        assertThat(conversacion.esperaCambioDeMarca()).isFalse();

        conversacion.ofrecerCambioDeMarca(2, AHORA.plusMinutes(1));

        assertThat(conversacion.esperaCambioDeMarca()).isTrue();
        assertThat(conversacion.renglonEnCambio()).isEqualTo(aceite);
        conversacion.cambiarMarca(natura, AHORA.plusMinutes(2));
        assertThat(conversacion.getLista()).hasSize(2);
        assertThat(conversacion.getLista().get(0)).isEqualTo(yerba);
        assertThat(conversacion.getLista().get(1).producto()).isEqualTo(natura);
        assertThat(conversacion.esperaCambioDeMarca()).isFalse();
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(2));
    }

    @Test
    @DisplayName("Dejar la marca como está conserva la lista y deja de esperar la elección")
    void dejarMarcaShouldKeepTheList() {
        List<LineaDeCompra> lista = List.of(LineaDeCompra.de(producto("Yerba", "3500", "10"), null, List.of()));
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.armarLista(lista, AHORA);
        conversacion.ofrecerCambioDeMarca(1, AHORA);

        conversacion.dejarMarca(AHORA.plusMinutes(3));

        assertThat(conversacion.esperaCambioDeMarca()).isFalse();
        assertThat(conversacion.getLista()).isEqualTo(lista);
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(3));
    }

    @Test
    @DisplayName("Vaciar la lista (descartar o confirmar) no deja nada armado ni esperando una marca")
    void vaciarListaShouldLeaveNothingPending() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.armarLista(List.of(LineaDeCompra.de(producto("Yerba", "3500", "10"), null, List.of())), AHORA);
        conversacion.ofrecerCambioDeMarca(1, AHORA);

        conversacion.vaciarLista(AHORA.plusMinutes(1));

        assertThat(conversacion.tieneLista()).isFalse();
        assertThat(conversacion.esperaCambioDeMarca()).isFalse();
    }

    @Test
    @DisplayName("Reiniciar, ofrecer opciones o elegir comercio también dejan de esperar una marca")
    void restartsShouldStopWaitingForABrand() {
        ConversacionCliente conversacion = ConversacionCliente.nueva(CLIENTE_ID, AHORA);
        conversacion.ofrecerCambioDeMarca(1, AHORA);
        conversacion.reiniciar(null, AHORA);
        assertThat(conversacion.esperaCambioDeMarca()).isFalse();
        conversacion.ofrecerCambioDeMarca(1, AHORA);
        conversacion.ofrecerOpciones(List.of(PEPE, ROSA), AHORA);
        assertThat(conversacion.esperaCambioDeMarca()).isFalse();
        conversacion.ofrecerCambioDeMarca(1, AHORA);
        conversacion.elegir(PEPE, AHORA);
        assertThat(conversacion.esperaCambioDeMarca()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cambiar", "Cambiar", " CAMBIAR ", "cambiar comercio", "cambiar de comercio",
            "Otro comercio", "otro"})
    @DisplayName("Reconoce las formas de pedir otro comercio")
    void esCambioDeComercioShouldRecognizeTheCommands(String texto) {
        assertThat(ConversacionCliente.esCambioDeComercio(texto)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dame yerba", "cambiar la yerba por otra", "", "sí"})
    @DisplayName("No toma como cambio de comercio un pedido que menciona la palabra")
    void esCambioDeComercioShouldIgnoreOtherTexts(String texto) {
        assertThat(ConversacionCliente.esCambioDeComercio(texto)).isFalse();
    }
}

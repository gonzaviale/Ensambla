package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ConversacionObjetivoTest {

    private static final int MAXIMO = 6;
    private static final int LARGO_MAXIMO_DE_MENSAJE = 500;

    private List<MensajeObjetivo> mensajes;
    private ConversacionObjetivo conversacion;
    private Throwable error;

    @Test
    @DisplayName("Una conversación válida deja los mensajes sin espacios y cuenta los que quedan")
    void deShouldStripTheMessagesAndCountTheRemainingOnes() {
        givenMensajes(persona("  Quiero vender más \n"), sami("¿Qué producto?"), persona("La yerba"));

        whenSeArmaLaConversacion();

        thenLosTextosSon("Quiero vender más", "¿Qué producto?", "La yerba");
        thenQuedanMensajes(4);
    }

    @Test
    @DisplayName("El objetivo de la persona es lo primero que escribió")
    void objetivoDeLaPersonaShouldBeTheFirstMessageOfThePerson() {
        givenMensajes(sami("Hola"), persona("Quiero vender más"), persona("La yerba"));

        whenSeArmaLaConversacion();

        thenElObjetivoDeLaPersonaEs("Quiero vender más");
    }

    @Test
    @DisplayName("A la IA le llegan los mensajes sin correos ni teléfonos")
    void paraLaIaShouldHidePersonalData() {
        givenMensajes(persona("Cobrale a ana@mail.com, tel 11 5555 5555"));

        whenSeArmaLaConversacion();

        thenALaIaLeLlegaSinDatosPersonales("Cobrale a [dato oculto], tel [dato oculto]");
        thenElMensajeOriginalConservaElCorreo("ana@mail.com");
    }

    @Test
    @DisplayName("Sin mensajes no hay conversación")
    void deShouldRejectNoMessages() {
        givenMensajes();

        whenSeArmaLaConversacionConError();

        thenSeRechazaConMensaje("Contale a Sami qué querés lograr");
    }

    @Test
    @DisplayName("Con mensajes nulos no hay conversación")
    void deShouldRejectNullMessages() {
        mensajes = null;

        whenSeArmaLaConversacionConError();

        thenSeRechaza();
    }

    @Test
    @DisplayName("Un mensaje vacío o en blanco se rechaza")
    void deShouldRejectBlankMessages() {
        givenMensajes(persona("   "));

        whenSeArmaLaConversacionConError();

        thenSeRechazaConMensaje("Los mensajes no pueden estar vacíos");
    }

    @Test
    @DisplayName("Un mensaje nulo se rechaza")
    void deShouldRejectNullTextMessages() {
        givenMensajes(persona(null));

        whenSeArmaLaConversacionConError();

        thenSeRechaza();
    }

    @Test
    @DisplayName("Un mensaje de 500 caracteres se acepta")
    void deShouldAcceptAMessageOfTheMaximumLength() {
        givenMensajes(persona("a".repeat(LARGO_MAXIMO_DE_MENSAJE)));

        whenSeArmaLaConversacion();

        thenHayMensajes(1);
    }

    @Test
    @DisplayName("Un mensaje de 501 caracteres se rechaza")
    void deShouldRejectAMessageOverTheMaximumLength() {
        givenMensajes(persona("a".repeat(LARGO_MAXIMO_DE_MENSAJE + 1)));

        whenSeArmaLaConversacionConError();

        thenSeRechazaConMensaje("Cada mensaje puede tener hasta 500 caracteres");
    }

    @Test
    @DisplayName("El último mensaje tiene que ser de la persona")
    void deShouldRejectAConversationEndingWithSami() {
        givenMensajes(persona("Hola"), sami("¿Qué querés?"));

        whenSeArmaLaConversacionConError();

        thenSeRechazaConMensaje("El último mensaje tiene que ser tuyo");
    }

    @Test
    @DisplayName("Seis mensajes de la persona se aceptan y no quedan más")
    void deShouldAcceptTheMaximumMessagesOfThePerson() {
        givenMensajesDeLaPersona(6);

        whenSeArmaLaConversacion();

        thenQuedanMensajes(0);
    }

    @Test
    @DisplayName("Siete mensajes de la persona se rechazan")
    void deShouldRejectMoreMessagesThanTheMaximum() {
        givenMensajesDeLaPersona(7);

        whenSeArmaLaConversacionConError();

        thenSeRechazaConMensaje("Llegaste al máximo de 6 mensajes de esta conversación. "
                + "Guardá lo que armamos o empezá una nueva");
    }

    @Test
    @DisplayName("Los mensajes de Sami no cuentan para el tope")
    void deShouldNotCountTheMessagesOfSami() {
        givenCincoMensajesDeLaPersonaIntercaladosConDosDeSami();

        whenSeArmaLaConversacion();

        thenQuedanMensajes(1);
    }

    // --- arrange ---
    private void givenMensajes(MensajeObjetivo... mensajesDados) {
        mensajes = List.of(mensajesDados);
    }

    private void givenMensajesDeLaPersona(int cantidad) {
        mensajes = mensajesDeLaPersona(cantidad);
    }

    private void givenCincoMensajesDeLaPersonaIntercaladosConDosDeSami() {
        List<MensajeObjetivo> intercalados = new ArrayList<>(mensajesDeLaPersona(5));
        intercalados.add(1, sami("Una pregunta"));
        intercalados.add(3, sami("Otra pregunta"));
        mensajes = intercalados;
    }

    // --- act ---
    private void whenSeArmaLaConversacion() {
        conversacion = ConversacionObjetivo.de(mensajes, MAXIMO);
    }

    private void whenSeArmaLaConversacionConError() {
        error = catchThrowable(this::whenSeArmaLaConversacion);
    }

    // --- assert ---
    private void thenLosTextosSon(String... textos) {
        assertThat(conversacion.mensajes()).extracting(MensajeObjetivo::texto).containsExactly(textos);
    }

    private void thenQuedanMensajes(int cantidad) {
        assertThat(conversacion.mensajesRestantes()).isEqualTo(cantidad);
    }

    private void thenHayMensajes(int cantidad) {
        assertThat(conversacion.mensajes()).hasSize(cantidad);
    }

    private void thenElObjetivoDeLaPersonaEs(String esperado) {
        assertThat(conversacion.objetivoDeLaPersona()).isEqualTo(esperado);
    }

    private void thenALaIaLeLlegaSinDatosPersonales(String texto) {
        assertThat(conversacion.paraLaIa()).extracting(MensajeObjetivo::texto).containsExactly(texto);
    }

    private void thenElMensajeOriginalConservaElCorreo(String correo) {
        assertThat(conversacion.mensajes().getFirst().texto()).contains(correo);
    }

    private void thenSeRechaza() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void thenSeRechazaConMensaje(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    // --- helpers ---
    private static MensajeObjetivo persona(String texto) {
        return new MensajeObjetivo(RolMensaje.PERSONA, texto);
    }

    private static MensajeObjetivo sami(String texto) {
        return new MensajeObjetivo(RolMensaje.SAMI, texto);
    }

    private static List<MensajeObjetivo> mensajesDeLaPersona(int cantidad) {
        return IntStream.rangeClosed(1, cantidad)
                .mapToObj(numero -> persona("Mensaje " + numero))
                .toList();
    }
}

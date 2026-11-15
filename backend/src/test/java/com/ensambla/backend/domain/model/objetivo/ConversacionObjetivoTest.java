package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversacionObjetivoTest {

    private static final int MAXIMO = 6;

    @Test
    @DisplayName("Una conversación válida deja los mensajes sin espacios y cuenta los que quedan")
    void deShouldStripTheMessagesAndCountTheRemainingOnes() {
        ConversacionObjetivo result = ConversacionObjetivo.de(List.of(
                persona("  Quiero vender más \n"),
                sami("¿Qué producto?"),
                persona("La yerba")), MAXIMO);

        assertThat(result.mensajes()).extracting(MensajeObjetivo::texto)
                .containsExactly("Quiero vender más", "¿Qué producto?", "La yerba");
        assertThat(result.mensajesRestantes()).isEqualTo(4);
    }

    @Test
    @DisplayName("El objetivo de la persona es lo primero que escribió")
    void objetivoDeLaPersonaShouldBeTheFirstMessageOfThePerson() {
        ConversacionObjetivo result = ConversacionObjetivo.de(
                List.of(sami("Hola"), persona("Quiero vender más"), persona("La yerba")), MAXIMO);

        assertThat(result.objetivoDeLaPersona()).isEqualTo("Quiero vender más");
    }

    @Test
    @DisplayName("A la IA le llegan los mensajes sin correos ni teléfonos")
    void paraLaIaShouldHidePersonalData() {
        ConversacionObjetivo result = ConversacionObjetivo.de(
                List.of(persona("Cobrale a ana@mail.com, tel 11 5555 5555")), MAXIMO);

        assertThat(result.paraLaIa()).extracting(MensajeObjetivo::texto)
                .containsExactly("Cobrale a [dato oculto], tel [dato oculto]");
        assertThat(result.mensajes().getFirst().texto()).contains("ana@mail.com");
    }

    @Test
    @DisplayName("Sin mensajes no hay conversación")
    void deShouldRejectNoMessages() {
        assertThatThrownBy(() -> ConversacionObjetivo.de(List.of(), MAXIMO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Contale a Sami qué querés lograr");
        assertThatThrownBy(() -> ConversacionObjetivo.de(null, MAXIMO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un mensaje vacío, en blanco o nulo se rechaza")
    void deShouldRejectEmptyMessages() {
        assertThatThrownBy(() -> ConversacionObjetivo.de(List.of(persona("   ")), MAXIMO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Los mensajes no pueden estar vacíos");
        assertThatThrownBy(() -> ConversacionObjetivo.de(List.of(persona(null)), MAXIMO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un mensaje de 500 caracteres se acepta y de 501 se rechaza")
    void deShouldLimitTheLengthOfEachMessage() {
        assertThat(ConversacionObjetivo.de(List.of(persona("a".repeat(500))), MAXIMO).mensajes()).hasSize(1);

        assertThatThrownBy(() -> ConversacionObjetivo.de(List.of(persona("a".repeat(501))), MAXIMO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cada mensaje puede tener hasta 500 caracteres");
    }

    @Test
    @DisplayName("El último mensaje tiene que ser de la persona")
    void deShouldRejectAConversationEndingWithSami() {
        assertThatThrownBy(() -> ConversacionObjetivo.de(List.of(persona("Hola"), sami("¿Qué querés?")), MAXIMO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El último mensaje tiene que ser tuyo");
    }

    @Test
    @DisplayName("Seis mensajes de la persona se aceptan y no quedan más; siete se rechazan")
    void deShouldLimitTheMessagesOfThePerson() {
        ConversacionObjetivo llena = ConversacionObjetivo.de(mensajesDeLaPersona(6), MAXIMO);

        assertThat(llena.mensajesRestantes()).isZero();
        assertThatThrownBy(() -> ConversacionObjetivo.de(mensajesDeLaPersona(7), MAXIMO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Llegaste al máximo de 6 mensajes de esta conversación. "
                        + "Guardá lo que armamos o empezá una nueva");
    }

    @Test
    @DisplayName("Los mensajes de Sami no cuentan para el tope")
    void deShouldNotCountTheMessagesOfSami() {
        List<MensajeObjetivo> mensajes = new ArrayList<>(mensajesDeLaPersona(5));
        mensajes.add(1, sami("Una pregunta"));
        mensajes.add(3, sami("Otra pregunta"));

        assertThat(ConversacionObjetivo.de(mensajes, MAXIMO).mensajesRestantes()).isEqualTo(1);
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

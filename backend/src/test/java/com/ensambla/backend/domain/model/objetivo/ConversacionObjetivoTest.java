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
    void unaConversacionValidaDejaLosMensajesSinEspaciosYCuentaLosQueQuedan() {
        dadoMensajes(persona("  Quiero vender más \n"), sami("¿Qué producto?"), persona("La yerba"));

        cuandoSeArmaLaConversacion();

        entoncesLosTextosSon("Quiero vender más", "¿Qué producto?", "La yerba");
        entoncesQuedanMensajes(4);
    }

    @Test
    @DisplayName("El objetivo de la persona es lo primero que escribió")
    void elObjetivoDeLaPersonaEsLoPrimeroQueEscribio() {
        dadoMensajes(sami("Hola"), persona("Quiero vender más"), persona("La yerba"));

        cuandoSeArmaLaConversacion();

        entoncesElObjetivoDeLaPersonaEs("Quiero vender más");
    }

    @Test
    @DisplayName("A la IA le llegan los mensajes sin correos ni teléfonos")
    void aLaIaLeLleganLosMensajesSinCorreosNiTelefonos() {
        dadoMensajes(persona("Cobrale a ana@mail.com, tel 11 5555 5555"));

        cuandoSeArmaLaConversacion();

        entoncesALaIaLeLlegaSinDatosPersonales("Cobrale a [dato oculto], tel [dato oculto]");
        entoncesElMensajeOriginalConservaElCorreo("ana@mail.com");
    }

    @Test
    @DisplayName("Sin mensajes no hay conversación")
    void sinMensajesNoHayConversacion() {
        dadoMensajes();

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechazaConMensaje("Contale a Sami qué querés lograr");
    }

    @Test
    @DisplayName("Con mensajes nulos no hay conversación")
    void conMensajesNulosNoHayConversacion() {
        mensajes = null;

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Un mensaje vacío o en blanco se rechaza")
    void unMensajeVacioOEnBlancoSeRechaza() {
        dadoMensajes(persona("   "));

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechazaConMensaje("Los mensajes no pueden estar vacíos");
    }

    @Test
    @DisplayName("Un mensaje nulo se rechaza")
    void unMensajeNuloSeRechaza() {
        dadoMensajes(persona(null));

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Un mensaje de 500 caracteres se acepta")
    void unMensajeDe500CaracteresSeAcepta() {
        dadoMensajes(persona("a".repeat(LARGO_MAXIMO_DE_MENSAJE)));

        cuandoSeArmaLaConversacion();

        entoncesHayMensajes(1);
    }

    @Test
    @DisplayName("Un mensaje de 501 caracteres se rechaza")
    void unMensajeDe501CaracteresSeRechaza() {
        dadoMensajes(persona("a".repeat(LARGO_MAXIMO_DE_MENSAJE + 1)));

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechazaConMensaje("Cada mensaje puede tener hasta 500 caracteres");
    }

    @Test
    @DisplayName("El último mensaje tiene que ser de la persona")
    void elUltimoMensajeTieneQueSerDeLaPersona() {
        dadoMensajes(persona("Hola"), sami("¿Qué querés?"));

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechazaConMensaje("El último mensaje tiene que ser tuyo");
    }

    @Test
    @DisplayName("Seis mensajes de la persona se aceptan y no quedan más")
    void seisMensajesDeLaPersonaSeAceptanYNoQuedanMas() {
        dadoMensajesDeLaPersona(6);

        cuandoSeArmaLaConversacion();

        entoncesQuedanMensajes(0);
    }

    @Test
    @DisplayName("Siete mensajes de la persona se rechazan")
    void sieteMensajesDeLaPersonaSeRechazan() {
        dadoMensajesDeLaPersona(7);

        cuandoSeArmaLaConversacionConError();

        entoncesSeRechazaConMensaje("Llegaste al máximo de 6 mensajes de esta conversación. "
                + "Guardá lo que armamos o empezá una nueva");
    }

    @Test
    @DisplayName("Los mensajes de Sami no cuentan para el tope")
    void losMensajesDeSamiNoCuentanParaElTope() {
        dadoCincoMensajesDeLaPersonaIntercaladosConDosDeSami();

        cuandoSeArmaLaConversacion();

        entoncesQuedanMensajes(1);
    }

    // --- arrange ---
    private void dadoMensajes(MensajeObjetivo... mensajesDados) {
        mensajes = List.of(mensajesDados);
    }

    private void dadoMensajesDeLaPersona(int cantidad) {
        mensajes = mensajesDeLaPersona(cantidad);
    }

    private void dadoCincoMensajesDeLaPersonaIntercaladosConDosDeSami() {
        List<MensajeObjetivo> intercalados = new ArrayList<>(mensajesDeLaPersona(5));
        intercalados.add(1, sami("Una pregunta"));
        intercalados.add(3, sami("Otra pregunta"));
        mensajes = intercalados;
    }

    // --- act ---
    private void cuandoSeArmaLaConversacion() {
        conversacion = ConversacionObjetivo.de(mensajes, MAXIMO);
    }

    private void cuandoSeArmaLaConversacionConError() {
        error = catchThrowable(this::cuandoSeArmaLaConversacion);
    }

    // --- assert ---
    private void entoncesLosTextosSon(String... textos) {
        assertThat(conversacion.mensajes()).extracting(MensajeObjetivo::texto).containsExactly(textos);
    }

    private void entoncesQuedanMensajes(int cantidad) {
        assertThat(conversacion.mensajesRestantes()).isEqualTo(cantidad);
    }

    private void entoncesHayMensajes(int cantidad) {
        assertThat(conversacion.mensajes()).hasSize(cantidad);
    }

    private void entoncesElObjetivoDeLaPersonaEs(String esperado) {
        assertThat(conversacion.objetivoDeLaPersona()).isEqualTo(esperado);
    }

    private void entoncesALaIaLeLlegaSinDatosPersonales(String texto) {
        assertThat(conversacion.paraLaIa()).extracting(MensajeObjetivo::texto).containsExactly(texto);
    }

    private void entoncesElMensajeOriginalConservaElCorreo(String correo) {
        assertThat(conversacion.mensajes().getFirst().texto()).contains(correo);
    }

    private void entoncesSeRechaza() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaConMensaje(String mensaje) {
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

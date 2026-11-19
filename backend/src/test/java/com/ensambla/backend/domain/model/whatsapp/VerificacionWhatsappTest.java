package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mockStatic;

class VerificacionWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final TelefonoWhatsapp TELEFONO = new TelefonoWhatsapp("5491122334455");
    private static final TelefonoWhatsapp OTRO_TELEFONO = new TelefonoWhatsapp("5491100000000");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final String CODIGO = "123456";
    private static final String CODIGO_INCORRECTO = "000000";

    private VerificacionWhatsapp verificacion;
    private Throwable error;

    @Test
    @DisplayName("Nueva guarda la huella del código, no el código, y vence a los 10 minutos")
    void nuevaGuardaLaHuellaDelCodigoYVenceALosDiezMinutos() {
        cuandoSeMandaUnCodigo(CODIGO);

        entoncesGuardaSoloLaHuellaDe(CODIGO);
        entoncesVenceEn(AHORA.plusMinutes(10));
        entoncesSePuedeReenviarDesde(AHORA.plusSeconds(60));
        entoncesEsDelTelefono("5491122334455");
    }

    @Test
    @DisplayName("Confirmar con el código correcto no lanza error aunque tenga espacios")
    void confirmarConElCodigoCorrectoNoLanzaError() {
        dadoQueSeMandoElCodigo(CODIGO);

        cuandoSeConfirma(" 123456 ", AHORA.plusMinutes(1));

        entoncesNoHuboError();
    }

    @Test
    @DisplayName("Confirmar con un código incorrecto cuenta el intento y dice cuántos quedan")
    void confirmarConUnCodigoIncorrectoCuentaElIntento() {
        dadoQueSeMandoElCodigo(CODIGO);

        cuandoSeConfirma(CODIGO_INCORRECTO, AHORA.plusMinutes(1));

        entoncesSeRechazaPorque("El código no es correcto. Te quedan 4 intentos");
        entoncesLlevaIntentos(1);
    }

    @Test
    @DisplayName("Confirmar después de 5 intentos fallidos pide un código nuevo aunque el código sea el correcto")
    void confirmarDespuesDeCincoIntentosPideUnCodigoNuevo() {
        dadoQueSeMandoElCodigo(CODIGO);
        dadoQueSeEquivocoTodasLasVeces();

        cuandoSeConfirma(CODIGO, AHORA.plusMinutes(1));

        entoncesSeRechazaPorque("Hiciste demasiados intentos. Pedí un código nuevo");
    }

    @Test
    @DisplayName("Confirmar con el código vencido pide uno nuevo")
    void confirmarConElCodigoVencidoPideUnoNuevo() {
        dadoQueSeMandoElCodigo(CODIGO);

        cuandoSeConfirma(CODIGO, AHORA.plusMinutes(10));

        entoncesSeRechazaPorque("El código venció. Pedí uno nuevo");
    }

    @Test
    @DisplayName("Reenviar antes de que pasen 60 segundos pide esperar")
    void reenviarAntesDeSesentaSegundosPideEsperar() {
        dadoQueSeMandoElCodigo(CODIGO);

        cuandoSeReenvia(TELEFONO, "654321", AHORA.plusSeconds(30));

        entoncesSeRechazaPorque("Esperá un momento antes de pedir otro código");
    }

    @Test
    @DisplayName("Reenviar reemplaza el código y el número, reinicia los intentos y cuenta el envío")
    void reenviarReemplazaElCodigoYElNumero() {
        dadoQueSeMandoElCodigo(CODIGO);
        dadoQueSeEquivocoUnaVez();

        cuandoSeReenvia(OTRO_TELEFONO, "654321", AHORA.plusSeconds(61));

        entoncesLlevaIntentos(0);
        entoncesEsDelTelefono("5491100000000");
        entoncesGuardaSoloLaHuellaDe("654321");
        entoncesLlevaEnviosEnLaVentana(2);
        entoncesVenceEn(AHORA.plusSeconds(61).plusMinutes(10));
    }

    @Test
    @DisplayName("Reenviar más de 5 veces en una hora se rechaza")
    void reenviarMasDeCincoVecesEnUnaHoraSeRechaza() {
        dadoQueSeMandoElCodigo(CODIGO);
        dadoQueSeReenvioHastaElLimite();

        cuandoSeReenvia(TELEFONO, "222222", AHORA.plusSeconds(61L * VerificacionWhatsapp.MAX_ENVIOS_POR_VENTANA));

        entoncesSeRechazaPorque("Pediste demasiados códigos. Probá de nuevo en una hora");
    }

    @Test
    @DisplayName("Pasada la hora se vuelve a poder reenviar y la cuenta de envíos empieza de nuevo")
    void pasadaLaHoraSeVuelveAPoderReenviar() {
        dadoQueSeMandoElCodigo(CODIGO);
        dadoQueSeReenvioHastaElLimite();

        cuandoSeReenvia(TELEFONO, "333333", AHORA.plusHours(1).plusSeconds(1));

        entoncesNoHuboError();
        entoncesLlevaEnviosEnLaVentana(1);
    }

    @Test
    @DisplayName("Sin el algoritmo SHA-256 no se puede guardar la huella del código")
    void sinElAlgoritmoNoSePuedeGuardarLaHuella() {
        cuandoSeCalculaLaHuellaSinAlgoritmo();

        entoncesFallaLaHuella();
    }

    private void dadoQueSeMandoElCodigo(String codigo) {
        cuandoSeMandaUnCodigo(codigo);
    }

    private void dadoQueSeEquivocoUnaVez() {
        cuandoSeConfirma(CODIGO_INCORRECTO, AHORA.plusSeconds(10));
    }

    private void dadoQueSeEquivocoTodasLasVeces() {
        IntStream.range(0, VerificacionWhatsapp.MAX_INTENTOS)
                .forEach(intento -> cuandoSeConfirma(CODIGO_INCORRECTO, AHORA.plusMinutes(1)));
    }

    /** Un reenvío por minuto hasta agotar los envíos de la hora. */
    private void dadoQueSeReenvioHastaElLimite() {
        IntStream.range(1, VerificacionWhatsapp.MAX_ENVIOS_POR_VENTANA)
                .forEach(envio -> verificacion.reenviar(TELEFONO, "111111", AHORA.plusSeconds(61L * envio)));
    }

    private void cuandoSeMandaUnCodigo(String codigo) {
        verificacion = VerificacionWhatsapp.nueva(USUARIO_ID, TELEFONO, codigo, AHORA);
    }

    private void cuandoSeConfirma(String codigo, OffsetDateTime momento) {
        error = catchThrowable(() -> verificacion.confirmar(codigo, momento));
    }

    private void cuandoSeReenvia(TelefonoWhatsapp telefono, String codigo, OffsetDateTime momento) {
        error = catchThrowable(() -> verificacion.reenviar(telefono, codigo, momento));
    }

    private void cuandoSeCalculaLaHuellaSinAlgoritmo() {
        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException());
            error = catchThrowable(() -> VerificacionWhatsapp.huella(CODIGO));
        }
    }

    private void entoncesFallaLaHuella() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesGuardaSoloLaHuellaDe(String codigo) {
        assertThat(verificacion.getCodigoHash()).isEqualTo(VerificacionWhatsapp.huella(codigo)).doesNotContain(codigo);
    }

    private void entoncesVenceEn(OffsetDateTime vencimiento) {
        assertThat(verificacion.getExpiraEn()).isEqualTo(vencimiento);
    }

    private void entoncesSePuedeReenviarDesde(OffsetDateTime momento) {
        assertThat(verificacion.reenviarDesde()).isEqualTo(momento);
    }

    private void entoncesEsDelTelefono(String telefono) {
        assertThat(verificacion.getTelefono()).isEqualTo(telefono);
    }

    private void entoncesNoHuboError() {
        assertThat(error).isNull();
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesLlevaIntentos(int intentos) {
        assertThat(verificacion.getIntentos()).isEqualTo(intentos);
    }

    private void entoncesLlevaEnviosEnLaVentana(int envios) {
        assertThat(verificacion.getEnviosVentana()).isEqualTo(envios);
    }
}
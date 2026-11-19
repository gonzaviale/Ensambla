package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class WhatsappModelTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final String TELEFONO = "5491122334455";

    private VinculoWhatsapp vinculo;
    private CodigoVinculacion codigo;
    private MensajeWhatsapp mensaje;
    private Throwable error;

    @ParameterizedTest
    @CsvSource({"5491122334455@s.whatsapp.net", "+54 9 11-2233-4455", "5491122334455:12@s.whatsapp.net"})
    @DisplayName("El teléfono deja sólo los dígitos del chat, del número con formato o del identificador con dispositivo")
    void elTelefonoDejaSoloLosDigitos(String escrito) {
        entoncesElTelefonoDe(escrito, TELEFONO);
    }

    @Test
    @DisplayName("Un teléfono nulo no es válido")
    void unTelefonoNuloNoEsValido() {
        cuandoSeIntenta(() -> TelefonoWhatsapp.de(null));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Un teléfono armado sin número no es válido")
    void unTelefonoArmadoSinNumeroNoEsValido() {
        cuandoSeIntenta(() -> new TelefonoWhatsapp(null));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Un teléfono corto no es válido")
    void unTelefonoCortoNoEsValido() {
        cuandoSeIntenta(() -> new TelefonoWhatsapp("12345"));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Un identificador de chat con letras no es un teléfono válido")
    void unIdentificadorConLetrasNoEsValido() {
        cuandoSeIntenta(() -> TelefonoWhatsapp.de("abc@lid"));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("El vínculo nuevo guarda el usuario, el negocio, el teléfono y cuándo se hizo")
    void elVinculoNuevoGuardaTodosSusDatos() {
        cuandoSeVincula();

        entoncesElVinculoEsDelUsuarioYElNegocio();
        entoncesElVinculoEsDelTelefonoDesde(TELEFONO, AHORA);
    }

    @Test
    @DisplayName("El código nuevo vence a los 10 minutos y todavía no se usó")
    void elCodigoNuevoVenceALosDiezMinutos() {
        cuandoSeGeneraElCodigo("ABC234");

        entoncesElCodigoVenceEn(AHORA.plusMinutes(10));
        entoncesElCodigoNoSeUso();
    }

    @Test
    @DisplayName("El código está vigente hasta el instante en que vence")
    void elCodigoEstaVigenteHastaQueVence() {
        cuandoSeGeneraElCodigo("ABC234");

        entoncesElCodigoEstaVigenteA(AHORA.plusMinutes(9), true);
        entoncesElCodigoEstaVigenteA(AHORA.plusMinutes(10), false);
    }

    @Test
    @DisplayName("Usar el código lo marca como usado y ya no está vigente")
    void usarElCodigoLoMarcaComoUsado() {
        dadoUnCodigoGenerado("ABC234");

        cuandoSeUsaElCodigo(AHORA.plusMinutes(1));

        entoncesElCodigoSeUsoEl(AHORA.plusMinutes(1));
        entoncesElCodigoEstaVigenteA(AHORA.plusMinutes(2), false);
    }

    @Test
    @DisplayName("No se puede usar un código vencido")
    void noSePuedeUsarUnCodigoVencido() {
        dadoUnCodigoGenerado("ABC234");

        cuandoSeIntentaUsarElCodigo(AHORA.plusMinutes(11));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("No se puede usar un código ya usado")
    void noSePuedeUsarUnCodigoYaUsado() {
        dadoUnCodigoGenerado("XYZ789");
        dadoQueElCodigoSeUso();

        cuandoSeIntentaUsarElCodigo(AHORA.plusMinutes(1));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("El mensaje recibido copia el contenido y queda en estado RECIBIDO")
    void elMensajeRecibidoCopiaElContenido() {
        cuandoLlegaUnAudio();

        entoncesElMensajeEsDelNegocioYElTelefono();
        entoncesElMensajeEsUnAudioDe(7);
        entoncesElMensajeQuedaRecibido();
    }

    private void dadoUnCodigoGenerado(String valor) {
        cuandoSeGeneraElCodigo(valor);
    }

    private void dadoQueElCodigoSeUso() {
        codigo.usar(AHORA);
    }

    private void cuandoSeIntenta(ThrowingCallable accion) {
        error = catchThrowable(accion);
    }

    private void cuandoSeVincula() {
        vinculo = VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA);
    }

    private void cuandoSeGeneraElCodigo(String valor) {
        codigo = CodigoVinculacion.nuevo(USUARIO_ID, valor, AHORA);
    }

    private void cuandoSeUsaElCodigo(OffsetDateTime momento) {
        codigo.usar(momento);
    }

    private void cuandoSeIntentaUsarElCodigo(OffsetDateTime momento) {
        cuandoSeIntenta(() -> codigo.usar(momento));
    }

    private void cuandoLlegaUnAudio() {
        mensaje = MensajeWhatsapp.recibido(NEGOCIO_ID,
                new MensajeEntrante(TELEFONO, "ID1", TipoMensajeWhatsapp.AUDIO, null, "audio/ogg", 7, AHORA));
    }

    private static void entoncesElTelefonoDe(String escrito, String queda) {
        assertThat(TelefonoWhatsapp.de(escrito).valor()).isEqualTo(queda);
    }

    private void entoncesSeRechaza() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesElVinculoEsDelUsuarioYElNegocio() {
        assertThat(vinculo.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(vinculo.getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesElVinculoEsDelTelefonoDesde(String telefono, OffsetDateTime momento) {
        assertThat(vinculo.getTelefono()).isEqualTo(telefono);
        assertThat(vinculo.getVinculadoEn()).isEqualTo(momento);
    }

    private void entoncesElCodigoVenceEn(OffsetDateTime momento) {
        assertThat(codigo.getExpiraEn()).isEqualTo(momento);
    }

    private void entoncesElCodigoNoSeUso() {
        assertThat(codigo.getUsadoEn()).isNull();
    }

    private void entoncesElCodigoSeUsoEl(OffsetDateTime momento) {
        assertThat(codigo.getUsadoEn()).isEqualTo(momento);
    }

    private void entoncesElCodigoEstaVigenteA(OffsetDateTime momento, boolean vigente) {
        assertThat(codigo.estaVigente(momento)).isEqualTo(vigente);
    }

    private void entoncesElMensajeEsDelNegocioYElTelefono() {
        assertThat(mensaje.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(mensaje.getTelefono()).isEqualTo(TELEFONO);
        assertThat(mensaje.getIdExterno()).isEqualTo("ID1");
    }

    private void entoncesElMensajeEsUnAudioDe(int segundos) {
        assertThat(mensaje.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
        assertThat(mensaje.getMimeMedia()).isEqualTo("audio/ogg");
        assertThat(mensaje.getDuracionSeg()).isEqualTo(segundos);
    }

    private void entoncesElMensajeQuedaRecibido() {
        assertThat(mensaje.getRecibidoEn()).isEqualTo(AHORA);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.RECIBIDO);
    }
}
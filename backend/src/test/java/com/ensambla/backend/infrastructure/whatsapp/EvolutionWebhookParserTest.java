package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionWebhookParserTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final String CONTACTO = "5491122334455@s.whatsapp.net";
    private static final String TEXTO_HOLA = "\"message\":{\"conversation\":\"hola\"}";

    private final JsonMapper json = JsonMapper.builder().build();
    private final EvolutionWebhookParser parser = new EvolutionWebhookParser(Clock.fixed(INSTANTE, ZoneOffset.UTC));

    private JsonNode cuerpo;
    private Optional<MensajeEntrante> mensaje;

    @Test
    @DisplayName("Lee un texto simple con el teléfono, el id y la hora del mensaje")
    void leeUnTextoSimpleConElTelefonoElIdYLaHoraDelMensaje() {
        dadoUnTextoSimple();

        cuandoSeParsea();

        entoncesSeLeeElMensaje(new MensajeEntrante("5491122334455", "ID1", TipoMensajeWhatsapp.TEXTO,
                "gasté 5000 en harina", null, null, AHORA));
    }

    @Test
    @DisplayName("Lee un texto que viene como mensaje extendido y acepta el evento en mayúsculas")
    void leeUnTextoQueVieneComoMensajeExtendidoYAceptaElEventoEnMayusculas() {
        dadoUnTextoExtendidoConElEventoEnMayusculas();

        cuandoSeParsea();

        entoncesSeLeeElTextoYLaHoraActual("vendí 3 panes");
    }

    @Test
    @DisplayName("Lee una nota de voz con su tipo de audio y su duración")
    void leeUnaNotaDeVozConSuTipoDeAudioYSuDuracion() {
        dadoUnaNotaDeVozDeSieteSegundos();

        cuandoSeParsea();

        entoncesSeLeeElMensaje(new MensajeEntrante("5491122334455", "ID3", TipoMensajeWhatsapp.AUDIO, null,
                "audio/ogg; codecs=opus", 7, AHORA));
    }

    @Test
    @DisplayName("Lee una foto con su pie y su tipo de imagen")
    void leeUnaFotoConSuPieYSuTipoDeImagen() {
        dadoUnaFotoConPie();

        cuandoSeParsea();

        entoncesSeLeeElMensaje(new MensajeEntrante("5491122334455", "ID6", TipoMensajeWhatsapp.IMAGEN,
                "ventas de hoy", "image/png", null, AHORA));
    }

    @Test
    @DisplayName("Una foto sin pie ni tipo se lee como JPEG sin texto")
    void unaFotoSinPieNiTipoSeLeeComoJpegSinTexto() {
        dadoUnaFotoSinPieNiTipo();

        cuandoSeParsea();

        entoncesSeLeeElMensaje(new MensajeEntrante("5491122334455", "ID7", TipoMensajeWhatsapp.IMAGEN,
                null, "image/jpeg", null, AHORA));
    }

    @Test
    @DisplayName("Una nota de voz sin tipo ni duración usa audio/ogg y duración vacía")
    void unaNotaDeVozSinTipoNiDuracionUsaAudioOggYDuracionVacia() {
        dadoUnaNotaDeVozSinTipoNiDuracion();

        cuandoSeParsea();

        entoncesElAudioEsOggSinDuracion();
    }

    @Test
    @DisplayName("Con el direccionamiento @lid toma el número real de senderPn")
    void conElDireccionamientoLidTomaElNumeroRealDeSenderPn() {
        dadoUnChatLidConSenderPn();

        cuandoSeParsea();

        entoncesElTelefonoEsElNumeroReal();
    }

    @Test
    @DisplayName("Con @lid y sin senderPn usa remoteJidAlt")
    void conLidYSinSenderPnUsaRemoteJidAlt() {
        dadoUnChatLidConRemoteJidAlt();

        cuandoSeParsea();

        entoncesElTelefonoEsElNumeroReal();
    }

    @Test
    @DisplayName("Con @lid y sin número real ignora el mensaje")
    void conLidYSinNumeroRealIgnoraElMensaje() {
        dadoUnChatLidSinNumeroReal();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora otros eventos")
    void ignoraOtrosEventos() {
        dadoUnEventoDeConexion();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora mis propios mensajes")
    void ignoraMisPropiosMensajes() {
        dadoUnMensajeMio();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora los grupos")
    void ignoraLosGrupos() {
        dadoUnMensajeDeUnGrupo();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora los estados")
    void ignoraLosEstados() {
        dadoUnEstadoDeWhatsapp();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora los mensajes sin id")
    void ignoraLosMensajesSinId() {
        dadoUnMensajeSinId();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora un chat con un número inválido")
    void ignoraUnChatConUnNumeroInvalido() {
        dadoUnChatConUnNumeroInvalido();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora los mensajes que no son texto, audio ni foto")
    void ignoraLosMensajesQueNoSonTextoAudioNiFoto() {
        dadoUnSticker();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    @Test
    @DisplayName("Ignora un cuerpo vacío")
    void ignoraUnCuerpoVacio() {
        dadoUnCuerpoVacio();

        cuandoSeParsea();

        entoncesSeIgnoraElMensaje();
    }

    // --- dado ---

    private void dadoUnTextoSimple() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, "ID1"),
                "\"message\":{\"conversation\":\"gasté 5000 en harina\"},\"messageTimestamp\":1792411200");
    }

    private void dadoUnTextoExtendidoConElEventoEnMayusculas() {
        cuerpo = evento("MESSAGES_UPSERT", key(CONTACTO, false, "ID2"),
                "\"message\":{\"extendedTextMessage\":{\"text\":\"vendí 3 panes\"}}");
    }

    private void dadoUnaNotaDeVozDeSieteSegundos() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, "ID3"),
                "\"message\":{\"audioMessage\":{\"mimetype\":\"audio/ogg; codecs=opus\",\"seconds\":7}}");
    }

    private void dadoUnaFotoConPie() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, "ID6"),
                "\"message\":{\"imageMessage\":{\"mimetype\":\"image/png\",\"caption\":\"ventas de hoy\"}}");
    }

    private void dadoUnaFotoSinPieNiTipo() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, "ID7"),
                "\"message\":{\"imageMessage\":{\"caption\":\"  \"}}");
    }

    private void dadoUnaNotaDeVozSinTipoNiDuracion() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, "ID4"), "\"message\":{\"audioMessage\":{}}");
    }

    private void dadoUnChatLidConSenderPn() {
        cuerpo = evento("messages.upsert",
                "\"key\":{\"remoteJid\":\"99887766@lid\",\"fromMe\":false,\"id\":\"ID5\","
                        + "\"senderPn\":\"" + CONTACTO + "\"}",
                TEXTO_HOLA);
    }

    private void dadoUnChatLidConRemoteJidAlt() {
        cuerpo = evento("messages.upsert",
                "\"key\":{\"remoteJid\":\"99887766@lid\",\"fromMe\":false,\"id\":\"ID6\","
                        + "\"remoteJidAlt\":\"" + CONTACTO + "\"}",
                TEXTO_HOLA);
    }

    private void dadoUnChatLidSinNumeroReal() {
        cuerpo = evento("messages.upsert",
                "\"key\":{\"remoteJid\":\"99887766@lid\",\"fromMe\":false,\"id\":\"ID7\"}", TEXTO_HOLA);
    }

    private void dadoUnEventoDeConexion() {
        cuerpo = evento("connection.update", key(CONTACTO, false, "A"), TEXTO_HOLA);
    }

    private void dadoUnMensajeMio() {
        cuerpo = evento("messages.upsert", key(CONTACTO, true, "B"), TEXTO_HOLA);
    }

    private void dadoUnMensajeDeUnGrupo() {
        cuerpo = evento("messages.upsert", key("120363@g.us", false, "C"), TEXTO_HOLA);
    }

    private void dadoUnEstadoDeWhatsapp() {
        cuerpo = evento("messages.upsert", key("status@broadcast", false, "D"), TEXTO_HOLA);
    }

    private void dadoUnMensajeSinId() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, ""), TEXTO_HOLA);
    }

    private void dadoUnChatConUnNumeroInvalido() {
        cuerpo = evento("messages.upsert", key("abc@s.whatsapp.net", false, "E"), TEXTO_HOLA);
    }

    private void dadoUnSticker() {
        cuerpo = evento("messages.upsert", key(CONTACTO, false, "F"), "\"message\":{\"stickerMessage\":{}}");
    }

    private void dadoUnCuerpoVacio() {
        cuerpo = json.readTree("{}");
    }

    // --- cuando ---

    private void cuandoSeParsea() {
        mensaje = parser.parsear(cuerpo);
    }

    // --- entonces ---

    private void entoncesSeLeeElMensaje(MensajeEntrante esperado) {
        assertThat(mensaje).contains(esperado);
    }

    private void entoncesSeLeeElTextoYLaHoraActual(String texto) {
        assertThat(mensaje).get().satisfies(leido -> {
            assertThat(leido.texto()).isEqualTo(texto);
            assertThat(leido.recibidoEn()).isEqualTo(AHORA);
        });
    }

    private void entoncesElAudioEsOggSinDuracion() {
        assertThat(mensaje).get().satisfies(leido -> {
            assertThat(leido.mimeMedia()).isEqualTo("audio/ogg");
            assertThat(leido.duracionSeg()).isNull();
        });
    }

    private void entoncesElTelefonoEsElNumeroReal() {
        assertThat(mensaje).get().extracting(MensajeEntrante::telefono).isEqualTo("5491122334455");
    }

    private void entoncesSeIgnoraElMensaje() {
        assertThat(mensaje).isEmpty();
    }

    // --- datos ---

    private JsonNode evento(String evento, String key, String resto) {
        return json.readTree("{\"event\":\"" + evento + "\",\"data\":{" + key + "," + resto + "}}");
    }

    private static String key(String jid, boolean propio, String id) {
        return "\"key\":{\"remoteJid\":\"" + jid + "\",\"fromMe\":" + propio + ",\"id\":\"" + id + "\"}";
    }
}

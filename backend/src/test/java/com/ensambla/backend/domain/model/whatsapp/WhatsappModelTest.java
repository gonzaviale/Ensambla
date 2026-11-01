package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappModelTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Test
    @DisplayName("El teléfono acepta el identificador del chat y deja sólo los dígitos")
    void telefonoDeShouldStripJidSuffix() {
        assertThat(TelefonoWhatsapp.de("5491122334455@s.whatsapp.net").valor()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("El teléfono acepta un número con signo, espacios y guiones")
    void telefonoDeShouldStripFormatting() {
        assertThat(TelefonoWhatsapp.de("+54 9 11-2233-4455").valor()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("El teléfono ignora el sufijo de dispositivo del identificador")
    void telefonoDeShouldIgnoreDeviceSuffix() {
        assertThat(TelefonoWhatsapp.de("5491122334455:12@s.whatsapp.net").valor()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Un teléfono nulo, corto o con letras no es válido")
    void telefonoShouldRejectInvalidNumbers() {
        assertThatThrownBy(() -> TelefonoWhatsapp.de(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new TelefonoWhatsapp(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new TelefonoWhatsapp("12345")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> TelefonoWhatsapp.de("abc@lid")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("El vínculo nuevo guarda el usuario, el negocio, el teléfono y cuándo se hizo")
    void vinculoNuevoShouldCopyEveryField() {
        VinculoWhatsapp result = VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID,
                new TelefonoWhatsapp("5491122334455"), AHORA);

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTelefono()).isEqualTo("5491122334455");
        assertThat(result.getVinculadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("El código nuevo vence a los 10 minutos")
    void codigoNuevoShouldExpireAfterTenMinutes() {
        CodigoVinculacion result = CodigoVinculacion.nuevo(USUARIO_ID, "ABC234", AHORA);

        assertThat(result.getExpiraEn()).isEqualTo(AHORA.plusMinutes(10));
        assertThat(result.getUsadoEn()).isNull();
    }

    @Test
    @DisplayName("El código está vigente hasta el instante en que vence")
    void estaVigenteShouldBeTrueOnlyBeforeExpiration() {
        CodigoVinculacion codigo = CodigoVinculacion.nuevo(USUARIO_ID, "ABC234", AHORA);

        assertThat(codigo.estaVigente(AHORA.plusMinutes(9))).isTrue();
        assertThat(codigo.estaVigente(AHORA.plusMinutes(10))).isFalse();
    }

    @Test
    @DisplayName("Usar el código lo marca como usado y ya no está vigente")
    void usarShouldMarkCodigoAsUsed() {
        CodigoVinculacion codigo = CodigoVinculacion.nuevo(USUARIO_ID, "ABC234", AHORA);

        codigo.usar(AHORA.plusMinutes(1));

        assertThat(codigo.getUsadoEn()).isEqualTo(AHORA.plusMinutes(1));
        assertThat(codigo.estaVigente(AHORA.plusMinutes(2))).isFalse();
    }

    @Test
    @DisplayName("No se puede usar un código vencido ni uno ya usado")
    void usarShouldRejectExpiredOrUsedCodigo() {
        CodigoVinculacion vencido = CodigoVinculacion.nuevo(USUARIO_ID, "ABC234", AHORA);
        CodigoVinculacion usado = CodigoVinculacion.nuevo(USUARIO_ID, "XYZ789", AHORA);
        usado.usar(AHORA);

        assertThatThrownBy(() -> vencido.usar(AHORA.plusMinutes(11))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> usado.usar(AHORA.plusMinutes(1))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("El mensaje recibido copia el contenido y queda en estado RECIBIDO")
    void recibidoShouldCopyEntranteAndStartReceived() {
        MensajeEntrante entrante = new MensajeEntrante("5491122334455", "ID1", TipoMensajeWhatsapp.AUDIO, null,
                "audio/ogg", 7, AHORA);

        MensajeWhatsapp result = MensajeWhatsapp.recibido(NEGOCIO_ID, entrante);

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTelefono()).isEqualTo("5491122334455");
        assertThat(result.getIdExterno()).isEqualTo("ID1");
        assertThat(result.getTipo()).isEqualTo(TipoMensajeWhatsapp.AUDIO);
        assertThat(result.getMimeMedia()).isEqualTo("audio/ogg");
        assertThat(result.getDuracionSeg()).isEqualTo(7);
        assertThat(result.getRecibidoEn()).isEqualTo(AHORA);
        assertThat(result.getEstado()).isEqualTo(EstadoMensajeWhatsapp.RECIBIDO);
    }
}

package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.enums.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IWhatsappJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final String TELEFONO = "5491122334455";
    private static final String CODIGO = "482913";

    @Autowired
    private IVinculoWhatsappJpaRepository vinculos;

    @Autowired
    private IMensajeWhatsappJpaRepository mensajes;

    private UUID negocioA;
    private UUID negocioB;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
    }

    @Test
    @DisplayName("El número vinculado resuelve sólo su negocio")
    void findByTelefonoShouldReturnOnlyItsNegocio() {
        vinculos.saveAndFlush(vinculado(negocioA, TELEFONO));
        vinculos.saveAndFlush(vinculado(negocioB, "5491199887766"));

        assertThat(vinculos.findByTelefono(TELEFONO)).get()
                .extracting(VinculoWhatsappEntity::getNegocioId).isEqualTo(negocioA);
        assertThat(vinculos.findByTelefono("5491100000000")).isEmpty();
    }

    @Test
    @DisplayName("Encuentra el vínculo por su código y sabe si un código está en uso")
    void findByCodigoShouldFindThePendingVinculo() {
        vinculos.saveAndFlush(conCodigo(negocioA, CODIGO));

        assertThat(vinculos.findByCodigo(CODIGO)).get()
                .extracting(VinculoWhatsappEntity::getNegocioId).isEqualTo(negocioA);
        assertThat(vinculos.existsByCodigo(CODIGO)).isTrue();
        assertThat(vinculos.existsByCodigo("000000")).isFalse();
        assertThat(vinculos.findByNegocioId(negocioA)).isPresent();
        assertThat(vinculos.findByNegocioId(negocioB)).isEmpty();
    }

    @Test
    @DisplayName("Un número no puede estar vinculado a dos negocios")
    void shouldRejectTheSameTelefonoInTwoNegocios() {
        vinculos.saveAndFlush(vinculado(negocioA, TELEFONO));

        assertThatThrownBy(() -> vinculos.saveAndFlush(vinculado(negocioB, TELEFONO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Un negocio tiene un solo vínculo")
    void shouldRejectASecondVinculoForTheSameNegocio() {
        vinculos.saveAndFlush(conCodigo(negocioA, CODIGO));

        assertThatThrownBy(() -> vinculos.saveAndFlush(conCodigo(negocioA, "105377")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Rechaza un número que no son sólo dígitos")
    void shouldRejectTelefonoWithSymbols() {
        assertThatThrownBy(() -> vinculos.saveAndFlush(vinculado(negocioA, "+54 9 11 2233-4455")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Rechaza un código sin vencimiento")
    void shouldRejectCodigoWithoutVencimiento() {
        VinculoWhatsappEntity vinculo = conCodigo(negocioA, CODIGO);
        vinculo.setCodigoVenceEn(null);

        assertThatThrownBy(() -> vinculos.saveAndFlush(vinculo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Guarda el texto y la nota de voz y sabe si un mensaje de Meta ya llegó")
    void saveShouldPersistTextAndAudio() {
        mensajes.saveAndFlush(mensaje(negocioA, "wamid.1", TipoMensajeWhatsapp.TEXTO, "Vendí 3 velas", null));
        mensajes.saveAndFlush(mensaje(negocioA, "wamid.2", TipoMensajeWhatsapp.AUDIO, null, "998877"));

        assertThat(mensajes.existsByWaMensajeId("wamid.1")).isTrue();
        assertThat(mensajes.existsByWaMensajeId("wamid.2")).isTrue();
        assertThat(mensajes.existsByWaMensajeId("wamid.3")).isFalse();
    }

    @Test
    @DisplayName("El mismo mensaje de Meta no se guarda dos veces")
    void shouldRejectRepeatedWaMensajeId() {
        mensajes.saveAndFlush(mensaje(negocioA, "wamid.1", TipoMensajeWhatsapp.TEXTO, "Hola", null));

        assertThatThrownBy(() -> mensajes.saveAndFlush(
                mensaje(negocioA, "wamid.1", TipoMensajeWhatsapp.TEXTO, "Hola", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una nota de voz sin id de audio no se guarda")
    void shouldRejectAudioWithoutMediaId() {
        assertThatThrownBy(() -> mensajes.saveAndFlush(
                mensaje(negocioA, "wamid.1", TipoMensajeWhatsapp.AUDIO, "texto", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Un mensaje de un negocio que no existe no se guarda")
    void shouldRejectMensajeOfMissingNegocio() {
        assertThatThrownBy(() -> mensajes.saveAndFlush(
                mensaje(UUID.randomUUID(), "wamid.1", TipoMensajeWhatsapp.TEXTO, "Hola", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---
    private VinculoWhatsappEntity vinculado(UUID negocioId, String telefono) {
        return VinculoWhatsappEntity.builder()
                .negocioId(negocioId)
                .telefono(telefono)
                .vinculadoEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    private VinculoWhatsappEntity conCodigo(UUID negocioId, String codigo) {
        return VinculoWhatsappEntity.builder()
                .negocioId(negocioId)
                .codigo(codigo)
                .codigoVenceEn(AHORA.plusMinutes(10))
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    private MensajeWhatsappEntity mensaje(UUID negocioId, String waMensajeId, TipoMensajeWhatsapp tipo,
                                          String texto, String mediaId) {
        return MensajeWhatsappEntity.builder()
                .negocioId(negocioId)
                .telefono(TELEFONO)
                .waMensajeId(waMensajeId)
                .tipo(tipo)
                .texto(texto)
                .mediaId(mediaId)
                .estado(EstadoMensajeWhatsapp.PENDIENTE)
                .recibidoEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }
}

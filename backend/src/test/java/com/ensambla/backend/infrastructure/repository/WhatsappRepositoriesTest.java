package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacionId;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsappId;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsappId;
import com.ensambla.backend.infrastructure.repository.entity.CodigoVinculacionEntity;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.CodigoVinculacionRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.MensajeWhatsappRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.VinculoWhatsappRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICodigoVinculacionJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsappRepositoriesTest {

    private static final UUID ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final WhatsappRepositoryMapper mapper = new WhatsappRepositoryMapper();
    private final IVinculoWhatsappJpaRepository vinculoJpa = mock(IVinculoWhatsappJpaRepository.class);
    private final ICodigoVinculacionJpaRepository codigoJpa = mock(ICodigoVinculacionJpaRepository.class);
    private final IMensajeWhatsappJpaRepository mensajeJpa = mock(IMensajeWhatsappJpaRepository.class);
    private final VinculoWhatsappRepositoryImpl vinculos = new VinculoWhatsappRepositoryImpl(vinculoJpa, mapper);
    private final CodigoVinculacionRepositoryImpl codigos = new CodigoVinculacionRepositoryImpl(codigoJpa, mapper);
    private final MensajeWhatsappRepositoryImpl mensajes = new MensajeWhatsappRepositoryImpl(mensajeJpa, mapper);

    @Test
    @DisplayName("El mapper pasa el vínculo de la entidad al dominio y de vuelta sin perder datos")
    void mapperShouldRoundTripVinculo() {
        VinculoWhatsapp dominio = givenVinculo();

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("El mapper pasa el código de la entidad al dominio y de vuelta sin perder datos")
    void mapperShouldRoundTripCodigo() {
        CodigoVinculacion dominio = CodigoVinculacion.builder().id(CodigoVinculacionId.de(ID)).usuarioId(USUARIO_ID).codigo("ABC234")
                .expiraEn(AHORA.plusMinutes(10)).usadoEn(AHORA).build();

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("El mapper pasa el mensaje de la entidad al dominio y de vuelta sin perder datos")
    void mapperShouldRoundTripMensaje() {
        MensajeWhatsapp dominio = MensajeWhatsapp.builder().id(MensajeWhatsappId.de(ID)).negocioId(NEGOCIO_ID).telefono("5491122334455")
                .idExterno("ID1").tipo(TipoMensajeWhatsapp.AUDIO).mimeMedia("audio/ogg").duracionSeg(7)
                .estado(EstadoMensajeWhatsapp.RECIBIDO).recibidoEn(AHORA).build();

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("Guarda y busca el vínculo por usuario y por teléfono")
    void vinculoRepositoryShouldSaveAndFind() {
        VinculoWhatsappEntity entity = mapper.toEntity(givenVinculo());
        when(vinculoJpa.saveAndFlush(any(VinculoWhatsappEntity.class))).thenReturn(entity);
        when(vinculoJpa.findByUsuarioId(USUARIO_ID.valor())).thenReturn(Optional.of(entity));
        when(vinculoJpa.findByTelefono("5491122334455")).thenReturn(Optional.of(entity));

        assertThat(vinculos.save(givenVinculo()).getId().valor()).isEqualTo(ID);
        assertThat(vinculos.findByUsuarioId(USUARIO_ID)).map(v -> v.getId().valor()).contains(ID);
        assertThat(vinculos.findByTelefono("5491122334455")).map(v -> v.getId().valor()).contains(ID);
    }

    @Test
    @DisplayName("Borra el vínculo del usuario")
    void vinculoRepositoryShouldDeleteByUsuario() {
        vinculos.deleteByUsuarioId(USUARIO_ID);

        verify(vinculoJpa).deleteByUsuarioId(USUARIO_ID.valor());
    }

    @Test
    @DisplayName("Guarda y busca el código y descarta los que no se usaron")
    void codigoRepositoryShouldSaveFindAndDelete() {
        CodigoVinculacionEntity entity = CodigoVinculacionEntity.builder().id(ID).usuarioId(USUARIO_ID.valor())
                .codigo("ABC234").expiraEn(AHORA).build();
        when(codigoJpa.saveAndFlush(any(CodigoVinculacionEntity.class))).thenReturn(entity);
        when(codigoJpa.findByCodigo("ABC234")).thenReturn(Optional.of(entity));
        when(codigoJpa.existsByCodigo("ABC234")).thenReturn(true);

        assertThat(codigos.save(CodigoVinculacion.builder().usuarioId(USUARIO_ID).codigo("ABC234").build())
                .getId().valor()).isEqualTo(ID);
        assertThat(codigos.findByCodigo("ABC234")).map(c -> c.getId().valor()).contains(ID);
        assertThat(codigos.existsByCodigo("ABC234")).isTrue();
        codigos.deleteSinUsarByUsuarioId(USUARIO_ID);
        verify(codigoJpa).deleteSinUsarByUsuarioId(USUARIO_ID.valor());
    }

    @Test
    @DisplayName("Guarda el mensaje y consulta si ya existe por su id externo")
    void mensajeRepositoryShouldSaveAndCheckDuplicates() {
        MensajeWhatsappEntity entity = mapper.toEntity(MensajeWhatsapp.builder().id(MensajeWhatsappId.de(ID)).negocioId(NEGOCIO_ID)
                .idExterno("ID1").tipo(TipoMensajeWhatsapp.TEXTO).texto("hola").recibidoEn(AHORA).build());
        when(mensajeJpa.saveAndFlush(any(MensajeWhatsappEntity.class))).thenReturn(entity);
        when(mensajeJpa.existsByIdExterno("ID1")).thenReturn(true);

        assertThat(mensajes.save(MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).idExterno("ID1").build()).getId().valor()).isEqualTo(ID);
        assertThat(mensajes.existsByIdExterno("ID1")).isTrue();
    }

    private static VinculoWhatsapp givenVinculo() {
        return VinculoWhatsapp.builder().id(VinculoWhatsappId.de(ID)).usuarioId(USUARIO_ID).negocioId(NEGOCIO_ID)
                .telefono("5491122334455").vinculadoEn(AHORA).build();
    }
}

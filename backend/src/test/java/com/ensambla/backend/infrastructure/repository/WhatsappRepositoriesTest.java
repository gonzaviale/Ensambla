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
    private static final String TELEFONO = "5491122334455";

    private final WhatsappRepositoryMapper mapper = new WhatsappRepositoryMapper();
    private final IVinculoWhatsappJpaRepository vinculoJpa = mock(IVinculoWhatsappJpaRepository.class);
    private final ICodigoVinculacionJpaRepository codigoJpa = mock(ICodigoVinculacionJpaRepository.class);
    private final IMensajeWhatsappJpaRepository mensajeJpa = mock(IMensajeWhatsappJpaRepository.class);
    private final VinculoWhatsappRepositoryImpl vinculos = new VinculoWhatsappRepositoryImpl(vinculoJpa, mapper);
    private final CodigoVinculacionRepositoryImpl codigos = new CodigoVinculacionRepositoryImpl(codigoJpa, mapper);
    private final MensajeWhatsappRepositoryImpl mensajes = new MensajeWhatsappRepositoryImpl(mensajeJpa, mapper);

    private VinculoWhatsapp vinculoGuardado;
    private Optional<VinculoWhatsapp> vinculoEncontrado;
    private CodigoVinculacion codigoGuardado;
    private Optional<CodigoVinculacion> codigoEncontrado;
    private boolean codigoExiste;
    private MensajeWhatsapp mensajeGuardado;
    private boolean mensajeExiste;

    @Test
    @DisplayName("El mapper pasa el vínculo de la entidad al dominio y de vuelta sin perder datos")
    void elMapperPasaElVinculoDeLaEntidadAlDominioYDeVueltaSinPerderDatos() {
        entoncesElVinculoIdaYVueltaNoPierdeDatos(unVinculo());
    }

    @Test
    @DisplayName("El mapper pasa el código de la entidad al dominio y de vuelta sin perder datos")
    void elMapperPasaElCodigoDeLaEntidadAlDominioYDeVueltaSinPerderDatos() {
        entoncesElCodigoIdaYVueltaNoPierdeDatos(unCodigoUsado());
    }

    @Test
    @DisplayName("El mapper pasa el mensaje de la entidad al dominio y de vuelta sin perder datos")
    void elMapperPasaElMensajeDeLaEntidadAlDominioYDeVueltaSinPerderDatos() {
        entoncesElMensajeIdaYVueltaNoPierdeDatos(unMensajeDeAudio());
    }

    @Test
    @DisplayName("Guarda el vínculo")
    void guardaElVinculo() {
        dadoUnVinculoPersistido();

        cuandoSeGuardaElVinculo();

        entoncesElVinculoGuardadoTieneElId();
    }

    @Test
    @DisplayName("Busca el vínculo por usuario")
    void buscaElVinculoPorUsuario() {
        dadoUnVinculoPersistido();

        cuandoSeBuscaElVinculoPorUsuario();

        entoncesSeEncuentraElVinculo();
    }

    @Test
    @DisplayName("Busca el vínculo por teléfono")
    void buscaElVinculoPorTelefono() {
        dadoUnVinculoPersistido();

        cuandoSeBuscaElVinculoPorTelefono();

        entoncesSeEncuentraElVinculo();
    }

    @Test
    @DisplayName("Borra el vínculo del usuario")
    void borraElVinculoDelUsuario() {
        cuandoSeBorraElVinculoDelUsuario();

        entoncesSeBorraElVinculoEnElRepositorioJpa();
    }

    @Test
    @DisplayName("Guarda el código")
    void guardaElCodigo() {
        dadoUnCodigoPersistido();

        cuandoSeGuardaElCodigo();

        entoncesElCodigoGuardadoTieneElId();
    }

    @Test
    @DisplayName("Busca el código y consulta si ya existe")
    void buscaElCodigoYConsultaSiYaExiste() {
        dadoUnCodigoPersistido();

        cuandoSeBuscaElCodigoYSeConsultaSiExiste();

        entoncesSeEncuentraElCodigoYExiste();
    }

    @Test
    @DisplayName("Descarta los códigos que no se usaron")
    void descartaLosCodigosQueNoSeUsaron() {
        cuandoSeDescartanLosCodigosSinUsarDelUsuario();

        entoncesSeDescartanEnElRepositorioJpa();
    }

    @Test
    @DisplayName("Guarda el mensaje")
    void guardaElMensaje() {
        dadoUnMensajePersistido();

        cuandoSeGuardaElMensaje();

        entoncesElMensajeGuardadoTieneElId();
    }

    @Test
    @DisplayName("Consulta si el mensaje ya existe por su id externo")
    void consultaSiElMensajeYaExistePorSuIdExterno() {
        dadoUnMensajePersistido();

        cuandoSeConsultaSiExisteElMensaje();

        entoncesElMensajeYaExiste();
    }

    // --- dado ---

    private void dadoUnVinculoPersistido() {
        VinculoWhatsappEntity entidad = mapper.toEntity(unVinculo());
        when(vinculoJpa.saveAndFlush(any(VinculoWhatsappEntity.class))).thenReturn(entidad);
        when(vinculoJpa.findByUsuarioId(USUARIO_ID.valor())).thenReturn(Optional.of(entidad));
        when(vinculoJpa.findByTelefono(TELEFONO)).thenReturn(Optional.of(entidad));
    }

    private void dadoUnCodigoPersistido() {
        CodigoVinculacionEntity entidad = CodigoVinculacionEntity.builder().id(ID).usuarioId(USUARIO_ID.valor())
                .codigo("ABC234").expiraEn(AHORA).build();
        when(codigoJpa.saveAndFlush(any(CodigoVinculacionEntity.class))).thenReturn(entidad);
        when(codigoJpa.findByCodigo("ABC234")).thenReturn(Optional.of(entidad));
        when(codigoJpa.existsByCodigo("ABC234")).thenReturn(true);
    }

    private void dadoUnMensajePersistido() {
        MensajeWhatsappEntity entidad = mapper.toEntity(MensajeWhatsapp.builder().id(MensajeWhatsappId.de(ID))
                .negocioId(NEGOCIO_ID).idExterno("ID1").tipo(TipoMensajeWhatsapp.TEXTO).texto("hola")
                .recibidoEn(AHORA).build());
        when(mensajeJpa.saveAndFlush(any(MensajeWhatsappEntity.class))).thenReturn(entidad);
        when(mensajeJpa.existsByIdExterno("ID1")).thenReturn(true);
    }

    // --- cuando ---

    private void cuandoSeGuardaElVinculo() {
        vinculoGuardado = vinculos.save(unVinculo());
    }

    private void cuandoSeBuscaElVinculoPorUsuario() {
        vinculoEncontrado = vinculos.findByUsuarioId(USUARIO_ID);
    }

    private void cuandoSeBuscaElVinculoPorTelefono() {
        vinculoEncontrado = vinculos.findByTelefono(TELEFONO);
    }

    private void cuandoSeBorraElVinculoDelUsuario() {
        vinculos.deleteByUsuarioId(USUARIO_ID);
    }

    private void cuandoSeGuardaElCodigo() {
        codigoGuardado = codigos.save(CodigoVinculacion.builder().usuarioId(USUARIO_ID).codigo("ABC234").build());
    }

    private void cuandoSeBuscaElCodigoYSeConsultaSiExiste() {
        codigoEncontrado = codigos.findByCodigo("ABC234");
        codigoExiste = codigos.existsByCodigo("ABC234");
    }

    private void cuandoSeDescartanLosCodigosSinUsarDelUsuario() {
        codigos.deleteSinUsarByUsuarioId(USUARIO_ID);
    }

    private void cuandoSeGuardaElMensaje() {
        mensajeGuardado = mensajes.save(MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).idExterno("ID1").build());
    }

    private void cuandoSeConsultaSiExisteElMensaje() {
        mensajeExiste = mensajes.existsByIdExterno("ID1");
    }

    // --- entonces ---

    private void entoncesElVinculoIdaYVueltaNoPierdeDatos(VinculoWhatsapp dominio) {
        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    private void entoncesElCodigoIdaYVueltaNoPierdeDatos(CodigoVinculacion dominio) {
        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    private void entoncesElMensajeIdaYVueltaNoPierdeDatos(MensajeWhatsapp dominio) {
        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    private void entoncesElVinculoGuardadoTieneElId() {
        assertThat(vinculoGuardado.getId().valor()).isEqualTo(ID);
    }

    private void entoncesSeEncuentraElVinculo() {
        assertThat(vinculoEncontrado).map(vinculo -> vinculo.getId().valor()).contains(ID);
    }

    private void entoncesSeBorraElVinculoEnElRepositorioJpa() {
        verify(vinculoJpa).deleteByUsuarioId(USUARIO_ID.valor());
    }

    private void entoncesElCodigoGuardadoTieneElId() {
        assertThat(codigoGuardado.getId().valor()).isEqualTo(ID);
    }

    private void entoncesSeEncuentraElCodigoYExiste() {
        assertThat(codigoEncontrado).map(codigo -> codigo.getId().valor()).contains(ID);
        assertThat(codigoExiste).isTrue();
    }

    private void entoncesSeDescartanEnElRepositorioJpa() {
        verify(codigoJpa).deleteSinUsarByUsuarioId(USUARIO_ID.valor());
    }

    private void entoncesElMensajeGuardadoTieneElId() {
        assertThat(mensajeGuardado.getId().valor()).isEqualTo(ID);
    }

    private void entoncesElMensajeYaExiste() {
        assertThat(mensajeExiste).isTrue();
    }

    // --- datos ---

    private static VinculoWhatsapp unVinculo() {
        return VinculoWhatsapp.builder().id(VinculoWhatsappId.de(ID)).usuarioId(USUARIO_ID).negocioId(NEGOCIO_ID)
                .telefono(TELEFONO).vinculadoEn(AHORA).build();
    }

    private static CodigoVinculacion unCodigoUsado() {
        return CodigoVinculacion.builder().id(CodigoVinculacionId.de(ID)).usuarioId(USUARIO_ID).codigo("ABC234")
                .expiraEn(AHORA.plusMinutes(10)).usadoEn(AHORA).build();
    }

    private static MensajeWhatsapp unMensajeDeAudio() {
        return MensajeWhatsapp.builder().id(MensajeWhatsappId.de(ID)).negocioId(NEGOCIO_ID).telefono(TELEFONO)
                .idExterno("ID1").tipo(TipoMensajeWhatsapp.AUDIO).mimeMedia("audio/ogg").duracionSeg(7)
                .estado(EstadoMensajeWhatsapp.RECIBIDO).recibidoEn(AHORA).build();
    }
}

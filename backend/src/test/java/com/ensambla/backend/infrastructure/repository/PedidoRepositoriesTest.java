package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.pedido.MensajeClienteId;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.ClienteWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.MensajeClienteEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.ClienteWhatsappRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.MensajeClienteRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IClienteWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeClienteJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PedidoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PedidoRepositoriesTest {

    private static final UUID ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final String TELEFONO = "5491199990000";

    private final PedidoRepositoryMapper mapper = new PedidoRepositoryMapper();
    private final IClienteWhatsappJpaRepository clienteJpa = mock(IClienteWhatsappJpaRepository.class);
    private final IMensajeClienteJpaRepository mensajeJpa = mock(IMensajeClienteJpaRepository.class);
    private final ClienteWhatsappRepositoryImpl clientes = new ClienteWhatsappRepositoryImpl(clienteJpa, mapper);
    private final MensajeClienteRepositoryImpl mensajes = new MensajeClienteRepositoryImpl(mensajeJpa, mapper);

    @Test
    @DisplayName("El mapper pasa el cliente de la entidad al dominio y de vuelta sin perder datos")
    void mapperShouldRoundTripCliente() {
        ClienteWhatsapp dominio = givenCliente();

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("El mapper acepta un cliente que todavía no tiene id ni último comercio")
    void mapperShouldMapNewCliente() {
        ClienteWhatsappEntity entity = mapper.toEntity(ClienteWhatsapp.nuevo(TELEFONO, AHORA));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getUltimoNegocioId()).isNull();
        assertThat(mapper.toDomain(entity).getUltimoNegocioId()).isNull();
    }

    @Test
    @DisplayName("El mapper pasa el mensaje de la entidad al dominio y de vuelta sin perder datos")
    void mapperShouldRoundTripMensaje() {
        MensajeCliente dominio = givenMensaje();

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("Guarda el cliente y lo busca por su teléfono")
    void clienteRepositoryShouldSaveAndFind() {
        ClienteWhatsappEntity entity = mapper.toEntity(givenCliente());
        when(clienteJpa.saveAndFlush(any(ClienteWhatsappEntity.class))).thenReturn(entity);
        when(clienteJpa.findByTelefono(TELEFONO)).thenReturn(Optional.of(entity));
        when(clienteJpa.findByTelefono("5491100000000")).thenReturn(Optional.empty());

        assertThat(clientes.save(ClienteWhatsapp.nuevo(TELEFONO, AHORA)).getId().valor()).isEqualTo(ID);
        assertThat(clientes.findByTelefono(TELEFONO)).map(c -> c.getId().valor()).contains(ID);
        assertThat(clientes.findByTelefono("5491100000000")).isEmpty();
    }

    @Test
    @DisplayName("Trae a los clientes con esos ids")
    void clienteRepositoryShouldFindManyByIds() {
        ClienteWhatsappEntity entity = mapper.toEntity(givenCliente());
        when(clienteJpa.findAllById(List.of(ID))).thenReturn(List.of(entity));

        assertThat(clientes.findAllByIds(List.of(ClienteId.de(ID)))).extracting(c -> c.getId().valor())
                .containsExactly(ID);
    }

    @Test
    @DisplayName("Guarda el mensaje y consulta si ya existe por su id externo")
    void mensajeRepositoryShouldSaveAndCheckDuplicates() {
        MensajeClienteEntity entity = mapper.toEntity(givenMensaje());
        when(mensajeJpa.saveAndFlush(any(MensajeClienteEntity.class))).thenReturn(entity);
        when(mensajeJpa.existsByIdExterno("ID1")).thenReturn(true);

        assertThat(mensajes.save(givenMensaje()).getId().valor()).isEqualTo(ID);
        assertThat(mensajes.existsByIdExterno("ID1")).isTrue();
        assertThat(mensajes.existsByIdExterno("ID2")).isFalse();
    }

    @Test
    @DisplayName("Reclama el mensaje más viejo, lo deja atendiéndose y no devuelve nada si la cola está vacía")
    void mensajeRepositoryShouldClaimNext() {
        MensajeClienteEntity entity = mapper.toEntity(givenMensaje());
        when(mensajeJpa.findSiguienteRecibidoBloqueado()).thenReturn(Optional.of(entity)).thenReturn(Optional.empty());
        when(mensajeJpa.saveAndFlush(any(MensajeClienteEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Optional<MensajeCliente> reclamado = mensajes.reclamarSiguiente(AHORA, Duration.ofMinutes(2));

        assertThat(reclamado).isPresent();
        assertThat(reclamado.get().getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIENDO);
        assertThat(reclamado.get().getReclamadoHasta()).isEqualTo(AHORA.plusMinutes(2));
        assertThat(mensajes.reclamarSiguiente(AHORA, Duration.ofMinutes(2))).isEmpty();
    }

    @Test
    @DisplayName("Lista los mensajes reclamados cuyo plazo ya pasó")
    void mensajeRepositoryShouldListExpired() {
        MensajeClienteEntity entity = mapper.toEntity(givenMensaje());
        when(mensajeJpa.findByEstadoAndReclamadoHastaBefore(EstadoMensajeWhatsapp.ATENDIENDO, AHORA))
                .thenReturn(List.of(entity));

        assertThat(mensajes.vencidos(AHORA)).extracting(m -> m.getId().valor()).containsExactly(ID);
    }

    private static ClienteWhatsapp givenCliente() {
        return ClienteWhatsapp.builder().id(ClienteId.de(ID)).telefono(TELEFONO).ultimoNegocioId(NEGOCIO_ID)
                .creadoEn(AHORA).build();
    }

    private static MensajeCliente givenMensaje() {
        return MensajeCliente.builder().id(MensajeClienteId.de(ID)).telefono(TELEFONO).idExterno("ID1").texto("hola")
                .estado(EstadoMensajeWhatsapp.RECIBIDO).recibidoEn(AHORA).build();
    }
}

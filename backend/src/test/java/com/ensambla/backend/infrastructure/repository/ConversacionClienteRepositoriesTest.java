package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.Comercio;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.model.pedido.ConversacionClienteId;
import com.ensambla.backend.domain.model.pedido.EstadoConversacionCliente;
import com.ensambla.backend.domain.model.pedido.LineaDeCompra;
import com.ensambla.backend.domain.model.pedido.ProductoEnStock;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.ConversacionClienteEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.ComercioRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.ConversacionClienteRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IConversacionClienteJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ConversacionClienteRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConversacionClienteRepositoriesTest {

    private static final UUID ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final ClienteId CLIENTE_ID = ClienteId.de("c0000000-0000-4000-8000-000000000001");
    private static final NegocioId PEPE = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId ROSA = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");

    private final ConversacionClienteRepositoryMapper mapper =
            new ConversacionClienteRepositoryMapper(JsonMapper.builder().build());
    private final IConversacionClienteJpaRepository conversacionJpa = mock(IConversacionClienteJpaRepository.class);
    private final INegocioJpaRepository negocioJpa = mock(INegocioJpaRepository.class);
    private final ConversacionClienteRepositoryImpl conversaciones =
            new ConversacionClienteRepositoryImpl(conversacionJpa, mapper);
    private final ComercioRepositoryImpl comercios = new ComercioRepositoryImpl(negocioJpa);

    @Test
    @DisplayName("El mapper pasa la conversación al dominio y de vuelta sin perder datos, con sus opciones")
    void mapperShouldRoundTripConversacion() {
        ConversacionCliente dominio = givenConversacion(PEPE, List.of(PEPE, ROSA));

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("El mapper acepta una conversación nueva, sin id, sin comercio y sin opciones")
    void mapperShouldMapNewConversacion() {
        ConversacionClienteEntity entity = mapper.toEntity(ConversacionCliente.nueva(CLIENTE_ID, AHORA));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getNegocioId()).isNull();
        assertThat(entity.getOpciones()).isEqualTo("[]");
        assertThat(mapper.toDomain(entity).getOpciones()).isEmpty();
    }

    @Test
    @DisplayName("Guarda la conversación y la busca por cliente")
    void conversacionRepositoryShouldSaveAndFind() {
        ConversacionClienteEntity entity = mapper.toEntity(givenConversacion(PEPE, List.of()));
        when(conversacionJpa.saveAndFlush(any(ConversacionClienteEntity.class))).thenReturn(entity);
        when(conversacionJpa.findByClienteId(CLIENTE_ID.valor())).thenReturn(Optional.of(entity));
        when(conversacionJpa.findByClienteId(UUID.fromString("c0000000-0000-4000-8000-000000000002")))
                .thenReturn(Optional.empty());

        assertThat(conversaciones.save(givenConversacion(PEPE, List.of())).getId().valor()).isEqualTo(ID);
        assertThat(conversaciones.findByClienteId(CLIENTE_ID)).map(c -> c.getId().valor()).contains(ID);
        assertThat(conversaciones.findByClienteId(ClienteId.de("c0000000-0000-4000-8000-000000000002"))).isEmpty();
    }

    @Test
    @DisplayName("Los comercios son los negocios con WhatsApp vinculado, con su id y su nombre")
    void comercioRepositoryShouldListBusinessesWithLinkedWhatsapp() {
        when(negocioJpa.findConWhatsappVinculado()).thenReturn(List.of(
                NegocioEntity.builder().id(PEPE.valor()).nombre("Almacén Don Pepe").build(),
                NegocioEntity.builder().id(ROSA.valor()).nombre("Almacén Doña Rosa").build()));

        assertThat(comercios.listarConWhatsapp()).containsExactly(
                new Comercio(PEPE, "Almacén Don Pepe"), new Comercio(ROSA, "Almacén Doña Rosa"));
    }

    @Test
    @DisplayName("El mapper guarda la lista de compra con sus productos, precios y alternativas y la lee igual")
    void mapperShouldRoundTripTheShoppingList() {
        ProductoEnStock yerba = new ProductoEnStock(RegistroId.de("d0000000-0000-4000-8000-000000000001"),
                "Yerba Playadito 1 kg", new BigDecimal("3500"), new BigDecimal("10"));
        ProductoEnStock taragui = new ProductoEnStock(RegistroId.de("d0000000-0000-4000-8000-000000000002"),
                "Yerba Taragüí 1 kg", new BigDecimal("3800"), new BigDecimal("6"));
        ConversacionCliente dominio = givenConversacion(PEPE, List.of()).toBuilder()
                .estado(EstadoConversacionCliente.PIDIENDO)
                .lista(List.of(LineaDeCompra.de(yerba, new BigDecimal("2"), List.of(taragui)),
                        LineaDeCompra.de(taragui, new BigDecimal("9"), List.of())))
                .build();

        ConversacionCliente leida = mapper.toDomain(mapper.toEntity(dominio));

        assertThat(leida).usingRecursiveComparison().isEqualTo(dominio);
        assertThat(leida.getLista().get(1).limitadaPorStock()).isTrue();
        assertThat(mapper.toEntity(ConversacionCliente.nueva(CLIENTE_ID, AHORA)).getLista()).isEqualTo("[]");
    }

    private static ConversacionCliente givenConversacion(NegocioId negocioId, List<NegocioId> opciones) {
        return ConversacionCliente.builder().id(ConversacionClienteId.de(ID)).clienteId(CLIENTE_ID)
                .estado(EstadoConversacionCliente.ELIGIENDO_COMERCIO).negocioId(negocioId).opciones(opciones)
                .actualizadoEn(AHORA).build();
    }
}

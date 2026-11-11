package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.ItemDeOrden;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.OrdenCompraEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.implementation.OrdenCompraRepositoryImpl;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IOrdenCompraJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.OrdenCompraRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrdenCompraRepositoriesTest {

    private static final UUID ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");

    private final OrdenCompraRepositoryMapper mapper = new OrdenCompraRepositoryMapper(JsonMapper.builder().build());
    private final IOrdenCompraJpaRepository jpa = mock(IOrdenCompraJpaRepository.class);
    private final OrdenCompraRepositoryImpl ordenes = new OrdenCompraRepositoryImpl(jpa, mapper);

    @Test
    @DisplayName("El mapper pasa la orden al dominio y de vuelta sin perder datos, con sus ítems")
    void mapperShouldRoundTripTheOrder() {
        OrdenCompra dominio = givenOrden();

        assertThat(mapper.toDomain(mapper.toEntity(dominio))).usingRecursiveComparison().isEqualTo(dominio);
    }

    @Test
    @DisplayName("El mapper conserva el estado y el motivo de rechazo")
    void mapperShouldKeepTheStateAndTheRejectionReason() {
        OrdenCompra rechazada = givenOrden().toBuilder().estado(EstadoOrdenCompra.RECHAZADA)
                .motivoRechazo("No me queda stock").build();

        OrdenCompra leida = mapper.toDomain(mapper.toEntity(rechazada));

        assertThat(leida.getEstado()).isEqualTo(EstadoOrdenCompra.RECHAZADA);
        assertThat(leida.getMotivoRechazo()).isEqualTo("No me queda stock");
    }

    @Test
    @DisplayName("Guarda la orden y devuelve la guardada con su id")
    void repositoryShouldSaveTheOrder() {
        OrdenCompraEntity entity = mapper.toEntity(givenOrden());
        when(jpa.saveAndFlush(any(OrdenCompraEntity.class))).thenReturn(entity);

        OrdenCompra guardada = ordenes.save(givenOrden().toBuilder().id(null).build());

        assertThat(guardada.getId().valor()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Lista las órdenes del negocio, todas o sólo las de un estado")
    void repositoryShouldListByBusinessAndState() {
        OrdenCompraEntity entity = mapper.toEntity(givenOrden());
        when(jpa.findTop100ByNegocioIdOrderByCreadoEnDescIdDesc(NEGOCIO_ID.valor())).thenReturn(List.of(entity));
        when(jpa.findTop100ByNegocioIdAndEstadoOrderByCreadoEnDescIdDesc(NEGOCIO_ID.valor(),
                EstadoOrdenCompra.LISTA)).thenReturn(List.of());

        assertThat(ordenes.findAllByNegocioId(NEGOCIO_ID)).extracting(o -> o.getId().valor()).containsExactly(ID);
        assertThat(ordenes.findAllByNegocioIdAndEstado(NEGOCIO_ID, EstadoOrdenCompra.LISTA)).isEmpty();
    }

    @Test
    @DisplayName("Busca la orden por id dentro del negocio y una ajena no se encuentra")
    void repositoryShouldFindByIdWithinTheBusiness() {
        OrdenCompraEntity entity = mapper.toEntity(givenOrden());
        when(jpa.findByIdAndNegocioId(ID, NEGOCIO_ID.valor())).thenReturn(Optional.of(entity));
        when(jpa.findByIdAndNegocioId(ID, OTRO_NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        assertThat(ordenes.findByIdAndNegocioId(OrdenCompraId.de(ID), NEGOCIO_ID)).isPresent();
        assertThat(ordenes.findByIdAndNegocioId(OrdenCompraId.de(ID), OTRO_NEGOCIO_ID)).isEmpty();
        assertThatThrownBy(() -> ordenes.obtener(OTRO_NEGOCIO_ID, OrdenCompraId.de(ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(ordenes.obtener(NEGOCIO_ID, OrdenCompraId.de(ID)).getId().valor()).isEqualTo(ID);
    }

    private static OrdenCompra givenOrden() {
        return OrdenCompra.builder().id(OrdenCompraId.de(ID))
                .negocioId(NEGOCIO_ID)
                .clienteId(ClienteId.de("c0000000-0000-4000-8000-000000000001"))
                .estado(EstadoOrdenCompra.PENDIENTE)
                .items(List.of(
                        new ItemDeOrden(RegistroId.de("d0000000-0000-4000-8000-000000000001"),
                                "Yerba Playadito 1 kg", new BigDecimal("2"), new BigDecimal("3500")),
                        new ItemDeOrden(RegistroId.de("d0000000-0000-4000-8000-000000000002"),
                                "Aceite Cocinero 900 ml", new BigDecimal("1"), new BigDecimal("2000"))))
                .total(new BigDecimal("9000")).creadoEn(AHORA).actualizadoEn(AHORA).build();
    }
}

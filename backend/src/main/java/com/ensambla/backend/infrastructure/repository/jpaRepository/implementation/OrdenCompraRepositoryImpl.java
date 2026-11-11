package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IOrdenCompraJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.OrdenCompraRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrdenCompraRepositoryImpl implements OrdenCompraRepository {

    private final IOrdenCompraJpaRepository jpa;
    private final OrdenCompraRepositoryMapper mapper;

    @Override
    public OrdenCompra save(OrdenCompra orden) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(orden)));
    }

    @Override
    public List<OrdenCompra> findAllByNegocioId(NegocioId negocioId) {
        return jpa.findTop100ByNegocioIdOrderByCreadoEnDescIdDesc(negocioId.valor()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OrdenCompra> findAllByNegocioIdAndEstado(NegocioId negocioId, EstadoOrdenCompra estado) {
        return jpa.findTop100ByNegocioIdAndEstadoOrderByCreadoEnDescIdDesc(negocioId.valor(), estado).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<OrdenCompra> findByIdAndNegocioId(OrdenCompraId ordenId, NegocioId negocioId) {
        return jpa.findByIdAndNegocioId(ordenId.valor(), negocioId.valor()).map(mapper::toDomain);
    }
}

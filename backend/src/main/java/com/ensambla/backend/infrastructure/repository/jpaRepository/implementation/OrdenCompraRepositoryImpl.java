package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IOrdenCompraJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.OrdenCompraRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrdenCompraRepositoryImpl implements OrdenCompraRepository {

    private final IOrdenCompraJpaRepository jpa;
    private final OrdenCompraRepositoryMapper mapper;

    @Override
    public OrdenCompra save(OrdenCompra orden) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(orden)));
    }
}

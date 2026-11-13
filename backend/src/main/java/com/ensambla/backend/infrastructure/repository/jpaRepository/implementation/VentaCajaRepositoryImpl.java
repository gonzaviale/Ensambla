package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVentaCajaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.VentaCajaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VentaCajaRepositoryImpl implements VentaCajaRepository {

    private final IVentaCajaJpaRepository ventaCajaJpaRepository;
    private final VentaCajaRepositoryMapper ventaCajaRepositoryMapper;

    @Override
    public Optional<VentaCaja> findByIdAndNegocioId(VentaCajaId id, NegocioId negocioId) {
        return ventaCajaJpaRepository.findByIdAndNegocioId(id.valor(), negocioId.valor())
                .map(ventaCajaRepositoryMapper::toDomain);
    }

    @Override
    public VentaCaja save(VentaCaja venta) {
        return ventaCajaRepositoryMapper.toDomain(ventaCajaJpaRepository.save(
                ventaCajaRepositoryMapper.toEntity(venta)));
    }
}

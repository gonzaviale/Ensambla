package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRenovacionPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RenovacionPremiumRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RenovacionPremiumRepositoryImpl implements RenovacionPremiumRepository {

    private final IRenovacionPremiumJpaRepository renovacionPremiumJpaRepository;

    private final RenovacionPremiumRepositoryMapper renovacionPremiumRepositoryMapper;

    @Override
    public RenovacionPremium save(RenovacionPremium renovacion) {
        return renovacionPremiumRepositoryMapper.toDomain(renovacionPremiumJpaRepository.saveAndFlush(
                renovacionPremiumRepositoryMapper.toEntity(renovacion)));
    }

    @Override
    public Optional<RenovacionPremium> findById(UUID id) {
        return renovacionPremiumJpaRepository.findById(id).map(renovacionPremiumRepositoryMapper::toDomain);
    }

    @Override
    public Optional<UUID> findNegocioIdById(UUID id) {
        return renovacionPremiumJpaRepository.findNegocioIdById(id);
    }

    @Override
    public Optional<RenovacionPremium> findByProveedorId(String proveedorId) {
        return renovacionPremiumJpaRepository.findByProveedorId(proveedorId)
                .map(renovacionPremiumRepositoryMapper::toDomain);
    }

    @Override
    public Optional<RenovacionPremium> findUltimaByNegocioId(UUID negocioId) {
        return renovacionPremiumJpaRepository.findActualByNegocioId(negocioId)
                .map(renovacionPremiumRepositoryMapper::toDomain);
    }

    @Override
    public List<UUID> listarParaSincronizar() {
        return renovacionPremiumJpaRepository.listarParaSincronizar();
    }

    @Override
    public boolean existeAbierta(UUID negocioId) {
        return renovacionPremiumJpaRepository.existsByNegocioIdAndEstadoNot(negocioId, "CANCELADA");
    }
}

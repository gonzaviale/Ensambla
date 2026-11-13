package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVentaCajaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.VentaCajaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.List;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class VentaCajaRepositoryImpl implements VentaCajaRepository {

    private final IVentaCajaJpaRepository ventaCajaJpaRepository;
    private final VentaCajaRepositoryMapper ventaCajaRepositoryMapper;

    @Override
    public List<VentaCaja> listarAbiertas(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId) {
        return ventaCajaJpaRepository
                .findByNegocioIdAndProyectoIdAndInventarioIdAndCierreIdIsNullOrderByCreadoEnAscIdAsc(
                        negocioId.valor(), proyectoId.valor(), inventarioId.valor())
                .stream().map(ventaCajaRepositoryMapper::toDomain).toList();
    }

    @Override
    public List<VentaCaja> listarEntre(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId,
            OffsetDateTime desde, OffsetDateTime hasta) {
        return ventaCajaJpaRepository
                .findByNegocioIdAndProyectoIdAndInventarioIdAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnAscIdAsc(
                        negocioId.valor(), proyectoId.valor(), inventarioId.valor(), desde, hasta)
                .stream().map(ventaCajaRepositoryMapper::toDomain).toList();
    }

    @Override
    public void marcarCerradas(NegocioId negocioId, CierreCajaId cierreId, List<VentaCajaId> ventas) {
        ventaCajaJpaRepository.marcarCerradas(negocioId.valor(), cierreId.valor(), Identificador.valores(ventas));
    }

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

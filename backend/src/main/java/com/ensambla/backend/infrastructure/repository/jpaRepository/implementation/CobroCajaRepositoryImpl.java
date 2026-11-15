package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICobroCajaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CobroCajaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CobroCajaRepositoryImpl implements CobroCajaRepository {

    private final ICobroCajaJpaRepository cobroCajaJpaRepository;
    private final CobroCajaRepositoryMapper cobroCajaRepositoryMapper;

    @Override
    public List<CobroCaja> listarTodos(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId) {
        return cobroCajaJpaRepository.findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnAscIdAsc(
                negocioId.valor(), proyectoId.valor(), inventarioId.valor()).stream()
                .map(cobroCajaRepositoryMapper::toDomain).toList();
    }

    @Override
    public Optional<CobroCaja> findByIdAndNegocioId(CobroCajaId id, NegocioId negocioId) {
        return cobroCajaJpaRepository.findByIdAndNegocioId(id.valor(), negocioId.valor())
                .map(cobroCajaRepositoryMapper::toDomain);
    }

    @Override
    public CobroCaja save(CobroCaja cobro) {
        return cobroCajaRepositoryMapper.toDomain(cobroCajaJpaRepository.save(cobroCajaRepositoryMapper.toEntity(cobro)));
    }

    @Override
    public List<CobroCaja> listarAbiertos(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId) {
        return cobroCajaJpaRepository
                .findByNegocioIdAndProyectoIdAndInventarioIdAndCierreIdIsNullOrderByCreadoEnAscIdAsc(
                        negocioId.valor(), proyectoId.valor(), inventarioId.valor()).stream()
                .map(cobroCajaRepositoryMapper::toDomain).toList();
    }

    @Override
    public List<CobroCaja> listarEntre(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId,
            OffsetDateTime desde, OffsetDateTime hasta) {
        return cobroCajaJpaRepository
                .findByNegocioIdAndProyectoIdAndInventarioIdAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnAscIdAsc(
                        negocioId.valor(), proyectoId.valor(), inventarioId.valor(), desde, hasta).stream()
                .map(cobroCajaRepositoryMapper::toDomain).toList();
    }

    @Override
    public void marcarCerrados(NegocioId negocioId, CierreCajaId cierreId, List<CobroCajaId> cobros) {
        cobroCajaJpaRepository.marcarCerrados(negocioId.valor(), cierreId.valor(), Identificador.valores(cobros));
    }
}

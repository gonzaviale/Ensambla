package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICierreCajaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CierreCajaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CierreCajaRepositoryImpl implements CierreCajaRepository {

    private final ICierreCajaJpaRepository cierreCajaJpaRepository;
    private final CierreCajaRepositoryMapper cierreCajaRepositoryMapper;

    @Override
    public Optional<CierreCaja> findByIdAndNegocioId(CierreCajaId id, NegocioId negocioId) {
        return cierreCajaJpaRepository.findByIdAndNegocioId(id.valor(), negocioId.valor())
                .map(cierreCajaRepositoryMapper::toDomain);
    }

    @Override
    public List<CierreCaja> listar(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId, int pagina) {
        return cierreCajaJpaRepository.findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnDescIdDesc(
                negocioId.valor(), proyectoId.valor(), inventarioId.valor(), PageRequest.of(pagina, 20))
                .stream().map(cierreCajaRepositoryMapper::toDomain).toList();
    }

    @Override
    public CierreCaja save(CierreCaja cierre) {
        return cierreCajaRepositoryMapper.toDomain(cierreCajaJpaRepository.save(
                cierreCajaRepositoryMapper.toEntity(cierre)));
    }
}

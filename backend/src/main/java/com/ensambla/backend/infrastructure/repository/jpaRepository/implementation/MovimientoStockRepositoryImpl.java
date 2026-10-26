package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMovimientoStockJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MovimientoStockRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MovimientoStockRepositoryImpl implements MovimientoStockRepository {

    private final IMovimientoStockJpaRepository jpa;

    private final MovimientoStockRepositoryMapper mapper;

    @Override
    public MovimientoStock save(MovimientoStock movimiento) {
        return mapper.toDomain(jpa.save(mapper.toEntity(movimiento)));
    }

    @Override
    public Map<RegistroId, BigDecimal> stockDe(Collection<RegistroId> registroIds, NegocioId negocioId) {
        if (registroIds.isEmpty()) {
            return Map.of();
        }
        return jpa.stockDe(negocioId.valor(), Identificador.valores(registroIds)).stream()
                .collect(Collectors.toMap(
                        stock -> RegistroId.de(stock.getRegistro()),
                        IMovimientoStockJpaRepository.StockPorRegistro::getStock));
    }

    @Override
    public List<MovimientoStock> findRecientes(BloqueId bloqueId, NegocioId negocioId, RegistroId registroId,
            int limite) {
        String filtro = registroId == null ? null : registroId.toString();
        return jpa.findRecientes(negocioId.valor(), bloqueId.valor(), filtro, limite).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.infrastructure.repository.entity.AgregadoZonaProductoEntity;
import org.springframework.stereotype.Component;

@Component
public class AgregadoZonaRepositoryMapper {

    public AgregadoZonaProducto toDomain(AgregadoZonaProductoEntity entity) {
        return new AgregadoZonaProducto(
                entity.getNombre(),
                entity.getComercios(),
                entity.getComerciosVenden(),
                entity.getPrecioMinimo(),
                entity.getPrecioPromedio(),
                entity.getPrecioMaximo(),
                entity.getUnidadesSemana());
    }
}

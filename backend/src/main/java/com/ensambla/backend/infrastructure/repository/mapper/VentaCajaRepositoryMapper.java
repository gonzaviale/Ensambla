package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.infrastructure.repository.entity.VentaCajaEntity;
import org.springframework.stereotype.Component;

@Component
public class VentaCajaRepositoryMapper {

    public VentaCaja toDomain(VentaCajaEntity entity) {
        return entity.getDatos();
    }

    public VentaCajaEntity toEntity(VentaCaja venta) {
        return VentaCajaEntity.builder()
                .id(venta.id().valor())
                .negocioId(venta.negocioId().valor())
                .proyectoId(venta.proyectoId().valor())
                .inventarioId(Identificador.valorDe(venta.inventarioId()))
                .datos(venta)
                .creadoEn(venta.creadoEn())
                .actualizadoEn(venta.creadoEn())
                .build();
    }
}

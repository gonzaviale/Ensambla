package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.infrastructure.repository.entity.PagoPremiumEntity;
import org.springframework.stereotype.Component;

@Component
public class PagoPremiumRepositoryMapper {

    public PagoPremium toDomain(PagoPremiumEntity origen) {
        return PagoPremium.builder()
                .facturaId(origen.getFacturaId())
                .negocioId(origen.getNegocioId())
                .renovacionId(origen.getRenovacionId())
                .pagoId(origen.getPagoId())
                .estado(origen.getEstado())
                .inicioEn(origen.getInicioEn())
                .finEn(origen.getFinEn())
                .modificadoProveedorEn(origen.getModificadoProveedorEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }

    public PagoPremiumEntity toEntity(PagoPremium origen) {
        return PagoPremiumEntity.builder()
                .facturaId(origen.getFacturaId())
                .negocioId(origen.getNegocioId())
                .renovacionId(origen.getRenovacionId())
                .pagoId(origen.getPagoId())
                .estado(origen.getEstado())
                .inicioEn(origen.getInicioEn())
                .finEn(origen.getFinEn())
                .modificadoProveedorEn(origen.getModificadoProveedorEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }
}

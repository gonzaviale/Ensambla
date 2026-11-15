package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.infrastructure.repository.entity.RenovacionPremiumEntity;
import org.springframework.stereotype.Component;

@Component
public class RenovacionPremiumRepositoryMapper {

    public RenovacionPremium toDomain(RenovacionPremiumEntity origen) {
        return RenovacionPremium.builder()
                .id(Identificador.opcional(origen.getId(), RenovacionId::de))
                .negocioId(Identificador.opcional(origen.getNegocioId(), NegocioId::de))
                .proveedorId(origen.getProveedorId())
                .enlace(origen.getEnlace())
                .estado(origen.getEstado())
                .importe(origen.getImporte())
                .moneda(origen.getMoneda())
                .primerCobroEn(origen.getPrimerCobroEn())
                .cancelacionSolicitada(origen.isCancelacionSolicitada())
                .proximoCobroEn(origen.getProximoCobroEn())
                .modificadoProveedorEn(origen.getModificadoProveedorEn())
                .sincronizadoEn(origen.getSincronizadoEn())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }

    public RenovacionPremiumEntity toEntity(RenovacionPremium origen) {
        return RenovacionPremiumEntity.builder()
                .id(Identificador.valorDe(origen.getId()))
                .negocioId(Identificador.valorDe(origen.getNegocioId()))
                .proveedorId(origen.getProveedorId())
                .enlace(origen.getEnlace())
                .estado(origen.getEstado())
                .importe(origen.getImporte())
                .moneda(origen.getMoneda())
                .primerCobroEn(origen.getPrimerCobroEn())
                .cancelacionSolicitada(origen.isCancelacionSolicitada())
                .proximoCobroEn(origen.getProximoCobroEn())
                .modificadoProveedorEn(origen.getModificadoProveedorEn())
                .sincronizadoEn(origen.getSincronizadoEn())
                .creadoEn(origen.getCreadoEn())
                .actualizadoEn(origen.getActualizadoEn())
                .build();
    }
}

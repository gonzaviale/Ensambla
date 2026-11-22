package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.CobroQrEntity;
import org.springframework.stereotype.Component;

@Component
public class CobroQrRepositoryMapper {

    public CobroQr toDomain(CobroQrEntity entity) {
        return new CobroQr(CobroQrId.de(entity.getId()), NegocioId.de(entity.getNegocioId()),
                new DestinoDeCobro(entity.getAlias(), entity.getTitular()), entity.getImporte(),
                entity.getEstado(), entity.getCreadoEn(), entity.getVenceEn(),
                entity.getConfirmadoEn(), entity.getConfirmacion(), entity.getActualizadoEn(),
                entity.getProveedorOrdenId(), entity.getProveedorPagoId(), entity.getQrDinamico(),
                entity.getQrEstatico(), entity.getEstadoDetalle());
    }

    public CobroQrEntity toEntity(CobroQr cobro) {
        return CobroQrEntity.builder()
                .id(cobro.id().valor())
                .negocioId(cobro.negocioId().valor())
                .alias(cobro.destino().alias())
                .titular(cobro.destino().titular())
                .importe(cobro.importe())
                .estado(cobro.estado())
                .creadoEn(cobro.creadoEn())
                .venceEn(cobro.venceEn())
                .confirmadoEn(cobro.confirmadoEn())
                .confirmacion(cobro.confirmacion())
                .actualizadoEn(cobro.actualizadoEn())
                .proveedorOrdenId(cobro.proveedorOrdenId())
                .proveedorPagoId(cobro.proveedorPagoId())
                .qrDinamico(cobro.qrDinamico())
                .qrEstatico(cobro.qrEstatico())
                .estadoDetalle(cobro.estadoDetalle())
                .build();
    }
}

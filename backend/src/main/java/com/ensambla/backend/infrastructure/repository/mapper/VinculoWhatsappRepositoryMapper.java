package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import org.springframework.stereotype.Component;

@Component
public class VinculoWhatsappRepositoryMapper {

    public VinculoWhatsapp toDomain(VinculoWhatsappEntity entity) {
        return VinculoWhatsapp.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .telefono(entity.getTelefono())
                .vinculadoEn(entity.getVinculadoEn())
                .codigo(entity.getCodigo())
                .codigoVenceEn(entity.getCodigoVenceEn())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public VinculoWhatsappEntity toEntity(VinculoWhatsapp vinculo) {
        return VinculoWhatsappEntity.builder()
                .id(vinculo.getId())
                .negocioId(vinculo.getNegocioId())
                .telefono(vinculo.getTelefono())
                .vinculadoEn(vinculo.getVinculadoEn())
                .codigo(vinculo.getCodigo())
                .codigoVenceEn(vinculo.getCodigoVenceEn())
                .creadoEn(vinculo.getCreadoEn())
                .actualizadoEn(vinculo.getActualizadoEn())
                .build();
    }
}

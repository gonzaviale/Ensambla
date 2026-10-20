package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.CodigoVinculacionEntity;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import org.springframework.stereotype.Component;

@Component
public class WhatsappRepositoryMapper {

    public VinculoWhatsapp toDomain(VinculoWhatsappEntity entity) {
        return VinculoWhatsapp.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .negocioId(entity.getNegocioId())
                .telefono(entity.getTelefono())
                .vinculadoEn(entity.getVinculadoEn())
                .build();
    }

    public VinculoWhatsappEntity toEntity(VinculoWhatsapp vinculo) {
        return VinculoWhatsappEntity.builder()
                .id(vinculo.getId())
                .usuarioId(vinculo.getUsuarioId())
                .negocioId(vinculo.getNegocioId())
                .telefono(vinculo.getTelefono())
                .vinculadoEn(vinculo.getVinculadoEn())
                .build();
    }

    public CodigoVinculacion toDomain(CodigoVinculacionEntity entity) {
        return CodigoVinculacion.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .codigo(entity.getCodigo())
                .expiraEn(entity.getExpiraEn())
                .usadoEn(entity.getUsadoEn())
                .build();
    }

    public CodigoVinculacionEntity toEntity(CodigoVinculacion codigo) {
        return CodigoVinculacionEntity.builder()
                .id(codigo.getId())
                .usuarioId(codigo.getUsuarioId())
                .codigo(codigo.getCodigo())
                .expiraEn(codigo.getExpiraEn())
                .usadoEn(codigo.getUsadoEn())
                .build();
    }

    public MensajeWhatsapp toDomain(MensajeWhatsappEntity entity) {
        return MensajeWhatsapp.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .telefono(entity.getTelefono())
                .idExterno(entity.getIdExterno())
                .tipo(entity.getTipo())
                .texto(entity.getTexto())
                .mimeAudio(entity.getMimeAudio())
                .duracionSeg(entity.getDuracionSeg())
                .estado(entity.getEstado())
                .recibidoEn(entity.getRecibidoEn())
                .build();
    }

    public MensajeWhatsappEntity toEntity(MensajeWhatsapp mensaje) {
        return MensajeWhatsappEntity.builder()
                .id(mensaje.getId())
                .negocioId(mensaje.getNegocioId())
                .telefono(mensaje.getTelefono())
                .idExterno(mensaje.getIdExterno())
                .tipo(mensaje.getTipo())
                .texto(mensaje.getTexto())
                .mimeAudio(mensaje.getMimeAudio())
                .duracionSeg(mensaje.getDuracionSeg())
                .estado(mensaje.getEstado())
                .recibidoEn(mensaje.getRecibidoEn())
                .build();
    }
}

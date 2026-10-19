package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import org.springframework.stereotype.Component;

@Component
public class MensajeWhatsappRepositoryMapper {

    public MensajeWhatsapp toDomain(MensajeWhatsappEntity entity) {
        return MensajeWhatsapp.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .telefono(entity.getTelefono())
                .waMensajeId(entity.getWaMensajeId())
                .tipo(entity.getTipo())
                .texto(entity.getTexto())
                .mediaId(entity.getMediaId())
                .estado(entity.getEstado())
                .recibidoEn(entity.getRecibidoEn())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public MensajeWhatsappEntity toEntity(MensajeWhatsapp mensaje) {
        return MensajeWhatsappEntity.builder()
                .id(mensaje.getId())
                .negocioId(mensaje.getNegocioId())
                .telefono(mensaje.getTelefono())
                .waMensajeId(mensaje.getWaMensajeId())
                .tipo(mensaje.getTipo())
                .texto(mensaje.getTexto())
                .mediaId(mensaje.getMediaId())
                .estado(mensaje.getEstado())
                .recibidoEn(mensaje.getRecibidoEn())
                .creadoEn(mensaje.getCreadoEn())
                .actualizadoEn(mensaje.getActualizadoEn())
                .build();
    }
}

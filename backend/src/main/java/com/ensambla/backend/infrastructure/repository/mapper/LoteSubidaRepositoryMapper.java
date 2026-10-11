package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import org.springframework.stereotype.Component;

@Component
public class LoteSubidaRepositoryMapper {

    public LoteSubida toDomain(LoteSubidaEntity source) {
        return LoteSubida.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .estado(source.getEstado())
                .intentos(source.getIntentos())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public LoteSubidaEntity toEntity(LoteSubida source) {
        return LoteSubidaEntity.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .estado(source.getEstado())
                .intentos(source.getIntentos())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public FotoCuaderno toDomain(FotoCuadernoEntity source) {
        return FotoCuaderno.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .loteId(source.getLoteId())
                .claveAlmacenamiento(source.getClaveAlmacenamiento())
                .tipoContenido(source.getTipoContenido())
                .tamanioBytes(source.getTamanioBytes())
                .rotacion(source.getRotacion())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public FotoCuadernoEntity toEntity(FotoCuaderno source) {
        return FotoCuadernoEntity.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .loteId(source.getLoteId())
                .claveAlmacenamiento(source.getClaveAlmacenamiento())
                .tipoContenido(source.getTipoContenido())
                .tamanioBytes(source.getTamanioBytes())
                .rotacion(source.getRotacion())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }
}

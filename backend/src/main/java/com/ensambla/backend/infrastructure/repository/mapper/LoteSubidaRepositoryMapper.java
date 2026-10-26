package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import org.springframework.stereotype.Component;

@Component
public class LoteSubidaRepositoryMapper {

    public LoteSubida toDomain(LoteSubidaEntity source) {
        return LoteSubida.builder()
                .id(Identificador.opcional(source.getId(), LoteId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .bloqueDestinoId(Identificador.opcional(source.getBloqueDestinoId(), BloqueId::de))
                .estado(source.getEstado())
                .intentos(source.getIntentos())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public LoteSubidaEntity toEntity(LoteSubida source) {
        return LoteSubidaEntity.builder()
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .bloqueDestinoId(Identificador.valorDe(source.getBloqueDestinoId()))
                .estado(source.getEstado())
                .intentos(source.getIntentos())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public FotoCuaderno toDomain(FotoCuadernoEntity source) {
        return FotoCuaderno.builder()
                .id(Identificador.opcional(source.getId(), FotoId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .loteId(Identificador.opcional(source.getLoteId(), LoteId::de))
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
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .loteId(Identificador.valorDe(source.getLoteId()))
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

package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.HistorialPrecioEntity;
import org.springframework.stereotype.Component;

@Component
public class HistorialPrecioRepositoryMapper {

    public CambioPrecio toDomain(HistorialPrecioEntity entity) {
        return new CambioPrecio(
                NegocioId.de(entity.getNegocioId()),
                RegistroId.de(entity.getRegistroId()),
                entity.getPrecioAnterior(),
                entity.getPrecioNuevo(),
                entity.getCambiadoEn());
    }

    public HistorialPrecioEntity toEntity(CambioPrecio cambio) {
        return HistorialPrecioEntity.builder()
                .negocioId(Identificador.valorDe(cambio.negocioId()))
                .registroId(Identificador.valorDe(cambio.registroId()))
                .precioAnterior(cambio.anterior())
                .precioNuevo(cambio.nuevo())
                .cambiadoEn(cambio.cambiadoEn())
                .build();
    }
}

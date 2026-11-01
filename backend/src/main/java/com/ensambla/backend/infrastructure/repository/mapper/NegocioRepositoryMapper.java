package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import org.springframework.stereotype.Component;

@Component
public class NegocioRepositoryMapper {

    public Negocio toDomain(NegocioEntity entity) {
        return Negocio.builder()
                .id(Identificador.opcional(entity.getId(), NegocioId::de))
                .nombre(entity.getNombre())
                .duenioId(Identificador.opcional(entity.getDuenioId(), UsuarioId::de))
                .rubro(entity.getRubro())
                .direccion(entity.getDireccion())
                .localidad(entity.getLocalidad())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .zonaId(Identificador.opcional(entity.getZonaId(), ZonaId::de))
                .comparteDatosZona(entity.getComparteDatosZona())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public NegocioEntity toEntity(Negocio negocio) {
        return NegocioEntity.builder()
                .id(Identificador.valorDe(negocio.getId()))
                .nombre(negocio.getNombre())
                .duenioId(Identificador.valorDe(negocio.getDuenioId()))
                .rubro(negocio.getRubro())
                .direccion(negocio.getDireccion())
                .localidad(negocio.getLocalidad())
                .latitud(negocio.getLatitud())
                .longitud(negocio.getLongitud())
                .zonaId(Identificador.valorDe(negocio.getZonaId()))
                .comparteDatosZona(negocio.getComparteDatosZona())
                .creadoEn(negocio.getCreadoEn())
                .actualizadoEn(negocio.getActualizadoEn())
                .build();
    }
}


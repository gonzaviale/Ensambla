package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.PreferenciaUsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class PreferenciaRepositoryMapper {

    public PreferenciaUsuario toDomain(PreferenciaUsuarioEntity entity) {
        return PreferenciaUsuario.builder()
                .usuarioId(Identificador.opcional(entity.getUsuarioId(), UsuarioId::de))
                .descartadas(entity.getAvisosDescartados())
                .grupos(entity.getGruposAvisos())
                .bienvenidaVista(entity.getBienvenidaVista())
                .nombre(entity.getNombrePreferido())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public PreferenciaUsuarioEntity toEntity(PreferenciaUsuario preferencia) {
        return PreferenciaUsuarioEntity.builder()
                .usuarioId(Identificador.valorDe(preferencia.getUsuarioId()))
                .avisosDescartados(preferencia.getDescartadas())
                .gruposAvisos(preferencia.getGrupos())
                .bienvenidaVista(preferencia.isBienvenidaVista())
                .nombrePreferido(preferencia.getNombre())
                .creadoEn(preferencia.getCreadoEn())
                .actualizadoEn(preferencia.getActualizadoEn())
                .build();
    }
}

package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioRepositoryMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        return Usuario.builder()
                .id(Identificador.opcional(entity.getId(), UsuarioId::de))
                .nombre(entity.getNombre())
                .email(entity.getEmail())
                .hashContrasenia(entity.getHashContrasenia())
                .estado(entity.getEstado())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public UsuarioEntity toEntity(Usuario usuario) {
        return UsuarioEntity.builder()
                .id(Identificador.valorDe(usuario.getId()))
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .hashContrasenia(usuario.getHashContrasenia())
                .estado(usuario.getEstado())
                .creadoEn(usuario.getCreadoEn())
                .actualizadoEn(usuario.getActualizadoEn())
                .build();
    }
}

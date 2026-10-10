package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.HASH;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.entity;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioRepositoryMapperTest {

    private UsuarioRepositoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UsuarioRepositoryMapper();
    }

    @Test
    @DisplayName("Mapea todos los campos de la entidad al modelo")
    void toDomainShouldMapEveryEntityField() {
        Usuario result = mapper.toDomain(entity());

        assertThat(result.getId()).isEqualTo(USUARIO_ID);
        assertThat(result.getNombre()).isEqualTo(NOMBRE);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getHashContrasenia()).isEqualTo(HASH);
        assertThat(result.estaActivo()).isTrue();
        assertThat(result.getCreadoEn()).isEqualTo(FECHA);
        assertThat(result.getActualizadoEn()).isEqualTo(FECHA);
    }

    @Test
    @DisplayName("Mapea todos los campos del modelo a la entidad")
    void toEntityShouldMapEveryDomainField() {
        UsuarioEntity result = mapper.toEntity(usuario());

        assertThat(result).usingRecursiveComparison().isEqualTo(entity());
    }
}

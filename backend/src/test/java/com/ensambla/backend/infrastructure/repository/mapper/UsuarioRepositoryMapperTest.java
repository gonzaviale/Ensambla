package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.support.IdsDePrueba;
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

    private final UsuarioRepositoryMapper mapper = new UsuarioRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos de la entidad al modelo")
    void unaEntidadSeLeeComoUsuarioConTodosSusDatos() {
        UsuarioEntity entidad = dadoUnaEntidadDeUsuario();

        Usuario resultado = cuandoSeConvierteEnUsuario(entidad);

        entoncesElUsuarioTieneTodosLosDatosDeLaEntidad(resultado);
    }

    @Test
    @DisplayName("Mapea todos los campos del modelo a la entidad")
    void unUsuarioSeGuardaConTodosSusDatos() {
        Usuario usuario = dadoUnUsuario();

        UsuarioEntity resultado = cuandoSeConvierteEnEntidad(usuario);

        entoncesLaEntidadTieneTodosLosDatosDelUsuario(resultado);
    }

    // --- dado ---
    private UsuarioEntity dadoUnaEntidadDeUsuario() {
        return entity();
    }

    private Usuario dadoUnUsuario() {
        return usuario();
    }

    // --- cuando ---
    private Usuario cuandoSeConvierteEnUsuario(UsuarioEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private UsuarioEntity cuandoSeConvierteEnEntidad(Usuario usuario) {
        return mapper.toEntity(usuario);
    }

    // --- entonces ---
    private void entoncesElUsuarioTieneTodosLosDatosDeLaEntidad(Usuario resultado) {
        assertThat(resultado.getId()).isEqualTo(USUARIO_ID);
        assertThat(resultado.getNombre()).isEqualTo(NOMBRE);
        assertThat(resultado.getEmail()).isEqualTo(EMAIL);
        assertThat(resultado.getHashContrasenia()).isEqualTo(HASH);
        assertThat(resultado.estaActivo()).isTrue();
        assertThat(resultado.getCreadoEn()).isEqualTo(FECHA);
        assertThat(resultado.getActualizadoEn()).isEqualTo(FECHA);
    }

    private void entoncesLaEntidadTieneTodosLosDatosDelUsuario(UsuarioEntity resultado) {
        assertThat(resultado).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(entity());
    }
}

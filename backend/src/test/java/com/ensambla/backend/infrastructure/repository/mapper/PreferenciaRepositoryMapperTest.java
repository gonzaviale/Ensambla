package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.PreferenciaUsuarioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreferenciaRepositoryMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final PreferenciaRepositoryMapper mapper = new PreferenciaRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void unaEntidadSeLeeComoPreferenciaConTodosSusDatos() {
        PreferenciaUsuarioEntity entidad = dadoUnaEntidadConUnAvisoDescartadoYUnGrupoOculto();

        PreferenciaUsuario preferencia = cuandoSeConvierteEnPreferencia(entidad);

        entoncesLaPreferenciaTieneTodosLosDatosDeLaEntidad(preferencia);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void unaPreferenciaSeGuardaConTodosSusDatos() {
        PreferenciaUsuario preferencia = dadoUnaPreferenciaConUnAvisoDescartadoYUnGrupoOculto();

        PreferenciaUsuarioEntity entidad = cuandoSeConvierteEnEntidad(preferencia);

        entoncesLaEntidadTieneTodosLosDatosDeLaPreferencia(entidad);
    }

    // --- dado ---
    private PreferenciaUsuarioEntity dadoUnaEntidadConUnAvisoDescartadoYUnGrupoOculto() {
        return PreferenciaUsuarioEntity.builder()
                .usuarioId(USUARIO_ID.valor())
                .avisosDescartados(List.of("a1"))
                .gruposAvisos(Map.of("ZONA", false))
                .bienvenidaVista(true)
                .nombrePreferido("Gonza")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    private PreferenciaUsuario dadoUnaPreferenciaConUnAvisoDescartadoYUnGrupoOculto() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        preferencia.actualizar(List.of("a1"), Map.of("ZONA", false), true, "Gonza", AHORA);
        return preferencia;
    }

    // --- cuando ---
    private PreferenciaUsuario cuandoSeConvierteEnPreferencia(PreferenciaUsuarioEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private PreferenciaUsuarioEntity cuandoSeConvierteEnEntidad(PreferenciaUsuario preferencia) {
        return mapper.toEntity(preferencia);
    }

    // --- entonces ---
    private void entoncesLaPreferenciaTieneTodosLosDatosDeLaEntidad(PreferenciaUsuario preferencia) {
        assertThat(preferencia.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(preferencia.getDescartadas()).containsExactly("a1");
        assertThat(preferencia.getGrupos()).containsEntry("ZONA", false);
        assertThat(preferencia.isBienvenidaVista()).isTrue();
        assertThat(preferencia.getNombre()).isEqualTo("Gonza");
        assertThat(preferencia.getCreadoEn()).isEqualTo(AHORA);
        assertThat(preferencia.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaEntidadTieneTodosLosDatosDeLaPreferencia(PreferenciaUsuarioEntity entidad) {
        assertThat(entidad.getUsuarioId()).isEqualTo(USUARIO_ID.valor());
        assertThat(entidad.getAvisosDescartados()).containsExactly("a1");
        assertThat(entidad.getGruposAvisos()).containsEntry("ZONA", false);
        assertThat(entidad.getBienvenidaVista()).isTrue();
        assertThat(entidad.getNombrePreferido()).isEqualTo("Gonza");
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA);
    }
}

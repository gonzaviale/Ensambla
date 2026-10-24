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
    void toDomainShouldMapEveryField() {
        PreferenciaUsuarioEntity entity = PreferenciaUsuarioEntity.builder()
                .usuarioId(USUARIO_ID.valor()).avisosDescartados(List.of("a1")).gruposAvisos(Map.of("ZONA", false))
                .bienvenidaVista(true).nombrePreferido("Gonza").creadoEn(AHORA).actualizadoEn(AHORA).build();

        PreferenciaUsuario result = mapper.toDomain(entity);

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.getDescartadas()).containsExactly("a1");
        assertThat(result.getGrupos()).containsEntry("ZONA", false);
        assertThat(result.isBienvenidaVista()).isTrue();
        assertThat(result.getNombre()).isEqualTo("Gonza");
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
        preferencia.actualizar(List.of("a1"), Map.of("ZONA", false), true, "Gonza", AHORA);

        PreferenciaUsuarioEntity result = mapper.toEntity(preferencia);

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID.valor());
        assertThat(result.getAvisosDescartados()).containsExactly("a1");
        assertThat(result.getGruposAvisos()).containsEntry("ZONA", false);
        assertThat(result.getBienvenidaVista()).isTrue();
        assertThat(result.getNombrePreferido()).isEqualTo("Gonza");
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }
}

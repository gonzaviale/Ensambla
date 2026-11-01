package com.ensambla.backend.domain.model.preferencia;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreferenciaUsuarioTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");
    private static final OffsetDateTime DESPUES = AHORA.plusHours(1);

    @Test
    @DisplayName("Las preferencias iniciales no descartan nada ni apagan grupos")
    void inicialShouldStartEmpty() {
        PreferenciaUsuario result = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.getDescartadas()).isEmpty();
        assertThat(result.getGrupos()).isEmpty();
        assertThat(result.isBienvenidaVista()).isFalse();
        assertThat(result.getNombre()).isNull();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Actualizar reemplaza los datos, recorta el nombre y mueve la fecha de actualización")
    void actualizarShouldReplaceData() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);

        preferencia.actualizar(List.of("a1"), Map.of("STOCK", false), true, "  Gonza ", DESPUES);

        assertThat(preferencia.getDescartadas()).containsExactly("a1");
        assertThat(preferencia.getGrupos()).containsEntry("STOCK", false);
        assertThat(preferencia.isBienvenidaVista()).isTrue();
        assertThat(preferencia.getNombre()).isEqualTo("Gonza");
        assertThat(preferencia.getCreadoEn()).isEqualTo(AHORA);
        assertThat(preferencia.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Un nombre vacío o en blanco se guarda como ausente")
    void actualizarShouldDropBlankNombre() {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);

        preferencia.actualizar(List.of(), Map.of(), false, "   ", DESPUES);
        assertThat(preferencia.getNombre()).isNull();

        preferencia.actualizar(List.of(), Map.of(), false, null, DESPUES);
        assertThat(preferencia.getNombre()).isNull();
    }
}

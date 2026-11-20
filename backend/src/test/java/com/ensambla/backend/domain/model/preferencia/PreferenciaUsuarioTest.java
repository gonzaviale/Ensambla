package com.ensambla.backend.domain.model.preferencia;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreferenciaUsuarioTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");
    private static final OffsetDateTime DESPUES = AHORA.plusHours(1);

    private PreferenciaUsuario preferencia;

    @Test
    @DisplayName("Las preferencias iniciales no descartan nada ni apagan grupos")
    void lasPreferenciasInicialesNoDescartanNadaNiApaganGrupos() {
        cuandoSeCreanLasPreferenciasIniciales();

        entoncesEmpiezanVacias();
    }

    @Test
    @DisplayName("Actualizar reemplaza los datos, recorta el nombre y mueve la fecha de actualización")
    void actualizarReemplazaLosDatosRecortaElNombreYMueveLaFechaDeActualizacion() {
        dadoQueSeCrearonLasPreferenciasIniciales();

        cuandoSeActualizanConElNombre("  Gonza ");

        entoncesSeReemplazanLosDatosYSeMueveLaFecha();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Un nombre vacío o en blanco se guarda como ausente")
    void unNombreVacioOEnBlancoSeGuardaComoAusente(String nombre) {
        dadoQueSeCrearonLasPreferenciasIniciales();

        cuandoSeActualizanConElNombre(nombre);

        entoncesElNombreQuedaAusente();
    }

    // --- dado ---

    private void dadoQueSeCrearonLasPreferenciasIniciales() {
        preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
    }

    // --- cuando ---

    private void cuandoSeCreanLasPreferenciasIniciales() {
        preferencia = PreferenciaUsuario.inicial(USUARIO_ID, AHORA);
    }

    private void cuandoSeActualizanConElNombre(String nombre) {
        preferencia.actualizar(List.of("a1"), Map.of("STOCK", false), true, nombre, DESPUES);
    }

    // --- entonces ---

    private void entoncesEmpiezanVacias() {
        assertThat(preferencia.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(preferencia.getDescartadas()).isEmpty();
        assertThat(preferencia.getGrupos()).isEmpty();
        assertThat(preferencia.isBienvenidaVista()).isFalse();
        assertThat(preferencia.getNombre()).isNull();
        assertThat(preferencia.getCreadoEn()).isEqualTo(AHORA);
        assertThat(preferencia.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeReemplazanLosDatosYSeMueveLaFecha() {
        assertThat(preferencia.getDescartadas()).containsExactly("a1");
        assertThat(preferencia.getGrupos()).containsEntry("STOCK", false);
        assertThat(preferencia.isBienvenidaVista()).isTrue();
        assertThat(preferencia.getNombre()).isEqualTo("Gonza");
        assertThat(preferencia.getCreadoEn()).isEqualTo(AHORA);
        assertThat(preferencia.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void entoncesElNombreQuedaAusente() {
        assertThat(preferencia.getNombre()).isNull();
    }
}

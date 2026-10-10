package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID CANTIDAD_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-09T12:00:00Z");

    @Test
    @DisplayName("Un registro nuevo nace sin id y con la misma fecha de creación y de modificación")
    void nuevoShouldBuildRegistroWithoutIdAndWithBothDates() {
        Registro registro = Registro.nuevo(NEGOCIO_ID, BLOQUE_ID, Map.of("k", "v"), AHORA);

        assertThat(registro.getId()).isNull();
        assertThat(registro.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(registro.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(registro.getValores()).containsEntry("k", "v");
        assertThat(registro.getFotoOrigenId()).isNull();
        assertThat(registro.getCreadoEn()).isEqualTo(AHORA);
        assertThat(registro.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Valida y devuelve los valores de los campos sin espacios en los bordes")
    void validarShouldReturnTrimmedValues() {
        Map<String, Object> valores = Map.of(
                NOMBRE_ID.toString(), "  Harina  ",
                CANTIDAD_ID.toString(), 12);

        Map<String, Object> result = Registro.validar(campos(true), valores);

        assertThat(result)
                .containsEntry(NOMBRE_ID.toString(), "Harina")
                .containsEntry(CANTIDAD_ID.toString(), 12)
                .hasSize(2);
    }

    @Test
    @DisplayName("Descarta las claves que no son campos del bloque")
    void validarShouldDiscardUnknownKeys() {
        Map<String, Object> valores = Map.of(
                NOMBRE_ID.toString(), "Harina",
                "otro", "dato");

        Map<String, Object> result = Registro.validar(campos(true), valores);

        assertThat(result).containsOnlyKeys(NOMBRE_ID.toString());
    }

    @Test
    @DisplayName("Omite los datos vacíos de los campos opcionales")
    void validarShouldOmitEmptyOptionalValues() {
        Map<String, Object> valores = Map.of(
                NOMBRE_ID.toString(), "Harina",
                CANTIDAD_ID.toString(), "   ");

        Map<String, Object> result = Registro.validar(campos(true), valores);

        assertThat(result).containsOnlyKeys(NOMBRE_ID.toString());
    }

    @Test
    @DisplayName("Marca como obligatorio al campo requerido que no se cargó")
    void validarShouldReportMissingRequiredField() {
        Map<String, Object> valores = Map.of(CANTIDAD_ID.toString(), 3);

        assertThatThrownBy(() -> Registro.validar(campos(true), valores))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, error -> assertThat(error.getErrores())
                        .containsOnly(Map.entry(NOMBRE_ID.toString(), "Este dato es obligatorio")));
    }

    @Test
    @DisplayName("Junta el error de cada campo inválido")
    void validarShouldCollectEveryFieldError() {
        Map<String, Object> valores = Map.of(
                NOMBRE_ID.toString(), 5,
                CANTIDAD_ID.toString(), "muchos");

        assertThatThrownBy(() -> Registro.validar(campos(true), valores))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, error -> assertThat(error.getErrores())
                        .containsEntry(NOMBRE_ID.toString(), "Tiene que ser un texto")
                        .containsEntry(CANTIDAD_ID.toString(), "Tiene que ser un número"));
    }

    @Test
    @DisplayName("Acepta un registro sin datos cuando ningún campo es obligatorio")
    void validarShouldAcceptEmptyValuesWhenNothingIsRequired() {
        Map<String, Object> result = Registro.validar(campos(false), Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Combina los valores actuales con los cambios sin modificar el registro")
    void combinarConShouldOverlayChangesWithoutMutating() {
        Registro registro = registroConValores(Map.of(NOMBRE_ID.toString(), "Harina", CANTIDAD_ID.toString(), 3));
        Map<String, Object> cambios = Map.of(CANTIDAD_ID.toString(), 8);

        Map<String, Object> result = registro.combinarCon(cambios);

        assertThat(result)
                .containsEntry(NOMBRE_ID.toString(), "Harina")
                .containsEntry(CANTIDAD_ID.toString(), 8);
        assertThat(registro.getValores()).containsEntry(CANTIDAD_ID.toString(), 3);
    }

    @Test
    @DisplayName("Un cambio nulo reemplaza el valor actual para poder borrarlo")
    void combinarConShouldKeepNullChange() {
        Registro registro = registroConValores(Map.of(CANTIDAD_ID.toString(), 3));
        Map<String, Object> cambios = new HashMap<>();
        cambios.put(CANTIDAD_ID.toString(), null);

        Map<String, Object> result = registro.combinarCon(cambios);

        assertThat(result).containsEntry(CANTIDAD_ID.toString(), null);
    }

    @Test
    @DisplayName("Reemplazar los valores actualiza la fecha de modificación y respeta la de creación")
    void reemplazarValoresShouldSetValuesAndTouchActualizadoEn() {
        Registro registro = registroConValores(Map.of("k", "v"));

        registro.reemplazarValores(Map.of("k", "nuevo"), AHORA);

        assertThat(registro.getValores()).containsEntry("k", "nuevo");
        assertThat(registro.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(registro.getCreadoEn()).isEqualTo(ANTES);
    }

    // --- helpers ---
    private List<Campo> campos(boolean nombreObligatorio) {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(nombreObligatorio).build(),
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).obligatorio(false).build());
    }

    private Registro registroConValores(Map<String, Object> valores) {
        return Registro.builder()
                .id(UUID.randomUUID())
                .valores(valores)
                .creadoEn(ANTES)
                .actualizadoEn(ANTES)
                .build();
    }
}

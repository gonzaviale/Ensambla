package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmacionLecturaTest {

    private static final UUID PROYECTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000021");
    private static final UUID BLOQUE_ID = UUID.fromString("00000000-0000-4000-8000-000000000022");

    @Test
    @DisplayName("Acepta un proyecto y una tabla nuevos con columnas y filas")
    void validarShouldAcceptNewProyectoAndBloque() {
        ConfirmacionLectura confirmacion = confirmacion(null, "Ventas", null, "Ventas", List.of(columna("monto")));

        assertThatCode(confirmacion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta una tabla nueva dentro de un proyecto existente sin nombre de proyecto")
    void validarShouldAcceptExistingProyectoWithoutName() {
        ConfirmacionLectura confirmacion = confirmacion(PROYECTO_ID, null, null, "Ventas", List.of(columna("monto")));

        assertThatCode(confirmacion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta sumar filas a un bloque existente sin nombres")
    void validarShouldAcceptExistingBloqueWithoutNames() {
        ConfirmacionLectura confirmacion = confirmacion(null, null, BLOQUE_ID, null, List.of(columna("monto")));

        assertThatCode(confirmacion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza una tabla nueva sin nombre")
    void validarShouldRejectNewBloqueWithoutName() {
        ConfirmacionLectura confirmacion = confirmacion(PROYECTO_ID, null, null, "  ", List.of(columna("monto")));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Escribí un nombre para la tabla");
    }

    @Test
    @DisplayName("Rechaza una tabla nueva sin nombre cuando el nombre es nulo")
    void validarShouldRejectNewBloqueWithNullName() {
        ConfirmacionLectura confirmacion = confirmacion(PROYECTO_ID, null, null, null, List.of(columna("monto")));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Escribí un nombre para la tabla");
    }

    @Test
    @DisplayName("Rechaza un proyecto nuevo sin nombre")
    void validarShouldRejectNewProyectoWithoutName() {
        ConfirmacionLectura confirmacion = confirmacion(null, "", null, "Ventas", List.of(columna("monto")));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Escribí un nombre para el proyecto");
    }

    @Test
    @DisplayName("Rechaza una confirmación sin columnas")
    void validarShouldRejectWithoutColumns() {
        ConfirmacionLectura confirmacion = confirmacion(null, "Ventas", null, "Ventas", List.of());

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dejá al menos una columna");
    }

    @Test
    @DisplayName("Rechaza columnas con la misma clave")
    void validarShouldRejectRepeatedKeys() {
        ConfirmacionLectura confirmacion = confirmacion(
                null, "Ventas", null, "Ventas", List.of(columna("monto"), columna("monto")));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Las columnas necesitan una clave distinta cada una");
    }

    @Test
    @DisplayName("Rechaza una columna sin clave")
    void validarShouldRejectBlankKey() {
        ConfirmacionLectura confirmacion = confirmacion(null, "Ventas", null, "Ventas", List.of(columna(" ")));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Las columnas necesitan una clave distinta cada una");
    }

    @Test
    @DisplayName("Rechaza más filas que el máximo por confirmación")
    void validarShouldRejectTooManyRows() {
        List<FilaConfirmada> filas = Collections.nCopies(
                ConfirmacionLectura.MAX_FILAS + 1, new FilaConfirmada(1, Map.of()));
        ConfirmacionLectura confirmacion = new ConfirmacionLectura(
                null, "Ventas", null, "Ventas", List.of(columna("monto")), filas);

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Podés guardar hasta 500 filas por vez");
    }

    // --- helpers ---
    private ConfirmacionLectura confirmacion(UUID proyectoId, String nombreProyecto, UUID bloqueId,
                                             String nombreBloque, List<CampoConfirmado> campos) {
        return new ConfirmacionLectura(
                proyectoId, nombreProyecto, bloqueId, nombreBloque, campos, List.of(new FilaConfirmada(1, Map.of())));
    }

    private CampoConfirmado columna(String clave) {
        return new CampoConfirmado(clave, null, "Monto", "NUMERO", List.of());
    }
}

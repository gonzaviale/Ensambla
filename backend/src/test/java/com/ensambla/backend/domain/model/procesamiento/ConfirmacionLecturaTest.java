package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmacionLecturaTest {

    private static final ProyectoId PROYECTO_ID = ProyectoId.de("00000000-0000-4000-8000-000000000021");
    private static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000022");

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

    @Test
    @DisplayName("Rechaza una columna de conexión nueva: las conexiones se arman en la tabla, no en la lectura")
    void validarShouldRejectNewRelacionColumn() {
        ConfirmacionLectura confirmacion = confirmacion(null, null, BLOQUE_ID, null,
                List.of(new CampoConfirmado("cliente", null, "Cliente", "RELACION", List.of())));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Una conexión con otra tabla no se puede crear acá: elegí una columna de tu tabla");
    }

    @Test
    @DisplayName("Acepta una columna de conexión que ya existe en la tabla")
    void validarShouldAcceptExistingRelacionColumn() {
        ConfirmacionLectura confirmacion = confirmacion(null, null, BLOQUE_ID, null,
                List.of(new CampoConfirmado("cliente", CampoId.nuevo(), "Cliente", "RELACION", List.of())));

        assertThatCode(confirmacion::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza dos columnas que apuntan al mismo campo del bloque: una pisaría los valores de la otra")
    void validarShouldRejectTwoColumnsOfTheSameCampo() {
        CampoId campoId = CampoId.nuevo();
        ConfirmacionLectura confirmacion = confirmacion(null, null, BLOQUE_ID, null, List.of(
                new CampoConfirmado("monto", campoId, "Monto", "NUMERO", List.of()),
                new CampoConfirmado("importe", campoId, "Monto", "NUMERO", List.of())));

        assertThatThrownBy(confirmacion::validar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dos columnas no pueden guardarse en el mismo campo de la tabla");
    }

    @Test
    @DisplayName("Acepta columnas de campos distintos junto con columnas nuevas")
    void validarShouldAcceptExistingAndNewColumnsTogether() {
        ConfirmacionLectura confirmacion = confirmacion(null, null, BLOQUE_ID, null, List.of(
                new CampoConfirmado("monto", CampoId.nuevo(), "Monto", "NUMERO", List.of()),
                new CampoConfirmado("fecha", CampoId.nuevo(), "Fecha", "FECHA", List.of()),
                new CampoConfirmado("telefono", null, "Teléfono", "TEXTO", List.of()),
                new CampoConfirmado("direccion", null, "Dirección", "TEXTO", List.of())));

        assertThatCode(confirmacion::validar).doesNotThrowAnyException();
    }

    // --- helpers ---
    private ConfirmacionLectura confirmacion(ProyectoId proyectoId, String nombreProyecto, BloqueId bloqueId,
                                             String nombreBloque, List<CampoConfirmado> campos) {
        return new ConfirmacionLectura(
                proyectoId, nombreProyecto, bloqueId, nombreBloque, campos, List.of(new FilaConfirmada(1, Map.of())));
    }

    private CampoConfirmado columna(String clave) {
        return new CampoConfirmado(clave, null, "Monto", "NUMERO", List.of());
    }
}

package com.ensambla.backend.domain.model.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimientoStockTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final LocalDate HOY = LocalDate.parse("2026-10-13");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    @Test
    @DisplayName("Arma un movimiento válido sin id, con la nota sin espacios en los bordes")
    void nuevoShouldBuildValidMovimiento() {
        MovimientoStock result = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", new BigDecimal("2.5"), HOY.minusDays(1), " Compra ", HOY, AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getRegistroId()).isEqualTo(REGISTRO_ID);
        assertThat(result.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(result.getCantidad()).isEqualByComparingTo("2.5");
        assertThat(result.getFecha()).isEqualTo(HOY.minusDays(1));
        assertThat(result.getNota()).isEqualTo("Compra");
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.esSalida()).isFalse();
    }

    @Test
    @DisplayName("Sin fecha es de hoy y una nota en blanco queda vacía")
    void nuevoShouldDefaultDateToTodayAndDropBlankNote() {
        MovimientoStock result = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "SALIDA", BigDecimal.ONE, null, "   ", HOY, AHORA);

        assertThat(result.getFecha()).isEqualTo(HOY);
        assertThat(result.getNota()).isNull();
        assertThat(result.esSalida()).isTrue();
    }

    @Test
    @DisplayName("Una nota ausente queda vacía y la fecha de hoy es válida")
    void nuevoShouldAcceptNullNoteAndTodayDate() {
        MovimientoStock result = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", BigDecimal.TEN, HOY, null, HOY, AHORA);

        assertThat(result.getNota()).isNull();
        assertThat(result.getFecha()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("Acepta la cantidad máxima y ceros de más en los decimales")
    void nuevoShouldAcceptMaximumQuantityAndTrailingZeros() {
        MovimientoStock maxima = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", MovimientoStock.CANTIDAD_MAXIMA, null, null, HOY, AHORA);
        MovimientoStock conCeros = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", new BigDecimal("1.50000"), null, null, HOY, AHORA);

        assertThat(maxima.getCantidad()).isEqualByComparingTo(MovimientoStock.CANTIDAD_MAXIMA);
        assertThat(conCeros.getCantidad()).isEqualByComparingTo("1.5");
    }

    @Test
    @DisplayName("Rechaza todos los datos inválidos juntos, un mensaje por dato")
    void nuevoShouldRejectEveryInvalidField() {
        assertThatThrownBy(() -> MovimientoStock.nuevo(
                NEGOCIO_ID, null, "OTRO", null, HOY.plusDays(1), "x".repeat(301), HOY, AHORA))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsAllEntriesOf(Map.of(
                                "registroId", "Elegí el producto",
                                "tipo", "Elegí si es una entrada o una salida",
                                "cantidad", "Escribí la cantidad",
                                "fecha", "No puede ser una fecha futura",
                                "nota", "Máximo 300 caracteres")));
    }

    @Test
    @DisplayName("Rechaza una cantidad en cero o negativa")
    void nuevoShouldRejectNonPositiveQuantity() {
        assertThat(errorDeCantidad(BigDecimal.ZERO)).isEqualTo("Tiene que ser mayor a cero");
        assertThat(errorDeCantidad(new BigDecimal("-1"))).isEqualTo("Tiene que ser mayor a cero");
    }

    @Test
    @DisplayName("Rechaza más de tres decimales y una cantidad demasiado grande")
    void nuevoShouldRejectTooManyDecimalsAndTooLargeQuantity() {
        assertThat(errorDeCantidad(new BigDecimal("0.0001"))).isEqualTo("Máximo 3 decimales");
        assertThat(errorDeCantidad(new BigDecimal("100000000000"))).isEqualTo("Es una cantidad demasiado grande");
    }

    private static String errorDeCantidad(BigDecimal cantidad) {
        try {
            MovimientoStock.nuevo(NEGOCIO_ID, REGISTRO_ID, "ENTRADA", cantidad, null, null, HOY, AHORA);
            return null;
        } catch (RegistroInvalidoException e) {
            return e.getErrores().get("cantidad");
        }
    }
}

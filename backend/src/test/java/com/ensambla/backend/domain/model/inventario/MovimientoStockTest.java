package com.ensambla.backend.domain.model.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MovimientoStockTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final LocalDate HOY = LocalDate.parse("2026-10-13");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    private MovimientoStock movimiento;
    private MovimientoStock otroMovimiento;
    private Throwable rechazo;

    @Test
    @DisplayName("Arma un movimiento válido sin id, con la nota sin espacios en los bordes")
    void armaUnMovimientoValidoSinIdConLaNotaSinEspaciosEnLosBordes() {
        cuandoSeRegistraUnaEntradaDeDosYMedioConNota(" Compra ");

        entoncesElMovimientoConservaSusDatosSinId();
    }

    @Test
    @DisplayName("Sin fecha es de hoy y una nota en blanco queda vacía")
    void sinFechaEsDeHoyYUnaNotaEnBlancoQuedaVacia() {
        cuandoSeRegistraUnaSalidaSinFechaYConNotaEnBlanco();

        entoncesLaFechaEsHoyYLaNotaQuedaVacia();
    }

    @Test
    @DisplayName("Una nota ausente queda vacía y la fecha de hoy es válida")
    void unaNotaAusenteQuedaVaciaYLaFechaDeHoyEsValida() {
        cuandoSeRegistraUnaEntradaDeHoySinNota();

        entoncesLaFechaEsHoyYLaNotaQuedaVacia();
    }

    @Test
    @DisplayName("Acepta la cantidad máxima y ceros de más en los decimales")
    void aceptaLaCantidadMaximaYCerosDeMasEnLosDecimales() {
        cuandoSeRegistranLaCantidadMaximaYUnaConCerosDeMas();

        entoncesSeAceptanAmbasCantidades();
    }

    @Test
    @DisplayName("Rechaza todos los datos inválidos juntos, un mensaje por dato")
    void rechazaTodosLosDatosInvalidosJuntosUnMensajePorDato() {
        cuandoSeIntentaRegistrarUnMovimientoConTodoInvalido();

        entoncesSeRechazaConUnMensajePorDato();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("Rechaza una cantidad en cero o negativa")
    void rechazaUnaCantidadEnCeroONegativa(String cantidad) {
        cuandoSeIntentaRegistrarLaCantidad(cantidad);

        entoncesElErrorDeLaCantidadEs("Tiene que ser mayor a cero");
    }

    @Test
    @DisplayName("Rechaza más de tres decimales")
    void rechazaMasDeTresDecimales() {
        cuandoSeIntentaRegistrarLaCantidad("0.0001");

        entoncesElErrorDeLaCantidadEs("Máximo 3 decimales");
    }

    @Test
    @DisplayName("Rechaza una cantidad demasiado grande")
    void rechazaUnaCantidadDemasiadoGrande() {
        cuandoSeIntentaRegistrarLaCantidad("100000000000");

        entoncesElErrorDeLaCantidadEs("Es una cantidad demasiado grande");
    }

    // --- cuando ---

    private void cuandoSeRegistraUnaEntradaDeDosYMedioConNota(String nota) {
        movimiento = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", new BigDecimal("2.5"), HOY.minusDays(1), nota, HOY, AHORA);
    }

    private void cuandoSeRegistraUnaSalidaSinFechaYConNotaEnBlanco() {
        movimiento = MovimientoStock.nuevo(NEGOCIO_ID, REGISTRO_ID, "SALIDA", BigDecimal.ONE, null, "   ", HOY, AHORA);
    }

    private void cuandoSeRegistraUnaEntradaDeHoySinNota() {
        movimiento = MovimientoStock.nuevo(NEGOCIO_ID, REGISTRO_ID, "ENTRADA", BigDecimal.TEN, HOY, null, HOY, AHORA);
    }

    private void cuandoSeRegistranLaCantidadMaximaYUnaConCerosDeMas() {
        movimiento = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", MovimientoStock.CANTIDAD_MAXIMA, null, null, HOY, AHORA);
        otroMovimiento = MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", new BigDecimal("1.50000"), null, null, HOY, AHORA);
    }

    private void cuandoSeIntentaRegistrarUnMovimientoConTodoInvalido() {
        rechazo = catchThrowable(() -> MovimientoStock.nuevo(
                NEGOCIO_ID, null, "OTRO", null, HOY.plusDays(1), "x".repeat(301), HOY, AHORA));
    }

    private void cuandoSeIntentaRegistrarLaCantidad(String cantidad) {
        rechazo = catchThrowable(() -> MovimientoStock.nuevo(
                NEGOCIO_ID, REGISTRO_ID, "ENTRADA", new BigDecimal(cantidad), null, null, HOY, AHORA));
    }

    // --- entonces ---

    private void entoncesElMovimientoConservaSusDatosSinId() {
        assertThat(movimiento.getId()).isNull();
        assertThat(movimiento.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(movimiento.getRegistroId()).isEqualTo(REGISTRO_ID);
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("2.5");
        assertThat(movimiento.getFecha()).isEqualTo(HOY.minusDays(1));
        assertThat(movimiento.getNota()).isEqualTo("Compra");
        assertThat(movimiento.getCreadoEn()).isEqualTo(AHORA);
        assertThat(movimiento.esSalida()).isFalse();
    }

    private void entoncesLaFechaEsHoyYLaNotaQuedaVacia() {
        assertThat(movimiento.getFecha()).isEqualTo(HOY);
        assertThat(movimiento.getNota()).isNull();
    }

    private void entoncesSeAceptanAmbasCantidades() {
        assertThat(movimiento.getCantidad()).isEqualByComparingTo(MovimientoStock.CANTIDAD_MAXIMA);
        assertThat(otroMovimiento.getCantidad()).isEqualByComparingTo("1.5");
    }

    private void entoncesSeRechazaConUnMensajePorDato() {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class,
                error -> assertThat(error.getErrores()).containsAllEntriesOf(Map.of(
                        "registroId", "Elegí el producto",
                        "tipo", "Elegí si es una entrada o una salida",
                        "cantidad", "Escribí la cantidad",
                        "fecha", "No puede ser una fecha futura",
                        "nota", "Máximo 300 caracteres")));
    }

    private void entoncesElErrorDeLaCantidadEs(String mensaje) {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class,
                error -> assertThat(error.getErrores().get("cantidad")).isEqualTo(mensaje));
    }
}

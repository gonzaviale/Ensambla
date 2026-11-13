package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProteccionCajaTest {

    private final Sistema actual = CajaTestData.sistema();
    private final Sistema propuesto = CajaTestData.sistema();

    @Test
    @DisplayName("Un sistema sin ventas de Caja conserva su edición habitual")
    void validateShouldAllowOrdinaryRows() {
        givenFila(null, actual);
        givenFila("WhatsApp", actual);

        assertThatCode(() -> ProteccionCaja.validar(List.of(actual), propuesto, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Una pestaña anterior a la venta no puede guardar stock desactualizado")
    void validateShouldRejectStaleEditor() {
        givenFila("Caja operación", actual);

        assertThatThrownBy(() -> ProteccionCaja.validar(List.of(actual), propuesto, CajaTestData.AHORA.minusSeconds(1)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Una escritura sin fecha de lectura no puede reemplazar ventas confirmadas")
    void validateShouldRejectUnversionedEdit() {
        givenFila("Caja operación", actual);

        assertThatThrownBy(() -> ProteccionCaja.validar(List.of(actual), propuesto, null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Una lectura vigente puede guardar conservando la venta intacta")
    void validateShouldAllowUnchangedReceipt() {
        Registro fila = givenFila("Caja operación", actual);
        propuesto.registrosPorBloque().put(CajaTestData.VENTAS, List.of(fila));

        assertThatCode(() -> ProteccionCaja.validar(List.of(actual), propuesto, CajaTestData.AHORA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("La venta confirmada no se puede quitar como un renglón común")
    void validateShouldRejectRemovedReceipt() {
        givenFila("Caja operación", actual);
        propuesto.registrosPorBloque().remove(CajaTestData.VENTAS);

        thenRejectsChangedReceipt();
    }

    @Test
    @DisplayName("La venta confirmada no se puede reemplazar con otro id")
    void validateShouldRejectReplacedReceipt() {
        givenFila("Caja operación", actual);
        givenFila("Caja operación", propuesto);

        thenRejectsChangedReceipt();
    }

    @Test
    @DisplayName("El contenido del comprobante es inmutable desde el editor")
    void validateShouldRejectChangedAmounts() {
        Registro fila = givenFila("Caja operación", actual);
        propuesto.registrosPorBloque().put(CajaTestData.VENTAS,
                List.of(fila.toBuilder().valores(Map.of("importe", 12)).build()));

        thenRejectsChangedReceipt();
    }

    @Test
    @DisplayName("El origen de Caja no puede quitarse para habilitar la edición")
    void validateShouldRejectChangedOrigin() {
        Registro fila = givenFila("Caja operación", actual);
        propuesto.registrosPorBloque().put(CajaTestData.VENTAS, List.of(fila.toBuilder().origen(null).build()));

        thenRejectsChangedReceipt();
    }

    // --- arrange ---
    private Registro givenFila(String origen, Sistema sistema) {
        Registro fila = Registro.nuevo(CajaTestData.NEGOCIO, CajaTestData.VENTAS, Map.of("importe", 10),
                CajaTestData.AHORA);
        fila.setId(RegistroId.nuevo());
        fila.setOrigen(origen);
        sistema.registrosPorBloque().get(CajaTestData.VENTAS).add(fila);
        return fila;
    }

    // --- assert ---
    private void thenRejectsChangedReceipt() {
        assertThatThrownBy(() -> ProteccionCaja.validar(List.of(actual), propuesto, CajaTestData.AHORA))
                .isInstanceOf(DuplicateResourceException.class);
    }
}

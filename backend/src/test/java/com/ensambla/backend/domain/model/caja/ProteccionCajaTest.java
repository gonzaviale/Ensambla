package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProteccionCajaTest {

    private final Sistema actual = CajaTestData.sistema();
    private final Sistema propuesto = CajaTestData.sistema();

    private Registro comprobante;
    private Throwable rechazo;

    @Test
    @DisplayName("Un sistema sin ventas de Caja conserva su edición habitual")
    void unSistemaSinVentasDeCajaConservaSuEdicionHabitual() {
        dadoUnSistemaConFilasComunes();

        cuandoSeValidaLaEdicionSinFechaDeLectura();

        entoncesSePermiteLaEdicion();
    }

    @Test
    @DisplayName("Una pestaña anterior a la venta no puede guardar stock desactualizado")
    void unaPestanaAnteriorALaVentaNoPuedeGuardarStockDesactualizado() {
        dadoUnComprobanteDeCajaConfirmado();

        cuandoSeValidaConUnaLecturaAnteriorALaVenta();

        entoncesSeRechazaElCambioDelComprobante();
    }

    @Test
    @DisplayName("Una escritura sin fecha de lectura no puede reemplazar ventas confirmadas")
    void unaEscrituraSinFechaDeLecturaNoPuedeReemplazarVentasConfirmadas() {
        dadoUnComprobanteDeCajaConfirmado();

        cuandoSeValidaLaEdicionSinFechaDeLectura();

        entoncesSeRechazaElCambioDelComprobante();
    }

    @Test
    @DisplayName("Una lectura vigente puede guardar conservando la venta intacta")
    void unaLecturaVigentePuedeGuardarConservandoLaVentaIntacta() {
        dadoUnComprobanteDeCajaConfirmado();
        dadoQueLaPropuestaConservaElComprobante();

        cuandoSeValidaConUnaLecturaVigente();

        entoncesSePermiteLaEdicion();
    }

    @Test
    @DisplayName("La venta confirmada no se puede quitar como un renglón común")
    void laVentaConfirmadaNoSePuedeQuitarComoUnRenglonComun() {
        dadoUnComprobanteDeCajaConfirmado();
        dadoQueLaPropuestaQuitaLosComprobantes();

        cuandoSeValidaConUnaLecturaVigente();

        entoncesSeRechazaElCambioDelComprobante();
    }

    @Test
    @DisplayName("La venta confirmada no se puede reemplazar con otro id")
    void laVentaConfirmadaNoSePuedeReemplazarConOtroId() {
        dadoUnComprobanteDeCajaConfirmado();
        dadoQueLaPropuestaTrae(comprobanteNuevoDeCaja());

        cuandoSeValidaConUnaLecturaVigente();

        entoncesSeRechazaElCambioDelComprobante();
    }

    @Test
    @DisplayName("El contenido del comprobante es inmutable desde el editor")
    void elContenidoDelComprobanteEsInmutableDesdeElEditor() {
        dadoUnComprobanteDeCajaConfirmado();
        dadoQueLaPropuestaTrae(comprobante.toBuilder().valores(Map.of("importe", 12)).build());

        cuandoSeValidaConUnaLecturaVigente();

        entoncesSeRechazaElCambioDelComprobante();
    }

    @Test
    @DisplayName("El origen de Caja no puede quitarse para habilitar la edición")
    void elOrigenDeCajaNoPuedeQuitarseParaHabilitarLaEdicion() {
        dadoUnComprobanteDeCajaConfirmado();
        dadoQueLaPropuestaTrae(comprobante.toBuilder().origen(null).build());

        cuandoSeValidaConUnaLecturaVigente();

        entoncesSeRechazaElCambioDelComprobante();
    }

    // --- dado ---

    private void dadoUnSistemaConFilasComunes() {
        agregarFila(null, actual);
        agregarFila("WhatsApp", actual);
    }

    private void dadoUnComprobanteDeCajaConfirmado() {
        comprobante = agregarFila("Caja operación", actual);
    }

    private void dadoQueLaPropuestaConservaElComprobante() {
        dadoQueLaPropuestaTrae(comprobante);
    }

    private void dadoQueLaPropuestaTrae(Registro fila) {
        propuesto.registrosPorBloque().put(CajaTestData.VENTAS, List.of(fila));
    }

    private void dadoQueLaPropuestaQuitaLosComprobantes() {
        propuesto.registrosPorBloque().remove(CajaTestData.VENTAS);
    }

    // --- cuando ---

    private void cuandoSeValidaLaEdicionSinFechaDeLectura() {
        rechazo = catchThrowable(() -> ProteccionCaja.validar(List.of(actual), propuesto, null));
    }

    private void cuandoSeValidaConUnaLecturaAnteriorALaVenta() {
        cuandoSeValidaConLaLectura(CajaTestData.AHORA.minusSeconds(1));
    }

    private void cuandoSeValidaConUnaLecturaVigente() {
        cuandoSeValidaConLaLectura(CajaTestData.AHORA);
    }

    private void cuandoSeValidaConLaLectura(OffsetDateTime lectura) {
        rechazo = catchThrowable(() -> ProteccionCaja.validar(List.of(actual), propuesto, lectura));
    }

    // --- entonces ---

    private void entoncesSePermiteLaEdicion() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaElCambioDelComprobante() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    // --- datos ---

    private Registro comprobanteNuevoDeCaja() {
        return agregarFila("Caja operación", propuesto);
    }

    private Registro agregarFila(String origen, Sistema sistema) {
        Registro fila = Registro.nuevo(CajaTestData.NEGOCIO, CajaTestData.VENTAS, Map.of("importe", 10),
                CajaTestData.AHORA);
        fila.setId(RegistroId.nuevo());
        fila.setOrigen(origen);
        sistema.registrosPorBloque().get(CajaTestData.VENTAS).add(fila);
        return fila;
    }
}

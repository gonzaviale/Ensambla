package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TablaDisponibleTest {

    private static final CampoConocido MONTO = new CampoConocido(CampoId.nuevo(), "Monto", TipoCampo.MONEDA, List.of());
    private static final CampoConocido NOMBRE = new CampoConocido(CampoId.nuevo(), "Nombre", TipoCampo.TEXTO, List.of());

    private TablaDisponible tabla;

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TipoBloque.class, names = {"INVENTARIO", "CLIENTES", "FORMULARIO"})
    @DisplayName("Las tablas de cosas (inventario, clientes, formularios) actualizan las filas existentes")
    void lasTablasDeCosasActualizanLasFilasExistentes(TipoBloque tipo) {
        dadoUnaTablaDelTipo(tipo);

        entoncesActualizaLasFilasExistentes(true);
    }

    @Test
    @DisplayName("Una tabla armada sólo con su bloque y su nombre actualiza las filas existentes")
    void unaTablaArmadaSoloConSuBloqueYSuNombreActualizaLasFilasExistentes() {
        dadoUnaTablaArmadaSinTipo();

        entoncesActualizaLasFilasExistentes(true);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TipoBloque.class,
            names = {"VENTAS", "GASTOS", "PEDIDOS", "ENTRADAS", "SALIDAS", "RESUMEN", "NOTA"})
    @DisplayName("Las tablas de movimientos (ventas, gastos, pedidos) suman una fila por mensaje")
    void lasTablasDeMovimientosSumanUnaFilaPorMensaje(TipoBloque tipo) {
        dadoUnaTablaDelTipo(tipo);

        entoncesActualizaLasFilasExistentes(false);
    }

    @Test
    @DisplayName("La columna que nombra a cada fila es la primera de texto")
    void laColumnaQueNombraACadaFilaEsLaPrimeraDeTexto() {
        dadoUnaTablaDelTipo(TipoBloque.INVENTARIO);

        entoncesElCampoIdentificadorEsElNombre();
    }

    @Test
    @DisplayName("Una tabla sin columnas de texto no tiene campo identificador")
    void unaTablaSinColumnasDeTextoNoTieneCampoIdentificador() {
        dadoUnaTablaSoloConMontos();

        entoncesNoTieneCampoIdentificador();
    }

    // --- dado ---
    private void dadoUnaTablaDelTipo(TipoBloque tipo) {
        tabla = unaTabla(tipo);
    }

    private void dadoUnaTablaArmadaSinTipo() {
        tabla = new TablaDisponible(unaTabla(TipoBloque.GASTOS).bloque(), "Panadería");
    }

    private void dadoUnaTablaSoloConMontos() {
        tabla = new TablaDisponible(new BloqueLectura(BloqueId.nuevo(), "Solo montos", List.of(MONTO)), "Panadería",
                TipoBloque.INVENTARIO);
    }

    private static TablaDisponible unaTabla(TipoBloque tipo) {
        return new TablaDisponible(new BloqueLectura(BloqueId.nuevo(), "Tabla", List.of(MONTO, NOMBRE)),
                "Panadería", tipo);
    }

    // --- entonces ---
    private void entoncesActualizaLasFilasExistentes(boolean esperado) {
        assertThat(tabla.actualizaFilasExistentes()).isEqualTo(esperado);
    }

    private void entoncesElCampoIdentificadorEsElNombre() {
        assertThat(tabla.campoIdentificador()).contains(NOMBRE);
    }

    private void entoncesNoTieneCampoIdentificador() {
        assertThat(tabla.campoIdentificador()).isEmpty();
    }
}

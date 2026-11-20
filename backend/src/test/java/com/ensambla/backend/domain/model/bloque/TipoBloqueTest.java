package com.ensambla.backend.domain.model.bloque;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TipoBloqueTest {

    private List<TipoBloque> obligatorios;
    private TipoBloque tipo;

    @Test
    @DisplayName("Todo negocio necesita Stock, Clientes, Ventas y Gastos")
    void todoNegocioNecesitaStockClientesVentasYGastos() {
        cuandoSePidenLosTiposObligatorios();

        entoncesSonStockClientesVentasYGastos();
    }

    @Test
    @DisplayName("Stock, Ventas, Clientes, Gastos, Entradas y Salidas llevan columnas fijas")
    void stockVentasClientesGastosEntradasYSalidasLlevanColumnasFijas() {
        entoncesLlevanColumnasFijas(TipoBloque.INVENTARIO, TipoBloque.VENTAS, TipoBloque.CLIENTES, TipoBloque.GASTOS,
                TipoBloque.ENTRADAS, TipoBloque.SALIDAS);
    }

    @Test
    @DisplayName("Pedidos, Resumen, Formulario y Nota no llevan columnas fijas")
    void pedidosResumenFormularioYNotaNoLlevanColumnasFijas() {
        entoncesNoLlevanColumnasFijas(TipoBloque.PEDIDOS, TipoBloque.RESUMEN, TipoBloque.FORMULARIO, TipoBloque.NOTA);
    }

    @ParameterizedTest
    @EnumSource(TipoBloque.class)
    @DisplayName("Cada tipo declara sus columnas fijas sin repetir claves")
    void cadaTipoDeclaraSusColumnasFijasSinRepetirClaves(TipoBloque tipoDeBloque) {
        dadoUnTipo(tipoDeBloque);

        entoncesNoRepiteClavesDeColumnas();
    }

    @Test
    @DisplayName("Una sección nace con su nombre, su color y la dirección de su stock")
    void unaSeccionNaceConSuNombreSuColorYLaDireccionDeSuStock() {
        dadoUnTipo(TipoBloque.VENTAS);

        entoncesNaceComo("Ventas", ColorBloque.AMARILLO, (short) -1);
    }

    @Test
    @DisplayName("Una sección de Stock no mueve stock")
    void unaSeccionDeStockNoMueveStock() {
        dadoUnTipo(TipoBloque.INVENTARIO);

        entoncesNaceComo("Stock", ColorBloque.NARANJA, null);
    }

    @Test
    @DisplayName("Los mensajes explican que Ensambla necesita la sección")
    void losMensajesExplicanQueEnsamblaNecesitaLaSeccion() {
        dadoUnTipo(TipoBloque.GASTOS);

        entoncesLosMensajesDicen("No se puede quitar la sección «Gastos»: Ensambla la necesita para funcionar.",
                "No se puede cambiar el tipo de una sección «Gastos»: Ensambla la necesita para funcionar. "
                        + "Podés cambiarle el nombre.");
    }

    private void dadoUnTipo(TipoBloque tipoDeBloque) {
        tipo = tipoDeBloque;
    }

    private void cuandoSePidenLosTiposObligatorios() {
        obligatorios = TipoBloque.obligatorios();
    }

    private void entoncesSonStockClientesVentasYGastos() {
        assertThat(obligatorios).containsExactly(TipoBloque.INVENTARIO, TipoBloque.CLIENTES, TipoBloque.VENTAS,
                TipoBloque.GASTOS);
    }

    private void entoncesLlevanColumnasFijas(TipoBloque... tipos) {
        assertThat(tipos).allMatch(TipoBloque::tieneColumnasFijas);
    }

    private void entoncesNoLlevanColumnasFijas(TipoBloque... tipos) {
        assertThat(tipos).noneMatch(TipoBloque::tieneColumnasFijas);
    }

    private void entoncesNoRepiteClavesDeColumnas() {
        assertThat(tipo.columnasFijas()).extracting(ColumnaFija::clave).doesNotHaveDuplicates();
    }

    private void entoncesNaceComo(String nombre, ColorBloque color, Short direccion) {
        assertThat(tipo.nombreInicial()).isEqualTo(nombre);
        assertThat(tipo.colorInicial()).isEqualTo(color);
        assertThat(tipo.direccionDeStock()).isEqualTo(direccion);
    }

    private void entoncesLosMensajesDicen(String alQuitar, String alCambiar) {
        assertThat(tipo.mensajeAlQuitarLaUltima()).isEqualTo(alQuitar);
        assertThat(tipo.mensajeAlCambiarElTipo()).isEqualTo(alCambiar);
    }
}

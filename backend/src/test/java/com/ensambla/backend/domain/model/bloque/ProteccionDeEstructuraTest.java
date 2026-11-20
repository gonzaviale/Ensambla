package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.sistema.Sistema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.SistemasDePrueba.agregarLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.cambiarElTipoDeLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.cambiarElTipoDeLaSeccion;
import static com.ensambla.backend.support.SistemasDePrueba.conLaConexionA;
import static com.ensambla.backend.support.SistemasDePrueba.conLasOpciones;
import static com.ensambla.backend.support.SistemasDePrueba.dejarDeExigirLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.laColumna;
import static com.ensambla.backend.support.SistemasDePrueba.laSeccionDe;
import static com.ensambla.backend.support.SistemasDePrueba.quitarLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.quitarLaSeccion;
import static com.ensambla.backend.support.SistemasDePrueba.renombrarLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.unaColumnaPropia;
import static com.ensambla.backend.support.SistemasDePrueba.unaCopiaDe;
import static com.ensambla.backend.support.SistemasDePrueba.unaSeccionVaciaDe;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaCompleto;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaCon;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaConOtroId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProteccionDeEstructuraTest {

    private Sistema actual;
    private Sistema propuesto;
    private Throwable rechazo;

    @Test
    @DisplayName("Un guardado que no toca la estructura fija se acepta")
    void unGuardadoQueNoTocaLaEstructuraFijaSeAcepta() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Un negocio nuevo no se compara con nada")
    void unNegocioNuevoNoSeComparaConNada() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();

        cuandoSeValidaContraNingunNegocioGuardado();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Un guardado se compara sólo con el mismo negocio")
    void unGuardadoSeComparaSoloConElMismoNegocio() {
        dadoUnNegocioGuardadoDeOtroIdYUnaPropuestaSinVentas();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Una sección que falta en lo que se guarda no se rechaza: se repone al completar")
    void unaSeccionQueFaltaEnLoQueSeGuardaNoSeRechazaSeReponeAlCompletar() {
        dadoUnaPropuestaSinLaSeccionDeVentas();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("No se puede cambiar el tipo de una sección de Stock")
    void noSePuedeCambiarElTipoDeUnaSeccionDeStock() {
        dadoUnNegocioConDosStockYUnaPropuestaDondeUnoPasaAFormulario();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorque("No se puede cambiar el tipo de una sección «Stock»: Ensambla la necesita para "
                + "funcionar. Podés cambiarle el nombre.");
    }

    @Test
    @DisplayName("Se puede cambiar el tipo de una sección que no lleva columnas fijas")
    void sePuedeCambiarElTipoDeUnaSeccionQueNoLlevaColumnasFijas() {
        dadoUnNegocioConUnaNotaQuePasaAFormulario();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Una columna que falta en lo que se guarda no se rechaza: se repone al completar")
    void unaColumnaQueFaltaEnLoQueSeGuardaNoSeRechazaSeReponeAlCompletar() {
        dadoUnNegocioGuardadoYUnaPropuestaSinLaColumnaDeCosto();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Una columna que ya no cumplía su función se acepta tal como estaba")
    void unaColumnaQueYaNoCumpliaSuFuncionSeAceptaTalComoEstaba() {
        dadoUnNegocioConElPrecioEnTextoYLaMismaPropuesta();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("No se puede cambiar el tipo de dato del precio")
    void noSePuedeCambiarElTipoDeDatoDelPrecio() {
        dadoUnaPropuestaConElPrecioEnTexto();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorque("No se puede cambiar «Precio»: Ensambla la necesita para cobrar en Caja y calcular "
                + "la ganancia. Podés cambiarle el nombre.");
    }

    @Test
    @DisplayName("No se puede dejar de exigir la cantidad")
    void noSePuedeDejarDeExigirLaCantidad() {
        dadoUnaPropuestaDondeLaCantidadYaNoEsObligatoria();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorque("No se puede cambiar «Cantidad»: Ensambla la necesita para controlar el stock y "
                + "descontarlo en Caja. Podés cambiarle el nombre.");
    }

    @Test
    @DisplayName("No se puede quitar la mercadería de las categorías de gasto")
    void noSePuedeQuitarLaMercaderiaDeLasCategoriasDeGasto() {
        dadoUnaPropuestaSinLaCategoriaMercaderia();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorque("No se puede cambiar «Categoría»: Ensambla la necesita para separar la mercadería "
                + "de los otros gastos. Podés cambiarle el nombre.");
    }

    @Test
    @DisplayName("No se puede apuntar el producto de una venta a otra sección que no sea Stock")
    void noSePuedeApuntarElProductoDeUnaVentaAOtraSeccionQueNoSeaStock() {
        dadoUnaPropuestaConElProductoApuntandoAClientes();

        cuandoSeIntentaValidar();

        entoncesSeRechazaPorque("No se puede cambiar «Producto»: Ensambla la necesita para saber qué producto se "
                + "vendió. Podés cambiarle el nombre.");
    }

    @Test
    @DisplayName("Se puede cambiar el nombre de una columna fija")
    void sePuedeCambiarElNombreDeUnaColumnaFija() {
        dadoUnaPropuestaConElPrecioRenombrado();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Quitar la última sección de Stock por fuera del guardado se rechaza")
    void quitarLaUltimaSeccionDeStockPorFueraDelGuardadoSeRechaza() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();

        cuandoSeIntentaQuitarLaSeccionDeStock();

        entoncesSeRechazaPorque("No se puede quitar la sección «Stock»: Ensambla la necesita para funcionar.");
    }

    @Test
    @DisplayName("Quitar una sección de Stock por fuera del guardado se acepta si hay otra")
    void quitarUnaSeccionDeStockPorFueraDelGuardadoSeAceptaSiHayOtra() {
        dadoUnNegocioConDosStockYUnaPropuestaConUno();

        cuandoSeIntentaQuitarLaSeccionDeStock();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Quitar una columna fija por fuera del guardado se rechaza")
    void quitarUnaColumnaFijaPorFueraDelGuardadoSeRechaza() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();

        cuandoSeIntentaQuitarLaColumna("cost");

        entoncesSeRechazaPorque("No se puede quitar «Costo»: Ensambla la necesita para calcular la ganancia de "
                + "Objetivos y Mi negocio.");
    }

    @Test
    @DisplayName("Quitar una columna propia por fuera del guardado se acepta")
    void quitarUnaColumnaPropiaPorFueraDelGuardadoSeAcepta() {
        dadoUnNegocioConUnaColumnaPropiaQueLaPropuestaQuita();

        cuandoSeIntentaQuitarLaColumna("marca");

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Cambiar el tipo de una columna fija por fuera del guardado se rechaza")
    void cambiarElTipoDeUnaColumnaFijaPorFueraDelGuardadoSeRechaza() {
        dadoUnaPropuestaConElPrecioEnTexto();

        cuandoSeIntentaCambiarLaColumna("price");

        entoncesSeRechazaPorque("No se puede cambiar «Precio»: Ensambla la necesita para cobrar en Caja y calcular "
                + "la ganancia. Podés cambiarle el nombre.");
    }

    @Test
    @DisplayName("Cambiar sólo el nombre de una columna fija por fuera del guardado se acepta")
    void cambiarSoloElNombreDeUnaColumnaFijaPorFueraDelGuardadoSeAcepta() {
        dadoUnaPropuestaConElPrecioRenombrado();

        cuandoSeIntentaCambiarLaColumna("price");

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Cambiar el tipo de una columna propia por fuera del guardado se acepta")
    void cambiarElTipoDeUnaColumnaPropiaPorFueraDelGuardadoSeAcepta() {
        dadoUnaPropuestaConLaColumnaPropiaEnOtroTipo();

        cuandoSeIntentaCambiarLaColumna("marca");

        entoncesSeAcepta();
    }

    private void dadoUnNegocioGuardadoYUnaPropuestaIgual() {
        actual = unSistemaCompleto();
        propuesto = unaCopiaDe(actual);
    }

    private void dadoUnNegocioGuardadoDeOtroIdYUnaPropuestaSinVentas() {
        dadoUnaPropuestaSinLaSeccionDeVentas();
        actual = unSistemaConOtroId(actual);
    }

    private void dadoUnaPropuestaSinLaSeccionDeVentas() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        quitarLaSeccion(propuesto, laSeccionDe(propuesto, TipoBloque.VENTAS));
    }

    private void dadoUnNegocioConDosStockYUnaPropuestaConUno() {
        actual = unSistemaCon(unaSeccionVaciaDe(TipoBloque.INVENTARIO), unaSeccionVaciaDe(TipoBloque.INVENTARIO));
        propuesto = unaCopiaDe(actual);
        quitarLaSeccion(propuesto, propuesto.proyecto().getBloques().get(1));
    }

    private void dadoUnNegocioConDosStockYUnaPropuestaDondeUnoPasaAFormulario() {
        actual = unSistemaCon(unaSeccionVaciaDe(TipoBloque.INVENTARIO), unaSeccionVaciaDe(TipoBloque.INVENTARIO));
        propuesto = unaCopiaDe(actual);
        cambiarElTipoDeLaSeccion(propuesto.proyecto().getBloques().get(0), TipoBloque.FORMULARIO);
    }

    private void dadoUnNegocioConUnaNotaQuePasaAFormulario() {
        actual = unSistemaCon(unaSeccionVaciaDe(TipoBloque.NOTA));
        propuesto = unaCopiaDe(actual);
        cambiarElTipoDeLaSeccion(propuesto.proyecto().getBloques().get(0), TipoBloque.FORMULARIO);
    }

    private void dadoUnNegocioGuardadoYUnaPropuestaSinLaColumnaDeCosto() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        quitarLaColumna(laSeccionDe(propuesto, TipoBloque.INVENTARIO), "cost");
    }

    private void dadoUnNegocioConUnaColumnaPropiaQueLaPropuestaQuita() {
        actual = unSistemaCompleto();
        Bloque stock = laSeccionDe(actual, TipoBloque.INVENTARIO);
        agregarLaColumna(stock, unaColumnaPropia(stock, "marca", "Marca", TipoCampo.TEXTO));
        propuesto = unaCopiaDe(actual);
        quitarLaColumna(laSeccionDe(propuesto, TipoBloque.INVENTARIO), "marca");
    }

    private void dadoUnNegocioConElPrecioEnTextoYLaMismaPropuesta() {
        actual = unSistemaCompleto();
        cambiarElTipoDeLaColumna(laSeccionDe(actual, TipoBloque.INVENTARIO), "price", TipoCampo.TEXTO);
        propuesto = unaCopiaDe(actual);
    }

    private void dadoUnaPropuestaConElPrecioEnTexto() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        cambiarElTipoDeLaColumna(laSeccionDe(propuesto, TipoBloque.INVENTARIO), "price", TipoCampo.TEXTO);
    }

    private void dadoUnaPropuestaDondeLaCantidadYaNoEsObligatoria() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        dejarDeExigirLaColumna(laSeccionDe(propuesto, TipoBloque.INVENTARIO), "quantity");
    }

    private void dadoUnaPropuestaSinLaCategoriaMercaderia() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        conLasOpciones(laSeccionDe(propuesto, TipoBloque.GASTOS), "category", List.of("Servicios", "Alquiler"));
    }

    private void dadoUnaPropuestaConElProductoApuntandoAClientes() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        conLaConexionA(laSeccionDe(propuesto, TipoBloque.VENTAS), "product",
                laSeccionDe(propuesto, TipoBloque.CLIENTES).getId());
    }

    private void dadoUnaPropuestaConElPrecioRenombrado() {
        dadoUnNegocioGuardadoYUnaPropuestaIgual();
        renombrarLaColumna(laSeccionDe(propuesto, TipoBloque.INVENTARIO), "price", "Precio de venta");
    }

    private void dadoUnaPropuestaConLaColumnaPropiaEnOtroTipo() {
        dadoUnNegocioConUnaColumnaPropiaQueLaPropuestaQuita();
        Bloque stock = laSeccionDe(propuesto, TipoBloque.INVENTARIO);
        agregarLaColumna(stock, unaColumnaPropia(stock, "marca", "Marca", TipoCampo.MONEDA));
    }

    private void cuandoSeValida() {
        ProteccionDeEstructura.validar(List.of(actual), propuesto);
    }

    private void cuandoSeValidaContraNingunNegocioGuardado() {
        ProteccionDeEstructura.validar(List.of(), propuesto);
    }

    private void cuandoSeIntentaValidar() {
        rechazo = catchThrowable(this::cuandoSeValida);
    }

    private void cuandoSeIntentaQuitarLaSeccionDeStock() {
        rechazo = catchThrowable(() -> ProteccionDeEstructura.exigirQueSePuedaQuitarLaSeccion(
                actual.proyecto().getBloques().get(0), actual.proyecto().getBloques()));
    }

    private void cuandoSeIntentaQuitarLaColumna(String clave) {
        Bloque stock = laSeccionDe(actual, TipoBloque.INVENTARIO);
        rechazo = catchThrowable(() -> ProteccionDeEstructura.exigirQueSePuedaQuitarLaColumna(
                stock, laColumna(stock, clave)));
    }

    private void cuandoSeIntentaCambiarLaColumna(String clave) {
        Bloque anterior = laSeccionDe(actual, TipoBloque.INVENTARIO);
        Bloque nuevo = laSeccionDe(propuesto, TipoBloque.INVENTARIO);
        Campo editado = laColumna(nuevo, clave);
        rechazo = catchThrowable(() -> ProteccionDeEstructura.exigirQueSePuedaCambiarLaColumna(
                anterior, laColumna(anterior, clave), editado, propuesto.proyecto().getBloques()));
    }

    private void entoncesSeAcepta() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class).hasMessage(mensaje);
    }
}

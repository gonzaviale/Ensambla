package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.SistemasDePrueba.agregarLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.cambiarElTipoDeLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.dejarDeExigirLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.laColumna;
import static com.ensambla.backend.support.SistemasDePrueba.laSeccionDe;
import static com.ensambla.backend.support.SistemasDePrueba.quitarLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.tieneLaColumna;
import static com.ensambla.backend.support.SistemasDePrueba.unaColumnaPropia;
import static com.ensambla.backend.support.SistemasDePrueba.unaSeccionVaciaDe;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaCon;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaCompleto;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaSinPestanas;
import static com.ensambla.backend.support.SistemasDePrueba.unSistemaVacio;
import static org.assertj.core.api.Assertions.assertThat;

class EstructuraFijaTest {

    private Sistema sistema;
    private Sistema completo;
    private Bloque stock;

    @Test
    @DisplayName("Un negocio vacío recibe Stock, Clientes, Ventas y Gastos en la primera pestaña")
    void unNegocioVacioRecibeLasCuatroSeccionesObligatoriasEnLaPrimeraPestana() {
        dadoUnNegocioVacio();

        cuandoSeCompleta();

        entoncesTieneLasCuatroSeccionesEnLaPrimeraPestana();
    }

    @Test
    @DisplayName("Un negocio sin pestañas recibe las secciones sin pestaña")
    void unNegocioSinPestanasRecibeLasSeccionesSinPestana() {
        dadoUnNegocioSinPestanas();

        cuandoSeCompleta();

        entoncesLasSeccionesNoTienenPestana();
    }

    @Test
    @DisplayName("Las secciones nuevas no traen filas")
    void lasSeccionesNuevasNoTraenFilas() {
        dadoUnNegocioVacio();

        cuandoSeCompleta();

        entoncesCadaSeccionNuevaTieneUnaListaDeFilasVacia();
    }

    @Test
    @DisplayName("Cada sección nueva lleva las columnas fijas de su tipo")
    void cadaSeccionNuevaLlevaLasColumnasFijasDeSuTipo() {
        dadoUnNegocioVacio();

        cuandoSeCompleta();

        entoncesLaSeccionDeStockTieneSusNueveColumnas();
    }

    @Test
    @DisplayName("Las conexiones de Ventas apuntan a Stock")
    void lasConexionesDeVentasApuntanAStock() {
        dadoUnNegocioVacio();

        cuandoSeCompleta();

        entoncesElProductoDeVentasApuntaAStock();
    }

    @Test
    @DisplayName("Un negocio completo no cambia al completarlo")
    void unNegocioCompletoNoCambiaAlCompletarlo() {
        dadoUnNegocioCompleto();

        cuandoSeCompleta();

        entoncesSigueTeniendoLosMismosBloquesYColumnas();
    }

    @Test
    @DisplayName("Un negocio completo está completo y uno vacío no")
    void unNegocioCompletoEstaCompletoYUnoVacioNo() {
        dadoUnNegocioCompleto();

        entoncesEstaCompletoElNegocioPeroNoElVacio();
    }

    @Test
    @DisplayName("Un negocio al que le falta una columna no está completo")
    void unNegocioAlQueLeFaltaUnaColumnaNoEstaCompleto() {
        dadoUnNegocioCompletoSinLaColumnaDeCosto();

        entoncesNoEstaCompleto();
    }

    @Test
    @DisplayName("Un negocio con una columna que no cumple no está completo")
    void unNegocioConUnaColumnaQueNoCumpleNoEstaCompleto() {
        dadoUnNegocioCompletoConElPrecioEnTexto();

        entoncesNoEstaCompleto();
    }

    @Test
    @DisplayName("La columna que falta se agrega al final y sin tocar las otras")
    void laColumnaQueFaltaSeAgregaAlFinalYSinTocarLasOtras() {
        dadoUnNegocioCompletoSinLaColumnaDeCosto();

        cuandoSeCompleta();

        entoncesElCostoVuelveAlFinalDeStock();
    }

    @Test
    @DisplayName("Una columna que no era obligatoria pasa a serlo sin perder su id")
    void unaColumnaQueNoEraObligatoriaPasaASerloSinPerderSuId() {
        dadoUnNegocioCompletoConLaCantidadOpcional();

        cuandoSeCompleta();

        entoncesLaCantidadEsObligatoriaYConservaSuId();
    }

    @Test
    @DisplayName("Una columna de la persona con una clave reservada y otro tipo se conserva como anterior")
    void unaColumnaDeLaPersonaConUnaClaveReservadaYOtroTipoSeConservaComoAnterior() {
        dadoUnNegocioCompletoConElPrecioEnTexto();

        cuandoSeCompleta();

        entoncesElPrecioEnTextoQuedaComoAnteriorYHayUnPrecioDeMoneda();
    }

    @Test
    @DisplayName("La columna repuesta no repite el id de la que se apartó")
    void laColumnaRepuestaNoRepiteElIdDeLaQueSeAparto() {
        dadoUnNegocioConElPrecioEnTextoConElIdQueLeCorrespondeALaClave();

        cuandoSeCompleta();

        entoncesLasColumnasDeStockTienenIdsDistintos();
    }

    @Test
    @DisplayName("Si ya hay una columna anterior, la que se aparta lleva otra clave y no la repite")
    void siYaHayUnaColumnaAnteriorLaQueSeApartaLlevaOtraClaveYNoLaRepite() {
        dadoUnNegocioConUnPrecioAnteriorYOtroPrecioEnTexto();

        cuandoSeCompleta();

        entoncesLasClavesDeStockNoSeRepitenYLasDosAnterioresSeConservan();
    }

    @Test
    @DisplayName("Una conexión que apunta a la sección equivocada se aparta y se repone")
    void unaConexionQueApuntaALaSeccionEquivocadaSeApartaYSeRepone() {
        dadoUnNegocioCompletoConElProductoDeVentasApuntandoAClientes();

        cuandoSeCompleta();

        entoncesElProductoApuntaAStockYLaAnteriorQuedaApartada();
    }

    @Test
    @DisplayName("Una columna propia con otra clave no se toca")
    void unaColumnaPropiaConOtraClaveNoSeToca() {
        dadoUnNegocioCompletoConUnaColumnaPropiaEnStock();

        cuandoSeCompleta();

        entoncesLaColumnaPropiaSigueIgual();
    }

    @Test
    @DisplayName("Una sección que no es de un tipo obligatorio se puede quitar")
    void unaSeccionQueNoEsDeUnTipoObligatorioSePuedeQuitar() {
        dadoUnNegocioCompleto();

        entoncesSePuedeQuitarUnaNotaPeroNoElUnicoStock();
    }

    @Test
    @DisplayName("Se puede quitar una sección obligatoria si hay otra de su tipo")
    void sePuedeQuitarUnaSeccionObligatoriaSiHayOtraDeSuTipo() {
        dadoUnNegocioCompletoConOtroStock();

        entoncesSePuedenQuitarLosDosStock();
    }

    @Test
    @DisplayName("Las columnas fijas se distinguen de las propias")
    void lasColumnasFijasSeDistinguenDeLasPropias() {
        dadoUnNegocioCompletoConUnaColumnaPropiaEnStock();

        entoncesSoloElCostoEsUnaColumnaFija();
    }

    private void dadoUnNegocioVacio() {
        sistema = unSistemaVacio();
    }

    private void dadoUnNegocioSinPestanas() {
        sistema = unSistemaSinPestanas();
    }

    private void dadoUnNegocioCompleto() {
        sistema = unSistemaCompleto();
        stock = laSeccionDe(sistema, TipoBloque.INVENTARIO);
    }

    private void dadoUnNegocioCompletoSinLaColumnaDeCosto() {
        dadoUnNegocioCompleto();
        quitarLaColumna(stock, "cost");
    }

    private void dadoUnNegocioCompletoConElPrecioEnTexto() {
        dadoUnNegocioCompleto();
        cambiarElTipoDeLaColumna(stock, "price", TipoCampo.TEXTO);
    }

    private void dadoUnNegocioConUnPrecioAnteriorYOtroPrecioEnTexto() {
        dadoUnNegocioCompletoConElPrecioEnTexto();
        agregarLaColumna(stock, unaColumnaPropia(stock, "price_anterior", "Precio (anterior)", TipoCampo.TEXTO));
    }

    private void dadoUnNegocioCompletoConLaCantidadOpcional() {
        dadoUnNegocioCompleto();
        dejarDeExigirLaColumna(stock, "quantity");
    }

    private void dadoUnNegocioConElPrecioEnTextoConElIdQueLeCorrespondeALaClave() {
        dadoUnNegocioCompleto();
        quitarLaColumna(stock, "price");
        agregarLaColumna(stock, precioEnTextoConElIdDeLaClave());
    }

    private void dadoUnNegocioCompletoConElProductoDeVentasApuntandoAClientes() {
        dadoUnNegocioCompleto();
        Bloque ventas = laSeccionDe(sistema, TipoBloque.VENTAS);
        quitarLaColumna(ventas, "product");
        agregarLaColumna(ventas, unaConexionDeProductoA(ventas, laSeccionDe(sistema, TipoBloque.CLIENTES)));
    }

    private void dadoUnNegocioCompletoConUnaColumnaPropiaEnStock() {
        dadoUnNegocioCompleto();
        agregarLaColumna(stock, unaColumnaPropia(stock, "marca", "Marca", TipoCampo.TEXTO));
    }

    private void dadoUnNegocioCompletoConOtroStock() {
        sistema = unSistemaCon(unaSeccionVaciaDe(TipoBloque.INVENTARIO), unaSeccionVaciaDe(TipoBloque.INVENTARIO),
                unaSeccionVaciaDe(TipoBloque.NOTA));
    }

    private void cuandoSeCompleta() {
        completo = EstructuraFija.completar(sistema, AHORA);
    }

    private Campo precioEnTextoConElIdDeLaClave() {
        return Campo.builder().id(IdSistema.aId(stock.getId(), "price", CampoId::de)).bloqueId(stock.getId())
                .nombre("Precio").tipo(TipoCampo.TEXTO).obligatorio(false).clave("price")
                .posicion((short) stock.getCampos().size()).build();
    }

    private Campo unaConexionDeProductoA(Bloque ventas, Bloque destino) {
        return Campo.builder().id(CampoId.nuevo()).bloqueId(ventas.getId()).nombre("Producto").clave("product")
                .tipo(TipoCampo.RELACION).obligatorio(true).bloqueRelacionadoId(destino.getId())
                .posicion((short) ventas.getCampos().size()).build();
    }

    private void entoncesTieneLasCuatroSeccionesEnLaPrimeraPestana() {
        assertThat(completo.proyecto().getBloques()).extracting(Bloque::getTipo)
                .containsExactly(TipoBloque.INVENTARIO, TipoBloque.CLIENTES, TipoBloque.VENTAS, TipoBloque.GASTOS);
        assertThat(completo.proyecto().getBloques()).extracting(Bloque::getSeccionId)
                .containsOnly(sistema.secciones().get(0).getId());
    }

    private void entoncesLasSeccionesNoTienenPestana() {
        assertThat(completo.proyecto().getBloques()).extracting(Bloque::getSeccionId).containsOnlyNulls();
    }

    private void entoncesCadaSeccionNuevaTieneUnaListaDeFilasVacia() {
        assertThat(completo.registrosPorBloque()).hasSize(4).allSatisfy((id, filas) -> assertThat(filas).isEmpty());
    }

    private void entoncesLaSeccionDeStockTieneSusNueveColumnas() {
        assertThat(laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos()).extracting(Campo::getClave)
                .containsExactly("name", "quantity", "price", "cost", "minimum", "barcode", "saleMode", "favorite",
                        "stockControl");
    }

    private void entoncesElProductoDeVentasApuntaAStock() {
        Campo producto = laColumna(laSeccionDe(completo, TipoBloque.VENTAS), "product");
        assertThat(producto.getBloqueRelacionadoId()).isEqualTo(laSeccionDe(completo, TipoBloque.INVENTARIO).getId());
    }

    private void entoncesSigueTeniendoLosMismosBloquesYColumnas() {
        assertThat(completo.proyecto().getBloques()).hasSize(4);
        assertThat(laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos()).hasSize(9);
    }

    private void entoncesEstaCompletoElNegocioPeroNoElVacio() {
        assertThat(EstructuraFija.estaCompleta(sistema)).isTrue();
        assertThat(EstructuraFija.estaCompleta(unSistemaVacio())).isFalse();
    }

    private void entoncesNoEstaCompleto() {
        assertThat(EstructuraFija.estaCompleta(sistema)).isFalse();
    }

    private void entoncesElCostoVuelveAlFinalDeStock() {
        List<Campo> columnas = laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos();
        assertThat(columnas).extracting(Campo::getClave).endsWith("cost");
        assertThat(columnas).hasSize(9);
    }

    private void entoncesLaCantidadEsObligatoriaYConservaSuId() {
        Campo cantidad = laColumna(laSeccionDe(completo, TipoBloque.INVENTARIO), "quantity");
        assertThat(cantidad.esObligatorio()).isTrue();
        assertThat(cantidad.getId()).isEqualTo(laColumna(stock, "quantity").getId());
    }

    private void entoncesElPrecioEnTextoQuedaComoAnteriorYHayUnPrecioDeMoneda() {
        List<Campo> columnas = laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos();
        assertThat(columnas).filteredOn(campo -> "price_anterior".equals(campo.getClave()))
                .singleElement().satisfies(anterior -> {
                    assertThat(anterior.getNombre()).isEqualTo("Precio (anterior)");
                    assertThat(anterior.getTipo()).isEqualTo(TipoCampo.TEXTO);
                });
        assertThat(laColumna(laSeccionDe(completo, TipoBloque.INVENTARIO), "price").getTipo())
                .isEqualTo(TipoCampo.MONEDA);
    }

    private void entoncesLasClavesDeStockNoSeRepitenYLasDosAnterioresSeConservan() {
        List<Campo> columnas = laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos();
        assertThat(columnas).extracting(Campo::getClave).doesNotHaveDuplicates()
                .contains("price", "price_anterior", "price_anterior_2");
        assertThat(columnas).extracting(Campo::getNombre).contains("Precio (anterior)", "Precio (anterior 2)");
    }

    private void entoncesLasColumnasDeStockTienenIdsDistintos() {
        assertThat(laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos()).extracting(Campo::getId)
                .doesNotHaveDuplicates();
    }

    private void entoncesElProductoApuntaAStockYLaAnteriorQuedaApartada() {
        Bloque ventas = laSeccionDe(completo, TipoBloque.VENTAS);
        assertThat(laColumna(ventas, "product").getBloqueRelacionadoId())
                .isEqualTo(laSeccionDe(completo, TipoBloque.INVENTARIO).getId());
        assertThat(tieneLaColumna(ventas, "product_anterior")).isTrue();
    }

    private void entoncesLaColumnaPropiaSigueIgual() {
        Campo marca = laColumna(laSeccionDe(completo, TipoBloque.INVENTARIO), "marca");
        assertThat(marca.getNombre()).isEqualTo("Marca");
        assertThat(laSeccionDe(completo, TipoBloque.INVENTARIO).getCampos()).hasSize(10);
    }

    private void entoncesSePuedeQuitarUnaNotaPeroNoElUnicoStock() {
        Bloque nota = unaSeccionVaciaDe(TipoBloque.NOTA);
        assertThat(EstructuraFija.sePuedeQuitar(nota, sistema.proyecto().getBloques())).isTrue();
        assertThat(EstructuraFija.sePuedeQuitar(stock, sistema.proyecto().getBloques())).isFalse();
    }

    private void entoncesSePuedenQuitarLosDosStock() {
        List<Bloque> bloques = sistema.proyecto().getBloques();
        assertThat(bloques).filteredOn(bloque -> bloque.getTipo() == TipoBloque.INVENTARIO)
                .allMatch(bloque -> EstructuraFija.sePuedeQuitar(bloque, bloques));
    }

    private void entoncesSoloElCostoEsUnaColumnaFija() {
        assertThat(EstructuraFija.esColumnaFija(stock, laColumna(stock, "cost"))).isTrue();
        assertThat(EstructuraFija.esColumnaFija(stock, laColumna(stock, "marca"))).isFalse();
    }
}

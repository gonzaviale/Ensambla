package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.MapasDePrueba.mapa;
import static org.assertj.core.api.Assertions.assertThat;

/** editar_campo: cambiar la etiqueta, el tipo, las opciones, si es obligatorio o el lugar de un dato que ya existe. */
class ConversarEnTallerEdicionTest {

    private static final EstadoTaller ESTADO = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of(
                    new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                    new CampoTaller("Precio", "currency"))),
            new PiezaTaller("e2", "sales", "Ventas", null, List.of(
                    new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number"),
                    new CampoTaller("Precio", "currency"), new CampoTaller("Cliente", "relation"))),
            new PiezaTaller("e3", "clients", "Clientes", null, List.of(
                    new CampoTaller("Nombre", "text"), new CampoTaller("Teléfono", "tel"),
                    new CampoTaller("Rubro", "select")))));

    private TallerDePrueba taller;
    private List<LlamadaHerramienta> llamadas;
    private ValidacionTaller validacion;

    @BeforeEach
    void setUp() {
        taller = new TallerDePrueba();
    }

    @Test
    @DisplayName("Renombra un dato buscando la pieza por nombre y el dato sin importar mayúsculas")
    void renombraUnDatoBuscandoLaPiezaPorNombreYElDatoSinImportarMayusculas() {
        dadoQueSePideRenombrarElTelefonoDeClientesComoCelular();

        cuandoSeValidanLasLlamadas();

        entoncesElTelefonoDeClientesQuedaRenombradoComoCelular();
    }

    @Test
    @DisplayName("No renombra un dato fijo del Stock")
    void noRenombraUnDatoFijoDelStock() {
        dadoQueSePideRenombrarElPrecioDelStockComoValor();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("No cambia el tipo de un dato fijo del Stock")
    void noCambiaElTipoDeUnDatoFijoDelStock() {
        dadoQueSePidePasarElPrecioATipoTexto();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Puede cambiar sólo las mayúsculas de la etiqueta del mismo dato")
    void puedeCambiarSoloLasMayusculasDeLaEtiquetaDelMismoDato() {
        dadoQueSePideEscribirElRubroEnMayusculas();

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(editada("pieza", "e3", "campo", "Rubro", "etiqueta", "RUBRO"));
    }

    @Test
    @DisplayName("Cambia el tipo de un dato a uno del catálogo sin importar mayúsculas")
    void cambiaElTipoDeUnDatoAUnoDelCatalogoSinImportarMayusculas() {
        dadoQueSePidePasarElPrecioATipoNumberEscritoEnMayusculas();

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(editada("pieza", "e3", "campo", "Teléfono", "tipo", "text"));
    }

    @Test
    @DisplayName("Pasa un dato a lista con sus opciones sin repetir y se queda con las primeras seis")
    void pasaUnDatoAListaConSusOpcionesSinRepetirYSeQuedaConLasPrimerasSeis() {
        dadoQueSePidePasarLaCantidadAListaConOpcionesRepetidasYDeMas();

        cuandoSeValidanLasLlamadas();

        entoncesLaCantidadQuedaComoListaConLasPrimerasSeisOpcionesSinRepetir();
    }

    @Test
    @DisplayName("Descarta pasar un dato a lista sin opciones")
    void descartaPasarUnDatoAListaSinOpciones() {
        dadoLasLlamadas(editar("pieza", "e3", "campo", "Teléfono", "tipo", "select"));

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Descarta pasar un dato a lista con una sola opción")
    void descartaPasarUnDatoAListaConUnaSolaOpcion() {
        dadoLasLlamadas(editar("pieza", "e3", "campo", "Teléfono", "tipo", "select", "opciones", List.of("Una")));

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("En un dato que ya es lista, las opciones válidas reemplazan a las que tenía y las inválidas descartan")
    void enUnDatoQueYaEsListaLasOpcionesValidasReemplazanALasQueTeniaYLasInvalidasDescartan() {
        dadoQueSePidenOpcionesInvalidasYValidasParaElRubro();

        cuandoSeValidanLasLlamadas();

        entoncesSoloSeReemplazanLasOpcionesDelRubroPorLasValidas();
    }

    @Test
    @DisplayName("Renombra un dato que ya es lista sin volver a pedir sus opciones")
    void renombraUnDatoQueYaEsListaSinVolverAPedirSusOpciones() {
        dadoLasLlamadas(editar("pieza", "e3", "campo", "Rubro", "etiqueta", "Tipo"));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(editada("pieza", "e3", "campo", "Rubro", "etiqueta", "Tipo"));
    }

    @Test
    @DisplayName("Ignora las opciones en un dato que no es lista y marca si es obligatorio")
    void ignoraLasOpcionesEnUnDatoQueNoEsListaYMarcaSiEsObligatorio() {
        dadoQueSePideMarcarObligatoriosDatosConYSinOpciones();

        cuandoSeValidanLasLlamadas();

        entoncesSeMarcanLosObligatoriosSinOpciones();
    }

    @Test
    @DisplayName("Mueve un dato a la posición pedida, que empieza en 1 y llega como número del modelo")
    void mueveUnDatoALaPosicionPedidaQueEmpiezaEn1YLlegaComoNumeroDelModelo() {
        dadoLasLlamadas(editar("pieza", "sales", "campo", "Cliente", "posicion", 2.0));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(editada("pieza", "e2", "campo", "Cliente", "posicion", 2));
    }

    @Test
    @DisplayName("Acota la posición a la cantidad de datos de la pieza")
    void acotaLaPosicionALaCantidadDeDatosDeLaPieza() {
        dadoQueSePidenPosicionesFueraDeRango();

        cuandoSeValidanLasLlamadas();

        entoncesLasPosicionesQuedanAcotadas();
    }

    @Test
    @DisplayName("Descarta una posición que no es un entero o que deja el dato donde estaba")
    void descartaUnaPosicionQueNoEsUnEnteroOQueDejaElDatoDondeEstaba() {
        dadoQueSePidenPosicionesInvalidasOQueNoMueven();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("No cambia el tipo de un vínculo ni del primer dato: esa parte se ignora y, si no queda nada, se descarta")
    void noCambiaElTipoDeUnVinculoNiDelPrimerDatoYSiNoQuedaNadaSeDescarta() {
        dadoQueSePideCambiarElTipoDeVinculosYDelPrimerDato();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Descarta una pieza o un dato inexistente, o una llamada sin dato")
    void descartaUnaPiezaOUnDatoInexistenteOUnaLlamadaSinDato() {
        dadoQueSePideEditarPiezasYDatosInexistentes();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Descarta una etiqueta repetida, muy larga o que no es texto")
    void descartaUnaEtiquetaRepetidaMuyLargaOQueNoEsTexto() {
        dadoQueSePidenEtiquetasInvalidas();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Descarta un tipo que no es del catálogo, un vínculo o un obligatorio que no es sí o no")
    void descartaUnTipoQueNoEsDelCatalogoUnVinculoOUnObligatorioQueNoEsSiONo() {
        dadoQueSePidenTiposYObligatoriosInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Descarta la llamada si no cambia nada: sin partes, en blanco o con los mismos valores")
    void descartaLaLlamadaSiNoCambiaNadaSinPartesEnBlancoOConLosMismosValores() {
        dadoQueSePidenEdicionesQueNoCambianNada();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Las llamadas siguientes del turno ven la etiqueta, el tipo y el lugar nuevos del dato")
    void lasLlamadasSiguientesDelTurnoVenLaEtiquetaElTipoYElLugarNuevosDelDato() {
        dadoVariasEdicionesEncadenadasDelPrecioEnElMismoTurno();

        cuandoSeValidanLasLlamadas();

        entoncesLasEdicionesVenLosCambiosAnterioresDelTurno();
    }

    @Test
    @DisplayName("Edita un dato de una pieza que se sumó en el mismo turno y la nombra por su tipo")
    void editaUnDatoDeUnaPiezaQueSeSumoEnElMismoTurnoYLaNombraPorSuTipo() {
        dadoQueSeSumaUnaPiezaDeGastosYSeEditaUnDatoSuyoEnElMismoTurno();

        cuandoSeValidanLasLlamadas();

        entoncesSeEditaElDatoDeLaPiezaNuevaDeGastos();
    }

    // --- dado ---

    private void dadoQueSePideRenombrarElTelefonoDeClientesComoCelular() {
        dadoLasLlamadas(editar("pieza", "Clientes", "campo", " teléfono ", "etiqueta", " Celular "));
    }

    private void dadoQueSePideRenombrarElPrecioDelStockComoValor() {
        dadoLasLlamadas(editar("pieza", "e1", "campo", "Precio", "etiqueta", "Valor"));
    }

    private void dadoQueSePideEscribirElRubroEnMayusculas() {
        dadoLasLlamadas(editar("pieza", "e3", "campo", "Rubro", "etiqueta", "RUBRO"));
    }

    private void dadoQueSePidePasarElPrecioATipoNumberEscritoEnMayusculas() {
        dadoLasLlamadas(editar("pieza", "e3", "campo", "teléfono", "tipo", " TEXT "));
    }

    private void dadoQueSePidePasarElPrecioATipoTexto() {
        dadoLasLlamadas(editar("pieza", "e1", "campo", "Precio", "tipo", "text"));
    }

    private void dadoQueSePidePasarLaCantidadAListaConOpcionesRepetidasYDeMas() {
        dadoLasLlamadas(editar("pieza", "e3", "campo", "Teléfono", "tipo", "select",
                "opciones", List.of("XS", "S", "XS", "M", "L", "XL", "XXL", "3XL")));
    }

    private void dadoQueSePidenOpcionesInvalidasYValidasParaElRubro() {
        dadoLasLlamadas(
                editar("pieza", "e3", "campo", "Rubro", "opciones", List.of("Sola")),
                editar("pieza", "e3", "campo", "Rubro", "tipo", "select", "opciones", List.of("A", "B")));
    }

    private void dadoQueSePideMarcarObligatoriosDatosConYSinOpciones() {
        dadoLasLlamadas(
                editar("pieza", "e1", "campo", "Precio", "opciones", List.of("A", "B"), "obligatorio", true),
                editar("pieza", "e1", "campo", "Cantidad", "obligatorio", false));
    }

    private void dadoQueSePidenPosicionesFueraDeRango() {
        dadoLasLlamadas(
                editar("pieza", "e1", "campo", "Precio", "posicion", 0),
                editar("pieza", "e1", "campo", "Precio", "posicion", 99));
    }

    private void dadoQueSePidenPosicionesInvalidasOQueNoMueven() {
        dadoLasLlamadas(
                editar("pieza", "e1", "campo", "Precio", "posicion", 1.5),
                editar("pieza", "e1", "campo", "Precio", "posicion", "2"),
                editar("pieza", "e1", "campo", "Precio", "posicion", 3));
    }

    private void dadoQueSePideCambiarElTipoDeVinculosYDelPrimerDato() {
        dadoLasLlamadas(
                editar("pieza", "e2", "campo", "Cliente", "tipo", "text"),
                editar("pieza", "e2", "campo", "Producto", "tipo", "relation"),
                editar("pieza", "e1", "campo", "Producto", "tipo", "select", "opciones", List.of("A", "B")));
    }

    private void dadoQueSePideEditarPiezasYDatosInexistentes() {
        dadoLasLlamadas(
                editar("pieza", "fantasma", "campo", "Precio", "etiqueta", "Costo"),
                editar("pieza", "e1", "campo", "Color", "etiqueta", "Costo"),
                editar("pieza", "e1", "etiqueta", "Costo"));
    }

    private void dadoQueSePidenEtiquetasInvalidas() {
        dadoLasLlamadas(
                editar("pieza", "e1", "campo", "Cantidad", "etiqueta", "precio"),
                editar("pieza", "e1", "campo", "Cantidad", "etiqueta", "x".repeat(61)),
                editar("pieza", "e1", "campo", "Cantidad", "etiqueta", 5));
    }

    private void dadoQueSePidenTiposYObligatoriosInvalidos() {
        dadoLasLlamadas(
                editar("pieza", "e3", "campo", "Teléfono", "tipo", "color"),
                editar("pieza", "e3", "campo", "Teléfono", "tipo", "relation"),
                editar("pieza", "e1", "campo", "Cantidad", "obligatorio", "sí"));
    }

    private void dadoQueSePidenEdicionesQueNoCambianNada() {
        dadoLasLlamadas(
                editar("pieza", "e1", "campo", "Cantidad"),
                editar("pieza", "e3", "campo", "Rubro", "etiqueta", " ", "opciones", List.of()),
                editar("pieza", "e1", "campo", "Cantidad", "etiqueta", "Cantidad", "tipo", "NUMBER"),
                editar("pieza", "e3", "campo", "Rubro", "tipo", "select"));
    }

    private void dadoVariasEdicionesEncadenadasDelPrecioEnElMismoTurno() {
        dadoLasLlamadas(
                editar("pieza", "e3", "campo", "Teléfono", "etiqueta", "Celular", "posicion", 3),
                editar("pieza", "e3", "campo", "Teléfono", "obligatorio", true),
                editar("pieza", "e3", "campo", "celular", "tipo", "text"),
                editar("pieza", "e3", "campo", "Celular", "tipo", "text"),
                llamada("quitar_campo", Map.of("pieza", "e3", "campo", "celular")));
    }

    private void dadoQueSeSumaUnaPiezaDeGastosYSeEditaUnDatoSuyoEnElMismoTurno() {
        dadoLasLlamadas(
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                editar("pieza", "Gastos", "campo", "importe", "posicion", 1));
    }

    private void dadoLasLlamadas(LlamadaHerramienta... pedidas) {
        llamadas = List.of(pedidas);
    }

    // --- cuando ---

    private void cuandoSeValidanLasLlamadas() {
        validacion = taller.validar(llamadas, ESTADO);
    }

    // --- entonces ---

    private void entoncesElTelefonoDeClientesQuedaRenombradoComoCelular() {
        entoncesSeAcepta(editada("pieza", "e3", "campo", "Teléfono", "etiqueta", "Celular"));
    }

    private void entoncesLaCantidadQuedaComoListaConLasPrimerasSeisOpcionesSinRepetir() {
        entoncesSeAcepta(editada("pieza", "e3", "campo", "Teléfono", "tipo", "select",
                "opciones", List.of("XS", "S", "M", "L", "XL", "XXL")));
    }

    private void entoncesSoloSeReemplazanLasOpcionesDelRubroPorLasValidas() {
        entoncesSeAcepta(editada("pieza", "e3", "campo", "Rubro", "opciones", List.of("A", "B")));
    }

    private void entoncesSeMarcanLosObligatoriosSinOpciones() {
        entoncesSeAcepta(
                editada("pieza", "e1", "campo", "Precio", "obligatorio", true),
                editada("pieza", "e1", "campo", "Cantidad", "obligatorio", false));
    }

    private void entoncesLasPosicionesQuedanAcotadas() {
        entoncesSeAcepta(
                editada("pieza", "e1", "campo", "Precio", "posicion", 1),
                editada("pieza", "e1", "campo", "Precio", "posicion", 3));
    }

    private void entoncesLasEdicionesVenLosCambiosAnterioresDelTurno() {
        entoncesSeAcepta(
                editada("pieza", "e3", "campo", "Teléfono", "etiqueta", "Celular", "posicion", 3),
                editada("pieza", "e3", "campo", "Celular", "tipo", "text"),
                new AccionTaller("quitar_campo", Map.of("pieza", "e3", "campo", "Celular")));
    }

    private void entoncesSeEditaElDatoDeLaPiezaNuevaDeGastos() {
        assertThat(validacion.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "editar_campo");
        assertThat(validacion.acciones().get(1))
                .isEqualTo(editada("pieza", "expenses", "campo", "Importe", "posicion", 1));
    }

    private void entoncesSeAcepta(AccionTaller... esperadas) {
        assertThat(validacion.acciones()).containsExactly(esperadas);
    }

    private void entoncesSeDescartaTodo() {
        assertThat(validacion.acciones()).isEmpty();
    }

    // --- datos ---

    private static LlamadaHerramienta editar(Object... argumentos) {
        return llamada("editar_campo", mapa(argumentos));
    }

    private static LlamadaHerramienta llamada(String nombre, Map<String, Object> argumentos) {
        return new LlamadaHerramienta(nombre, argumentos);
    }

    private static AccionTaller editada(Object... argumentos) {
        return new AccionTaller("editar_campo", mapa(argumentos));
    }
}

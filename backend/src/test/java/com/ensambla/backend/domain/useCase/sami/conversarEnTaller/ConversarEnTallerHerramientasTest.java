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

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.ensambla.backend.support.MapasDePrueba.mapa;
import static org.assertj.core.api.Assertions.assertThat;

class ConversarEnTallerHerramientasTest {

    private TallerDePrueba taller;
    private EstadoTaller estado;
    private List<LlamadaHerramienta> llamadas = List.of();
    private ValidacionTaller validacion;

    @BeforeEach
    void setUp() {
        taller = new TallerDePrueba();
        estado = new EstadoTaller("Mi almacén", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Inventario", null, List.of(
                        new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                        new CampoTaller("Precio", "currency"))),
                new PiezaTaller("e2", "clients", "Clientes", "General", List.of(new CampoTaller("Nombre", "text")))));
    }

    @Test
    @DisplayName("Ignora una herramienta que no existe y la cuenta como descartada")
    void ignoraUnaHerramientaQueNoExisteYLaCuentaComoDescartada() {
        dadoLasLlamadas(llamada("borrar_todo"));

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Sin llamadas no hay acciones ni opciones")
    void sinLlamadasNoHayAccionesNiOpciones() {
        dadoLasLlamadas();

        cuandoSeValidanLasLlamadas();

        entoncesNoHayAccionesNiOpciones();
    }

    @Test
    @DisplayName("Suma una pieza del catálogo sin importar mayúsculas")
    void sumaUnaPiezaDelCatalogoSinImportarMayusculas() {
        dadoLasLlamadas(llamada("agregar_pieza", "tipo", " EXPENSES "));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("agregar_pieza", "tipo", "expenses"));
    }

    @Test
    @DisplayName("No suma tipos inventados, ya presentes, vacíos ni que no sean texto")
    void noSumaTiposInventadosYaPresentesVaciosNiQueNoSeanTexto() {
        dadoQueSePidenPiezasConTiposInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("No suma dos veces el mismo tipo en un turno")
    void noSumaDosVecesElMismoTipoEnUnTurno() {
        dadoLasLlamadas(llamada("agregar_pieza", "tipo", "expenses"), llamada("agregar_pieza", "tipo", "expenses"));

        cuandoSeValidanLasLlamadas();

        entoncesSeAceptaUnaSolaAccion();
    }

    @Test
    @DisplayName("Quita una pieza por id, por nombre o por tipo y ya no la encuentra después")
    void quitaUnaPiezaPorIdPorNombreOPorTipoYYaNoLaEncuentraDespues() {
        dadoQueSeQuitaLaMismaPiezaPorIdPorNombreYPorTipo();

        cuandoSeValidanLasLlamadas();

        entoncesSeQuitanLasDosPiezasUnaSolaVezCadaUna();
    }

    @Test
    @DisplayName("Encuentra una pieza por su tipo aunque venga en mayúsculas")
    void encuentraUnaPiezaPorSuTipoAunqueVengaEnMayusculas() {
        dadoUnaPiezaDePedidosQueSePideQuitarPorSuTipoEnMayusculas();

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("quitar_pieza", "pieza", "e2"));
    }

    @Test
    @DisplayName("Quita por tipo una pieza que se sumó en el mismo turno")
    void quitaPorTipoUnaPiezaQueSeSumoEnElMismoTurno() {
        dadoQueSeSumaYSeQuitaLaPiezaDePedidosEnElMismoTurno();

        cuandoSeValidanLasLlamadas();

        entoncesSeQuitaLaPiezaDePedidosPorSuTipo();
    }

    @Test
    @DisplayName("No quita una pieza inexistente o sin referencia")
    void noQuitaUnaPiezaInexistenteOSinReferencia() {
        dadoLasLlamadas(llamada("quitar_pieza", "pieza", "fantasma"), llamada("quitar_pieza"));

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Renombra una pieza con un nombre válido")
    void renombraUnaPiezaConUnNombreValido() {
        dadoLasLlamadas(llamada("renombrar_pieza", "pieza", "Clientes", "nombre", " Mis socios "));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("renombrar_pieza", "pieza", "e2", "nombre", "Mis socios"));
    }

    @Test
    @DisplayName("No renombra sin nombre, con un nombre muy largo o una pieza inexistente")
    void noRenombraSinNombreConUnNombreMuyLargoOUnaPiezaInexistente() {
        dadoQueSePideRenombrarConDatosInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Renombra el sistema y rechaza un nombre vacío")
    void renombraElSistemaYRechazaUnNombreVacio() {
        dadoLasLlamadas(llamada("renombrar_sistema", "nombre", "Velas de Ana"),
                llamada("renombrar_sistema", "nombre", " "));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("renombrar_sistema", "nombre", "Velas de Ana"));
    }

    @Test
    @DisplayName("Suma un dato de texto a una pieza y no manda opciones")
    void sumaUnDatoDeTextoAUnaPiezaYNoMandaOpciones() {
        dadoLasLlamadas(llamada("agregar_campo", "pieza", "e1", "etiqueta", "Proveedor", "tipo", "TEXT",
                "opciones", List.of("ignorada")));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("agregar_campo", "pieza", "e1", "etiqueta", "Proveedor", "tipo", "text"));
    }

    @Test
    @DisplayName("Suma un dato de lista con sus opciones sin repetir")
    void sumaUnDatoDeListaConSusOpcionesSinRepetir() {
        dadoLasLlamadas(llamada("agregar_campo", "pieza", "e1", "etiqueta", "Rubro", "tipo", "select",
                "opciones", List.of("Insumos", "Terminados", "Insumos", " ")));

        cuandoSeValidanLasLlamadas();

        entoncesLasOpcionesDelDatoSon(List.of("Insumos", "Terminados"));
    }

    @Test
    @DisplayName("Rechaza datos sin etiqueta, de tipo inválido, repetidos o de lista con menos de dos opciones")
    void rechazaDatosSinEtiquetaDeTipoInvalidoRepetidosODeListaConMenosDeDosOpciones() {
        dadoQueSePidenDatosInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("No pasa de dieciséis datos por pieza")
    void noPasaDeDieciseisDatosPorPieza() {
        dadoUnaPiezaConDieciseisDatosYSePideUnoMas();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Un dato recién sumado cuenta: no se puede repetir ni se puede quitar en el mismo turno")
    void unDatoRecienSumadoCuentaNoSePuedeRepetirNiSePuedeQuitarEnElMismoTurno() {
        dadoQueSeSumaRepiteYQuitaElMismoDatoEnElMismoTurno();

        cuandoSeValidanLasLlamadas();

        entoncesSeSumaYSeQuitaElDatoUnaVez();
    }

    @Test
    @DisplayName("Quita un dato con la etiqueta exacta que tiene la pieza")
    void quitaUnDatoConLaEtiquetaExactaQueTieneLaPieza() {
        dadoUnaPiezaAMedidaConUnDatoPropio();

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("quitar_campo", "pieza", "e1", "campo", "Marca"));
    }

    @Test
    @DisplayName("No quita un dato obligatorio ni fijo del componente ni un vínculo")
    void noQuitaUnDatoObligatorioDelComponenteNiUnVinculo() {
        dadoUnaVentaConVinculoYUnaAgendaYSePideQuitarSusDatos();

        cuandoSeValidanLasLlamadas();

        entoncesSoloSeQuitanLosDatosOpcionales();
    }

    @Test
    @DisplayName("No quita la única pieza de Stock del negocio")
    void noQuitaLaUnicaPiezaDeStockDelNegocio() {
        dadoQueSePideQuitarLaUnicaPiezaDeStock();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Quita una pieza de Stock si hay otra del mismo tipo")
    void quitaUnaPiezaDeStockSiHayOtraDelMismoTipo() {
        dadoDosPiezasDeStockYSePideQuitarUna();

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("quitar_pieza", "pieza", "e2"));
    }

    @Test
    @DisplayName("No quita un dato fijo del Stock")
    void noQuitaUnDatoFijoDelStock() {
        dadoQueSePideQuitarUnDatoFijoDelStock();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("No quita un dato inexistente, sin nombre o de una pieza que no existe")
    void noQuitaUnDatoInexistenteSinNombreODeUnaPiezaQueNoExiste() {
        dadoQueSePideQuitarDatosInvalidos();

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    @Test
    @DisplayName("Separa una pieza en una sección con nombre y rechaza la que no lo tiene")
    void separaUnaPiezaEnUnaSeccionConNombreYRechazaLaQueNoLoTiene() {
        dadoQueSePideSepararConYSinNombreDeSeccion();

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("separar_en_seccion", "pieza", "e2", "seccion", "Mis clientes"));
    }

    @Test
    @DisplayName("Cambia la distribución sólo a una del catálogo")
    void cambiaLaDistribucionSoloAUnaDelCatalogo() {
        dadoLasLlamadas(llamada("cambiar_distribucion", "layout", "TWO"),
                llamada("cambiar_distribucion", "layout", "diagonal"),
                llamada("cambiar_distribucion"));

        cuandoSeValidanLasLlamadas();

        entoncesSeAcepta(accion("cambiar_distribucion", "layout", "two"));
    }

    @Test
    @DisplayName("Una pregunta con opciones válidas llega como botonera y no cuenta como acción")
    void unaPreguntaConOpcionesValidasLlegaComoBotoneraYNoCuentaComoAccion() {
        dadoLasLlamadas(llamada("ofrecer_opciones", "pregunta", " ¿Qué querés sumar? ",
                "opciones", List.of("Gastos", " Clientes ", "Gastos")));

        cuandoSeValidanLasLlamadas();

        entoncesLlegaUnaBotoneraConLasOpcionesSinRepetirYNingunaAccion();
    }

    @Test
    @DisplayName("Se queda con las primeras seis opciones")
    void seQuedaConLasPrimerasSeisOpciones() {
        dadoLasLlamadas(llamada("ofrecer_opciones", "pregunta", "¿Cuál?",
                "opciones", List.of("1", "2", "3", "4", "5", "6", "7", "8")));

        cuandoSeValidanLasLlamadas();

        entoncesLaBotoneraTieneLasPrimerasSeisOpciones();
    }

    @Test
    @DisplayName("Descarta preguntas sin texto, con menos de dos opciones o con opciones que no son una lista")
    void descartaPreguntasSinTextoConMenosDeDosOpcionesOConOpcionesQueNoSonUnaLista() {
        dadoQueSePidenPreguntasInvalidas();

        cuandoSeValidanLasLlamadas();

        entoncesNoHayBotonera();
    }

    @Test
    @DisplayName("Una pregunta inválida posterior no pisa a la anterior")
    void unaPreguntaInvalidaPosteriorNoPisaALaAnterior() {
        dadoUnaPreguntaValidaSeguidaDeUnaInvalida();

        cuandoSeValidanLasLlamadas();

        entoncesSoloQuedaLaPrimeraPregunta();
    }

    @Test
    @DisplayName("Aplica como mucho doce acciones por turno y descarta el resto")
    void aplicaComoMuchoDoceAccionesPorTurnoYDescartaElResto() {
        dadoCatorceCambiosDeDistribucion();

        cuandoSeValidanLasLlamadas();

        entoncesSeAplicanSoloDoceAcciones();
    }

    @Test
    @DisplayName("Busca una pieza primero por id, después por tipo y por último por nombre")
    void buscaUnaPiezaPrimeroPorIdDespuesPorTipoYPorUltimoPorNombre() {
        dadoPiezasCuyosNombresYTiposSeCruzan();

        cuandoSeValidanLasLlamadas();

        entoncesCadaReferenciaSeResuelvePorIdLuegoPorTipoYLuegoPorNombre();
    }

    @Test
    @DisplayName("Encuentra una pieza por su nombre sin importar tildes, mayúsculas ni plural")
    void encuentraUnaPiezaPorSuNombreSinImportarTildesMayusculasNiPlural() {
        dadoPiezasLlamadasQuienMeDebeYMaterial();

        cuandoSeValidanLasLlamadas();

        entoncesSeQuitanLasDosPiezasPorSuNombre();
    }

    @Test
    @DisplayName("Un dato de lista se queda con las primeras seis opciones")
    void unDatoDeListaSeQuedaConLasPrimerasSeisOpciones() {
        dadoLasLlamadas(llamada("agregar_campo", "pieza", "e1", "etiqueta", "Talle", "tipo", "select",
                "opciones", List.of("XS", "S", "M", "L", "XL", "XXL", "3XL")));

        cuandoSeValidanLasLlamadas();

        entoncesLasOpcionesDelDatoSon(List.of("XS", "S", "M", "L", "XL", "XXL"));
    }

    @Test
    @DisplayName("No suma un vínculo como dato común: eso es conectar")
    void noSumaUnVinculoComoDatoComunEsoEsConectar() {
        dadoLasLlamadas(llamada("agregar_campo", "pieza", "e1", "etiqueta", "Cliente", "tipo", "relation"));

        cuandoSeValidanLasLlamadas();

        entoncesSeDescartaTodo();
    }

    // --- dado ---

    private void dadoQueSePidenPiezasConTiposInvalidos() {
        dadoLasLlamadas(
                llamada("agregar_pieza", "tipo", "turnos"),
                llamada("agregar_pieza", "tipo", "inventory"),
                llamada("agregar_pieza", "tipo", "  "),
                llamada("agregar_pieza", "tipo", 5),
                llamada("agregar_pieza"));
    }

    private void dadoQueSeQuitaLaMismaPiezaPorIdPorNombreYPorTipo() {
        estado = new EstadoTaller("Pedidos", "mosaic", List.of(
                new PiezaTaller("e1", "orders", "Pedidos", null, List.of(new CampoTaller("Nombre", "text"))),
                new PiezaTaller("e2", "form", "Material", null, List.of(new CampoTaller("Nombre", "text")))));
        dadoLasLlamadas(
                llamada("quitar_pieza", "pieza", "e1"),
                llamada("quitar_pieza", "pieza", "MATERIAL"),
                llamada("quitar_pieza", "pieza", "orders"));
    }

    private void dadoUnaPiezaDePedidosQueSePideQuitarPorSuTipoEnMayusculas() {
        estado = new EstadoTaller("Pedidos", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Inventario", null, List.of(new CampoTaller("Producto", "text"))),
                new PiezaTaller("e2", "orders", "Pedidos", null, List.of(new CampoTaller("Nombre", "text")))));
        dadoLasLlamadas(llamada("quitar_pieza", "pieza", "ORDERS"));
    }

    private void dadoQueSeSumaYSeQuitaLaPiezaDePedidosEnElMismoTurno() {
        dadoLasLlamadas(llamada("agregar_pieza", "tipo", "orders"), llamada("quitar_pieza", "pieza", "Pedidos"));
    }

    private void dadoQueSePideQuitarLaUnicaPiezaDeStock() {
        dadoLasLlamadas(llamada("quitar_pieza", "pieza", "e1"));
    }

    private void dadoDosPiezasDeStockYSePideQuitarUna() {
        estado = new EstadoTaller("Dos stocks", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Inventario", null, List.of(new CampoTaller("Producto", "text"))),
                new PiezaTaller("e2", "inventory", "Depósito", null, List.of(new CampoTaller("Producto", "text")))));
        dadoLasLlamadas(llamada("quitar_pieza", "pieza", "e2"));
    }

    private void dadoQueSePideQuitarUnDatoFijoDelStock() {
        dadoLasLlamadas(llamada("quitar_campo", "pieza", "e1", "campo", "Precio"));
    }

    private void dadoUnaPiezaAMedidaConUnDatoPropio() {
        estado = new EstadoTaller("A medida", "mosaic", List.of(
                new PiezaTaller("e1", "form", "Material", null,
                        List.of(new CampoTaller("Nombre", "text"), new CampoTaller("Marca", "text")))));
        dadoLasLlamadas(llamada("quitar_campo", "pieza", "e1", "campo", "marca"));
    }

    private void dadoQueSePideRenombrarConDatosInvalidos() {
        dadoLasLlamadas(
                llamada("renombrar_pieza", "pieza", "e2"),
                llamada("renombrar_pieza", "pieza", "e2", "nombre", "x".repeat(61)),
                llamada("renombrar_pieza", "pieza", "nada", "nombre", "Otro"));
    }

    private void dadoQueSePidenDatosInvalidos() {
        dadoLasLlamadas(
                llamada("agregar_campo", "pieza", "e1", "tipo", "text"),
                llamada("agregar_campo", "pieza", "e1", "etiqueta", "X"),
                llamada("agregar_campo", "pieza", "e1", "etiqueta", "X", "tipo", "color"),
                llamada("agregar_campo", "pieza", "e1", "etiqueta", "producto", "tipo", "text"),
                llamada("agregar_campo", "pieza", "e1", "etiqueta", "Rubro", "tipo", "select",
                        "opciones", List.of("Una sola")),
                llamada("agregar_campo", "pieza", "nada", "etiqueta", "Y", "tipo", "text"));
    }

    private void dadoUnaPiezaConDieciseisDatosYSePideUnoMas() {
        List<CampoTaller> campos = IntStream.rangeClosed(1, 16)
                .mapToObj(numero -> new CampoTaller("Dato " + numero, "text")).toList();
        estado = new EstadoTaller("Lleno", "mosaic", List.of(
                new PiezaTaller("e9", "inventory", "Inventario", null, campos)));
        dadoLasLlamadas(llamada("agregar_campo", "pieza", "e9", "etiqueta", "Uno más", "tipo", "text"));
    }

    private void dadoQueSeSumaRepiteYQuitaElMismoDatoEnElMismoTurno() {
        dadoLasLlamadas(
                llamada("agregar_campo", "pieza", "e1", "etiqueta", "Proveedor", "tipo", "text"),
                llamada("agregar_campo", "pieza", "e1", "etiqueta", "PROVEEDOR", "tipo", "text"),
                llamada("quitar_campo", "pieza", "e1", "campo", "proveedor"));
    }

    private void dadoUnaVentaConVinculoYUnaAgendaYSePideQuitarSusDatos() {
        estado = new EstadoTaller("S", "mosaic", List.of(
                new PiezaTaller("e1", "sales", "Ventas", null, List.of(
                        new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number"),
                        new CampoTaller("Cliente", "text"))),
                new PiezaTaller("e2", "calendar", "Agenda", null, List.of(new CampoTaller("Cantidad", "number")))));
        dadoLasLlamadas(
                llamada("quitar_campo", "pieza", "e1", "campo", "Producto"),
                llamada("quitar_campo", "pieza", "e1", "campo", "Cantidad"),
                llamada("quitar_campo", "pieza", "e1", "campo", "Cliente"),
                llamada("quitar_campo", "pieza", "e2", "campo", "Cantidad"));
    }

    private void dadoQueSePideQuitarDatosInvalidos() {
        dadoLasLlamadas(
                llamada("quitar_campo", "pieza", "e1", "campo", "Color"),
                llamada("quitar_campo", "pieza", "e1"),
                llamada("quitar_campo", "pieza", "nada", "campo", "Producto"));
    }

    private void dadoQueSePideSepararConYSinNombreDeSeccion() {
        dadoLasLlamadas(
                llamada("separar_en_seccion", "pieza", "e2", "seccion", "Mis clientes"),
                llamada("separar_en_seccion", "pieza", "e2"),
                llamada("separar_en_seccion", "pieza", "nada", "seccion", "X"));
    }

    private void dadoQueSePidenPreguntasInvalidas() {
        dadoLasLlamadas(
                llamada("ofrecer_opciones", "opciones", List.of("A", "B")),
                llamada("ofrecer_opciones", "pregunta", "¿Cuál?", "opciones", List.of("Sola")),
                llamada("ofrecer_opciones", "pregunta", "¿Cuál?", "opciones", List.of("A", 5, " ")),
                llamada("ofrecer_opciones", "pregunta", "¿Cuál?", "opciones", "Gastos"));
    }

    private void dadoUnaPreguntaValidaSeguidaDeUnaInvalida() {
        dadoLasLlamadas(
                llamada("ofrecer_opciones", "pregunta", "¿Cuál?", "opciones", List.of("A", "B")),
                llamada("ofrecer_opciones", "pregunta", "¿Otra?", "opciones", List.of("A")));
    }

    private void dadoCatorceCambiosDeDistribucion() {
        llamadas = Collections.nCopies(14, llamada("cambiar_distribucion", "layout", "single"));
    }

    private void dadoPiezasCuyosNombresYTiposSeCruzan() {
        estado = new EstadoTaller("Cruzado", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Sales", null, List.of()),
                new PiezaTaller("e2", "sales", "Caja", null, List.of()),
                new PiezaTaller("e2b", "clients", "e2", null, List.of())));
        dadoLasLlamadas(
                llamada("separar_en_seccion", "pieza", "sales", "seccion", "Caja"),
                llamada("separar_en_seccion", "pieza", "e2", "seccion", "Otra"),
                llamada("separar_en_seccion", "pieza", "«Sales»", "seccion", "Depósito"));
    }

    private void dadoPiezasLlamadasQuienMeDebeYMaterial() {
        estado = new EstadoTaller("Velas", "mosaic", List.of(
                new PiezaTaller("e7", "orders", "Quién me debe", null, List.of()),
                new PiezaTaller("e8", "form", "Material", null, List.of())));
        dadoLasLlamadas(
                llamada("quitar_pieza", "pieza", "QUIEN ME DEBE"),
                llamada("quitar_pieza", "pieza", "materiales"));
    }

    private void dadoLasLlamadas(LlamadaHerramienta... pedidas) {
        llamadas = List.of(pedidas);
    }

    // --- cuando ---

    private void cuandoSeValidanLasLlamadas() {
        validacion = taller.validar(llamadas, estado);
    }

    // --- entonces ---

    private void entoncesSeDescartaTodo() {
        assertThat(validacion.acciones()).isEmpty();
    }

    private void entoncesNoHayAccionesNiOpciones() {
        assertThat(validacion.acciones()).isEmpty();
        assertThat(validacion.preguntas()).isEmpty();
    }

    private void entoncesSeAcepta(AccionTaller... esperadas) {
        assertThat(validacion.acciones()).containsExactly(esperadas);
    }

    private void entoncesSeAceptaUnaSolaAccion() {
        assertThat(validacion.acciones()).hasSize(1);
    }

    private void entoncesSeQuitanLasDosPiezasUnaSolaVezCadaUna() {
        entoncesSeAcepta(accion("quitar_pieza", "pieza", "e1"), accion("quitar_pieza", "pieza", "e2"));
    }

    private void entoncesSeQuitaLaPiezaDePedidosPorSuTipo() {
        assertThat(validacion.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "quitar_pieza");
        assertThat(validacion.acciones().get(1).argumentos()).containsEntry("pieza", "orders");
    }

    private void entoncesLasOpcionesDelDatoSon(List<String> opciones) {
        assertThat(validacion.acciones()).hasSize(1);
        assertThat(validacion.acciones().getFirst().argumentos()).containsEntry("opciones", opciones);
    }

    private void entoncesSeSumaYSeQuitaElDatoUnaVez() {
        assertThat(validacion.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_campo", "quitar_campo");
    }

    private void entoncesSoloSeQuitanLosDatosOpcionales() {
        entoncesSeAcepta(accion("quitar_campo", "pieza", "e2", "campo", "Cantidad"));
    }

    private void entoncesLlegaUnaBotoneraConLasOpcionesSinRepetirYNingunaAccion() {
        assertThat(validacion.acciones()).isEmpty();
        assertThat(validacion.preguntas()).hasSize(1);
        assertThat(validacion.preguntas().getFirst().pregunta()).isEqualTo("¿Qué querés sumar?");
        assertThat(validacion.preguntas().getFirst().opciones()).containsExactly("Gastos", "Clientes");
    }

    private void entoncesLaBotoneraTieneLasPrimerasSeisOpciones() {
        assertThat(validacion.preguntas().getFirst().opciones()).containsExactly("1", "2", "3", "4", "5", "6");
    }

    private void entoncesNoHayBotonera() {
        assertThat(validacion.preguntas()).isEmpty();
    }

    private void entoncesSoloQuedaLaPrimeraPregunta() {
        assertThat(validacion.preguntas()).extracting("pregunta").containsExactly("¿Cuál?");
    }

    private void entoncesSeAplicanSoloDoceAcciones() {
        assertThat(validacion.acciones()).hasSize(12);
    }

    private void entoncesCadaReferenciaSeResuelvePorIdLuegoPorTipoYLuegoPorNombre() {
        entoncesSeAcepta(
                accion("separar_en_seccion", "pieza", "e2", "seccion", "Caja"),
                accion("separar_en_seccion", "pieza", "e2", "seccion", "Otra"),
                accion("separar_en_seccion", "pieza", "e1", "seccion", "Depósito"));
    }

    private void entoncesSeQuitanLasDosPiezasPorSuNombre() {
        entoncesSeAcepta(accion("quitar_pieza", "pieza", "e7"), accion("quitar_pieza", "pieza", "e8"));
    }

    // --- datos ---

    private static LlamadaHerramienta llamada(String nombre, Object... argumentos) {
        return new LlamadaHerramienta(nombre, mapa(argumentos));
    }

    private static AccionTaller accion(String herramienta, Object... argumentos) {
        return new AccionTaller(herramienta, mapa(argumentos));
    }
}
